package com.xvzhu.connections.operation;

import com.jcraft.jsch.JSchException;
import com.xvzhu.connections.apis.ConnectionConst;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionManagerConfig;
import com.xvzhu.connections.apis.ISftpConnection;
import com.xvzhu.connections.apis.ManagerBean;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
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

    private static final Object LOCK = new Object();

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
    public Consumer<Map.Entry<ConnectionBean, Map<Thread, ManagerBean>>> getReleaseConsumer() {
        return entry -> releaseSftpConnection(entry.getKey(), entry.getValue());
    }

    /**
     * Gets release consumer.
     *
     * @return the release biConsumer
     */
    public BiConsumer<ConnectionBean, Map<Thread, ManagerBean>> getReleaseBiConsumer() {
        return (key, value) -> releaseSftpConnection(key, value);
    }

    /**
     * Release a connection.
     *
     * @param managerBean the manager bean
     */
    public void releaseAConnection(@NonNull ManagerBean managerBean) {
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
                                 @NonNull Map<Thread, ManagerBean> hostConnectionMap) {
        try {
            ManagerBean managerBean = hostConnectionMap.get(thread);
            // double check.
            if (managerBean == null) {
                LOG.info("Then thread {} 's connection has been closed!", thread);
                return;
            }
            ISftpConnection connection = managerBean.getSftpConnection();
            if (null != connection.getChannelSftp()) {
                connection.getChannelSftp().disconnect();
                connection.getChannelSftp().getSession().disconnect();
            }
            hostConnectionMap.remove(thread);
        } catch (JSchException e) {
            LOG.error("Failed to close the connection", e);
        }
    }

    private void releaseSftpConnection(ConnectionBean connectionBean,
                                       Map<Thread, ManagerBean> hostConnectionMap) {
        if (Thread.currentThread().getName().equals(ConnectionConst.SCHEDULE_THREAD_NAME)) {
            synchronized (LOCK) {
                releaseAllConnections(connectionBean, hostConnectionMap);
            }
        } else {
            ManagerBean managerBean = hostConnectionMap.get(Thread.currentThread());
            if (managerBean == null) {
                return;
            }
            synchronized (managerBean.getLock()) {
                releaseCurrentConnection(hostConnectionMap);
            }
        }
    }

    private void releaseAllConnections(ConnectionBean connectionBean,
                                       Map<Thread, ManagerBean> hostConnectionMap) {
        LOG.debug("begin to release all the connections of host: {}", connectionBean.getHost());
        Set<Map.Entry<Thread, ManagerBean>> reuseSet
                = new HashSet<>(hostConnectionMap.size());
        long timeNow = Calendar.getInstance().getTimeInMillis();
        // 1. Release timed out and closed connections. Contains reuse and close time out.
        hostConnectionMap.entrySet().stream()
                .filter(entry -> entry.getValue().isConnectionBorrowed() &&
                        isTimedOut(timeNow, entry.getValue().getBorrowTime(), config.getBorrowTimeoutMS()))
                .forEach(entry -> reuseSet.add(entry));
        reuseSet.stream().forEach(entry -> releaseAConnection(entry.getValue()));

        Set<Map.Entry<Thread, ManagerBean>> closeSet
                = new HashSet<>(hostConnectionMap.size());
        hostConnectionMap.entrySet().stream()
                .filter(entry -> (!entry.getValue().isConnectionBorrowed()) &&
                        isTimedOut(timeNow, entry.getValue().getReleaseTime(), config.getIdleTimeoutSecond()))
                .forEach(entry -> closeSet.add(entry));
        closeSet.stream().forEach(entry -> closeAConnection(entry.getKey(), hostConnectionMap));

        // 2. Release unused connection if connections is exceed the max size.
        int hostConnectionRemainSize = hostConnectionMap.size() - config.getMaxConnectionSize();
        if (hostConnectionRemainSize <= 0) {
            return;
        }
        Set<Map.Entry<Thread, ManagerBean>> clearSet
                = new HashSet<>(hostConnectionMap.size());
        hostConnectionMap.entrySet().stream()
                .filter(entry -> (!entry.getValue().isConnectionBorrowed()))
                .forEach(entry -> clearSet.add(entry));
        clearSet.stream().forEach(entry -> closeAConnection(entry.getKey(), hostConnectionMap));
    }

    private boolean isTimedOut(long timeNow, long checkTime, int timedout) {
        return timeNow - checkTime > timedout;
    }

    private void releaseCurrentConnection(Map<Thread, ManagerBean> hostConnectionMap) {
        Thread releaseThread = Thread.currentThread();
        ManagerBean managerBean = hostConnectionMap.get(Thread.currentThread());
        long timeNow = Calendar.getInstance().getTimeInMillis();

        if (managerBean.isConnectionBorrowed() && isTimedOut(timeNow,
                managerBean.getBorrowTime(), config.getBorrowTimeoutMS())) {
            releaseAConnection(managerBean);
        } else if (!managerBean.isConnectionBorrowed() && isTimedOut(timeNow,
                managerBean.getReleaseTime(), config.getIdleTimeoutSecond())){
            closeAConnection(releaseThread, hostConnectionMap);
        } else {
            LOG.info("Do nothing");
        }
    }
}
