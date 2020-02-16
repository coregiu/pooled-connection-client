package com.xvzhu.connections.operation;

import com.jcraft.jsch.JSchException;
import com.xvzhu.connections.BasicSftpClientConnectionManager;
import com.xvzhu.connections.apis.ConnectionConst;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionManagerConfig;
import com.xvzhu.connections.apis.ISftpConnection;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>The Operation factory.</p>
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 16:01
 */
public class OperationFactory {
    private static final Logger LOG = LoggerFactory.getLogger(OperationFactory.class);
    private static final int SECOND_CONVERT_UNIT = 1000;

    private ConnectionManagerConfig config;

    /**
     * Instantiates a new Operation factory.
     *
     * @param connectionManagerConfig the connection manager config
     */
    public OperationFactory(@NonNull ConnectionManagerConfig connectionManagerConfig) {
        this.config = connectionManagerConfig;
    }

    /**
     * Gets release consumer.
     *
     * @return the release consumer
     */
    public Consumer<Map.Entry<ConnectionBean, Map<Thread, BasicSftpClientConnectionManager.ManagerBean>>> getReleaseConsumer() {
        return entry -> releaseSftpConnection(entry.getKey(), entry.getValue());
    }

    /**
     * Release a connection.
     *
     * @param managerBean the manager bean
     */
    public void releaseAConnection(@NonNull BasicSftpClientConnectionManager.ManagerBean managerBean) {
        managerBean.setReleaseTime(Calendar.getInstance().getTimeInMillis());
        managerBean.setConnectionBorrowed(false);
    }

    /**
     * Close a connection.
     *
     * @param thread            the thread
     * @param hostConnectionMap the host connection map
     */
    public void closeAConnection(@NonNull Thread thread,
                                 @NonNull Map<Thread, BasicSftpClientConnectionManager.ManagerBean> hostConnectionMap) {
        try {
            BasicSftpClientConnectionManager.ManagerBean managerBean = hostConnectionMap.get(thread);
            // double check.
            if (managerBean == null) {
                LOG.info("Then thread {} 's connection has been closed!", thread);
                return;
            }
            ISftpConnection connection = managerBean.getSftpConnection();
            connection.getChannelSftp().disconnect();
            hostConnectionMap.remove(Thread.currentThread());
            connection.getChannelSftp().getSession().disconnect();
        } catch (JSchException e) {
            LOG.error("Failed to close the connection", e);
        }
    }

    private void releaseSftpConnection(ConnectionBean connectionBean,
                                       Map<Thread, BasicSftpClientConnectionManager.ManagerBean> hostConnectionMap) {
        if (Thread.currentThread().getName().equals(ConnectionConst.SCHEDULE_THREAD_NAME)) {
            synchronized (this) {
                releaseAllConnections(connectionBean, hostConnectionMap);
            }
        } else {
            BasicSftpClientConnectionManager.ManagerBean managerBean = hostConnectionMap.get(Thread.currentThread());
            if (managerBean == null) {
                return;
            }
            synchronized (managerBean.getLock()) {
                releaseCurrentConnection(connectionBean, hostConnectionMap);
            }
        }
    }

    private void releaseAllConnections(ConnectionBean connectionBean,
                                       Map<Thread, BasicSftpClientConnectionManager.ManagerBean> hostConnectionMap) {
        LOG.debug("begin to release all the connections of host: {}", connectionBean.getHost());
        Set<Thread> reuseList = new HashSet<>(hostConnectionMap.size());
        long timeNow = Calendar.getInstance().getTimeInMillis();
        // 1. Release timed out and closed connections. Contains reuse and close time out.
        hostConnectionMap.entrySet().stream()
                .filter(entry -> isTimedOut(timeNow, entry.getValue().isConnectionBorrowed(),
                        entry.getValue().getBorrowTime(), config.getReuseTimeoutSecond()))
                .forEach(entry -> reuseList.add(entry.getKey()));
        reuseList.stream().forEach(thread -> releaseAConnection(hostConnectionMap.get(thread)));

        Set<Thread> closeList = new HashSet<>(hostConnectionMap.size());
        hostConnectionMap.entrySet().stream()
                .filter(entry -> isTimedOut(timeNow, !entry.getValue().isConnectionBorrowed(),
                        entry.getValue().getReleaseTime(), config.getCloseTimeoutSecond()))
                .forEach(entry -> closeList.add(entry.getKey()));
        closeList.stream().forEach(thread -> closeAConnection(thread, hostConnectionMap));

        // 2. Release unused connection if connections is exceed the max size.
        int hostConnectionRemainSize = hostConnectionMap.size() - config.getMaxConnectionSize();
        if (hostConnectionRemainSize <= 0) {
            return;
        }
        Set<Thread> clearList = new HashSet<>(hostConnectionMap.size());
        hostConnectionMap.entrySet().stream()
                .filter(entry -> !entry.getValue().isConnectionBorrowed())
                .forEach(entry -> clearList.add(entry.getKey()));
        closeList.stream().forEach(thread -> closeAConnection(thread, hostConnectionMap));
    }

    private boolean isTimedOut(long timeNow, boolean isBorrowed, long checkTime, int timedout) {
        return isBorrowed && ((timeNow - checkTime) / SECOND_CONVERT_UNIT > timedout);
    }

    private void releaseCurrentConnection(ConnectionBean connectionBean,
                                          Map<Thread, BasicSftpClientConnectionManager.ManagerBean> hostConnectionMap) {
        Thread releaseThread = Thread.currentThread();
        BasicSftpClientConnectionManager.ManagerBean managerBean = hostConnectionMap.get(Thread.currentThread());
        long timeNow = Calendar.getInstance().getTimeInMillis();

        if (isTimedOut(timeNow, managerBean.isConnectionBorrowed(),
                managerBean.getBorrowTime(), config.getReuseTimeoutSecond())) {
            releaseAConnection(managerBean);
        } else if (isTimedOut(timeNow, !managerBean.isConnectionBorrowed(),
                managerBean.getReleaseTime(), config.getCloseTimeoutSecond())){
            closeAConnection(releaseThread, hostConnectionMap);
        } else {
            LOG.info("Do nothing");
        }
    }
}
