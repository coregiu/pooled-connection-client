package com.xvzhu.connections.operation;

import com.xvzhu.connections.apis.ConnectionConst;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.ConnectionManagerConfig;
import com.xvzhu.connections.apis.protocol.IConnection;
import com.xvzhu.connections.apis.ConnectionManagerBean;
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
    public Consumer<Map.Entry<ConnectionBean, Map<Thread, ConnectionManagerBean>>> getReleaseConsumer() {
        return entry -> releaseConnection(entry.getKey(), entry.getValue());
    }

    /**
     * Gets release consumer.
     *
     * @return the release biConsumer
     */
    public BiConsumer<ConnectionBean, Map<Thread, ConnectionManagerBean>> getReleaseBiConsumer() {
        return this::releaseConnection;
    }

    /**
     * Release to manager.
     *
     * @param managerBean the manager bean
     */
    public void setConnection2Idle(@NonNull ConnectionManagerBean managerBean) {
        managerBean.setReleaseTime(Calendar.getInstance().getTimeInMillis());
        managerBean.setConnectionBorrowed(false);
    }

    /**
     * Close a connection.
     *
     * @param thread            the thread
     * @param hostConnectionMap the host connection map
     */
    public void shutdownConnection(@NonNull Thread thread,
                                   @NonNull Map<Thread, ConnectionManagerBean> hostConnectionMap) {
        ConnectionManagerBean managerBean = hostConnectionMap.get(thread);
        // double check.
        if (managerBean == null) {
            LOG.info("Then thread {} 's connection has been closed!", thread);
            return;
        }
        IConnection connection = managerBean.getConnectionClient();
        if (null != connection) {
            try {
                connection.disconnect();
            } catch (ConnectionException e) {
                LOG.error("Failed to disconnect", e);
            }
        }
        hostConnectionMap.remove(thread);
    }

    private void releaseConnection(ConnectionBean connectionBean,
                                   Map<Thread, ConnectionManagerBean> hostConnectionMap) {
        if (Thread.currentThread().getName().equals(ConnectionConst.SCHEDULE_THREAD_NAME)) {
            synchronized (LOCK) {
                batchReleaseAction(connectionBean, hostConnectionMap);
            }
        } else {
            if (hostConnectionMap == null || hostConnectionMap.get(Thread.currentThread()) == null) {
                return;
            }
            ConnectionManagerBean managerBean = hostConnectionMap.get(Thread.currentThread());
            synchronized (managerBean.getLock()) {
                singleReleaseAction(hostConnectionMap);
            }
        }
    }

    private void batchReleaseAction(ConnectionBean connectionBean,
                                    Map<Thread, ConnectionManagerBean> hostConnectionMap) {
        LOG.debug("begin to release all the connections of host: {}", connectionBean.getHost());
        long timeNow = Calendar.getInstance().getTimeInMillis();
        // 1. Release timed out and closed connections. Contains reuse and close time out.
        long releaseSize = hostConnectionMap.entrySet().stream()
                .filter(entry -> entry.getValue().isConnectionBorrowed() &&
                        isTimedOut(timeNow, entry.getValue().getBorrowTime(), config.getBorrowTimeoutMS()))
                .peek(entry -> setConnection2Idle(entry.getValue()))
                .count();
        LOG.warn("release size = {}", releaseSize);

        Set<Map.Entry<Thread, ConnectionManagerBean>> closeSet
                = new HashSet<>(hostConnectionMap.size());
        hostConnectionMap.entrySet().stream()
                .filter(entry -> (!entry.getValue().isConnectionBorrowed()) &&
                        isTimedOut(timeNow, entry.getValue().getReleaseTime(), config.getIdleTimeoutMS()))
                .forEach(closeSet::add);
        LOG.warn("shutdown size = {}", closeSet.size());
        closeSet.forEach(entry -> shutdownConnection(entry.getKey(), hostConnectionMap));

        // 2. Release unused connection if connections is exceed the max size.
        int hostConnectionRemainSize = hostConnectionMap.size() - config.getMaxConnectionSize();
        if (hostConnectionRemainSize <= 0) {
            return;
        }
        Set<Map.Entry<Thread, ConnectionManagerBean>> clearSet
                = new HashSet<>(hostConnectionMap.size());
        hostConnectionMap.entrySet().stream()
                .filter(entry -> (!entry.getValue().isConnectionBorrowed()))
                .forEach(clearSet::add);
        LOG.warn("shutdown by connection size check, size = {}", clearSet.size());
        clearSet.forEach(entry -> shutdownConnection(entry.getKey(), hostConnectionMap));
    }

    private boolean isTimedOut(long timeNow, long checkTime, int timeout) {
        return timeNow - checkTime > timeout;
    }

    private void singleReleaseAction(Map<Thread, ConnectionManagerBean> hostConnectionMap) {
        Thread releaseThread = Thread.currentThread();
        ConnectionManagerBean managerBean = hostConnectionMap.get(Thread.currentThread());
        long timeNow = Calendar.getInstance().getTimeInMillis();

        if (managerBean.isConnectionBorrowed() && isTimedOut(timeNow,
                managerBean.getBorrowTime(), config.getBorrowTimeoutMS())) {
            setConnection2Idle(managerBean);
        } else if (!managerBean.isConnectionBorrowed() && isTimedOut(timeNow,
                managerBean.getReleaseTime(), config.getIdleTimeoutMS())){
            shutdownConnection(releaseThread, hostConnectionMap);
        } else {
            LOG.info("Do nothing");
        }
    }
}