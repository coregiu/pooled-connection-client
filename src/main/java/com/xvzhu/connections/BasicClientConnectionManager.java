/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections;

import com.xvzhu.connections.apis.BorrowStatus;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.ConnectionManagerConfig;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IConnectionMonitor;
import com.xvzhu.connections.apis.IObserver;
import com.xvzhu.connections.apis.IOperation;
import com.xvzhu.connections.apis.protocol.IConnection;
import com.xvzhu.connections.apis.ConnectionManagerBean;
import com.xvzhu.connections.monitor.ConnectionMonitor;
import com.xvzhu.connections.operation.OperationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * <p>Single connection scene.</p>
 * Each host, thread has a connection.<Br>
 * See the connection {@link IConnectionMonitor}
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 14:22
 */
public class BasicClientConnectionManager implements IConnectionManager {
    private static final Logger LOG = LoggerFactory.getLogger(BasicClientConnectionManager.class);
    private static final int DEFAULT_MAX_CONNECTION_SIZE = 8;

    /**
     * monitor container.<br>
     * Static container for monitor all connections for each host, thread.
     */
    private static Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections
            = new ConcurrentHashMap<>(DEFAULT_MAX_CONNECTION_SIZE);

    private static ConnectionManagerConfig connectionManagerConfig = ConnectionManagerConfig.builder().build();

    private static IOperation operationFactory = new OperationFactory(connectionManagerConfig);

    private static IConnectionMonitor connectionMonitor = ConnectionMonitor.getInstance();

    /**
     * Gets release consumer.
     *
     * @return the release consumer
     */
    public static Consumer<Map.Entry<ConnectionBean, Map<Thread, ConnectionManagerBean>>> getReleaseConsumer() {
        return operationFactory.getReleaseConsumer();
    }

    /**
     * Gets release bi consumer.
     *
     * @return the release bi consumer
     */
    public static BiConsumer<ConnectionBean, Map<Thread, ConnectionManagerBean>> getReleaseBiConsumer() {
        return operationFactory.getReleaseBiConsumer();
    }

    /**
     * Borrow connection t.
     *
     * @param connectionBean the connection bean
     * @return the t
     * @throws ConnectionException the connection exception
     */
    @Override
    public <T extends IConnection> T borrowConnection(ConnectionBean connectionBean, Class<T> clazz)
            throws ConnectionException {
        connectionMonitor.notifyObservers(this, connectionBean, connections);

        Map<Thread, ConnectionManagerBean> threadManagerBeanMap = connections.get(connectionBean);
        BorrowStatus borrowStatus = BorrowStatus.INIT;
        while (borrowStatus != BorrowStatus.FINAL) {
            switch (borrowStatus) {
                case INIT:
                    borrowStatus = getBorrowStatus(connectionBean, threadManagerBeanMap);
                    break;
                case NO_CONNECTION:
                    borrowStatus = BorrowStatus.NEED_NEW_CONNECTION;
                    break;
                case THREAD_HAS_NO_CONNECTION:
                    borrowStatus = BorrowStatus.OTHER_THREAD_HAS_CONNECTION;
                    break;
                case THREAD_HAS_CONNECTION:
                    ConnectionManagerBean managerBean = threadManagerBeanMap.get(Thread.currentThread());
                    Optional<T> connection = reuseConnection(connectionBean, managerBean);
                    if (connection.isPresent()) {
                        return connection.get();
                    } else {
                        borrowStatus = BorrowStatus.OTHER_THREAD_HAS_CONNECTION;
                        break;
                    }
                case OTHER_THREAD_HAS_CONNECTION:
                    Optional<T> connectionOption = borrowFromOtherThread(connectionBean, clazz);
                    if (connectionOption.isPresent()) {
                        return connectionOption.get();
                    } else {
                        borrowStatus = BorrowStatus.NEED_NEW_CONNECTION;
                        break;
                    }
                case NEED_NEW_CONNECTION:
                    if (threadManagerBeanMap != null &&
                            threadManagerBeanMap.size() >= connectionManagerConfig.getMaxConnectionSize()) {
                        borrowStatus = BorrowStatus.OVER_LIMIT;
                        break;
                    }

                    return getAndRegisterNewConnection(connectionBean, clazz);
                case OVER_LIMIT:
                    LOG.error("The host:{}, thread:{}' connection is {}, more than the limit:{}.",
                            connectionBean.getHost(), Thread.currentThread(),
                            threadManagerBeanMap.size(), connectionManagerConfig.getMaxConnectionSize());
                    throw new ConnectionException("Failed to borrow connection because of to much connections.");
                default:
                    throw new ConnectionException("Failed to borrow connection for FINAL status.");
            }
        }
        throw new ConnectionException("Failed to borrow connection for FINAL status.");
    }

    private BorrowStatus getBorrowStatus(ConnectionBean connectionBean, Map<Thread, ConnectionManagerBean> threadManagerBeanMap) {
        BorrowStatus borrowStatus;
        if (null == threadManagerBeanMap) {
            LOG.info("Then host {} 's do not has any connections!",
                    connectionBean.getHost());
            borrowStatus = BorrowStatus.NO_CONNECTION;
        } else if (null == threadManagerBeanMap.get(Thread.currentThread())) {
            LOG.info("Then host {}, thread {} 's do not has any connections!",
                    connectionBean.getHost(),
                    Thread.currentThread().getName());
            borrowStatus = BorrowStatus.THREAD_HAS_NO_CONNECTION;
        } else {
            borrowStatus = BorrowStatus.THREAD_HAS_CONNECTION;
        }
        return borrowStatus;
    }

    /**
     * Release connection.
     *
     * @param connectionBean the connection bean
     */
    @Override
    public void releaseConnection(ConnectionBean connectionBean) {
        connectionMonitor.notifyObservers(this, connectionBean, connections);
        releaseOrShutdownConnection(connectionBean, false);
    }

    /**
     * Close connection.
     *
     * @param connectionBean the connection bean
     */
    @Override
    public void closeConnection(ConnectionBean connectionBean) {
        connectionMonitor.notifyObservers(this, connectionBean, connections);
        releaseOrShutdownConnection(connectionBean, true);
    }

    private void releaseOrShutdownConnection(ConnectionBean connectionBean, boolean isShutdown) {
        Map<Thread, ConnectionManagerBean> threadManagerBeanMap = connections.get(connectionBean);
        if (null == threadManagerBeanMap) {
            LOG.info("Then host {}, thread {} 's do not has any connections!",
                    connectionBean.getHost(),
                    Thread.currentThread().getName());
            return;
        }

        ConnectionManagerBean managerBean = threadManagerBeanMap.get(Thread.currentThread());
        if (null == managerBean) {
            LOG.info("Then host {}, thread {} 's connection has been closed!",
                    connectionBean.getHost(),
                    Thread.currentThread().getName());
            return;
        }
        if (isShutdown) {
            operationFactory.shutdownConnection(Thread.currentThread(), threadManagerBeanMap);
        } else {
            operationFactory.setConnection2Idle(managerBean);
        }
    }

    /**
     * Accept.
     *
     * @param observer       the observer
     * @param connectionBean the connection bean
     */
    @Override
    public void accept(IObserver observer, ConnectionBean connectionBean) {
        observer.visit(this, connectionBean, connections);
    }

    /**
     * Attach.
     *
     * @param observer the observer
     */
    @Override
    public void attach(IObserver observer) {
        connectionMonitor.attach(observer);
    }

    @SuppressWarnings("unchecked")
    private <T extends IConnection> Optional<T> reuseConnection(ConnectionBean connectionBean,
                                                                ConnectionManagerBean managerBean) {
        synchronized (managerBean.getLock()) {
            IConnection connection = null;
            if (managerBean.getConnectionClient() != null && managerBean.getConnectionClient().isValid()) {
                managerBean.setConnectionBorrowed(true);
                managerBean.setBorrowTime(Calendar.getInstance().getTimeInMillis());
                LOG.debug("Reuse the connection for host {}, thread {}",
                        connectionBean.getHost(), Thread.currentThread().getName());
                connection = managerBean.getConnectionClient();
            }

            return (Optional<T>) Optional.ofNullable(connection);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends IConnection> Optional<T> borrowFromOtherThread(ConnectionBean connectionBean, Class<T> clazz) {
        Map<Thread, ConnectionManagerBean> connectionMap = connections.get(connectionBean);
        T connectionClient = null;
        for (Map.Entry<Thread, ConnectionManagerBean> entry : connectionMap.entrySet()) {
            ConnectionManagerBean connectionManagerBean = entry.getValue();
            if (isConnectionValid(connectionManagerBean, clazz)) {
                synchronized (connectionManagerBean.getLock()) {
                    if (isConnectionValid(connectionManagerBean, clazz)) {
                        connectionClient = (T)connectionManagerBean.getConnectionClient();
                        connectionMap.remove(entry.getKey());
                        connectionManagerBean.setConnectionBorrowed(true);
                        connectionManagerBean.setBorrowTime(Calendar.getInstance().getTimeInMillis());
                        connectionMap.put(Thread.currentThread(), connectionManagerBean);
                    }
                }
            }
        }
        return Optional.ofNullable(connectionClient);
    }

    private <T extends IConnection> boolean isConnectionValid(ConnectionManagerBean connectionManagerBean, Class<T> clazz) {
        return !connectionManagerBean.isConnectionBorrowed()
                && connectionManagerBean.getConnectionClient() != null
                && connectionManagerBean.getConnectionClient().isValid()
                && clazz.isAssignableFrom(connectionManagerBean.getConnectionClient().getClass());
    }

    private <T extends IConnection> T getAndRegisterNewConnection(ConnectionBean connectionBean, Class<T> clazz)
            throws ConnectionException {
        // Each thread has a connection, no need to synchronize.
        ConnectionManagerBean managerBean;
        T connection = operationFactory.createConnection(connectionBean, connectionManagerConfig, clazz);

        managerBean = ConnectionManagerBean.builder()
                .isConnectionBorrowed(true)
                .connectionClient(connection)
                .build();
        Map<Thread, ConnectionManagerBean> managerBeanThreadLocal = new ConcurrentHashMap<>(DEFAULT_MAX_CONNECTION_SIZE);
        managerBeanThreadLocal.put(Thread.currentThread(), managerBean);
        connections.put(connectionBean, managerBeanThreadLocal);
        LOG.debug("New a connection for host {}, thread {}",
                connectionBean.getHost(), Thread.currentThread().getName());
        return connection;
    }

    /**
     * The type Basic sftp client connection manager builder.
     */
    public static class BasicSftpClientConnectionManagerBuilder {
        /**
         * Sets max connection size.
         *
         * @param maxConnectionSize the max connection size
         * @return the max connection size
         */
        public BasicSftpClientConnectionManagerBuilder setMaxConnectionSize(int maxConnectionSize) {
            connectionManagerConfig.setMaxConnectionSize(maxConnectionSize);
            return this;
        }

        /**
         * Sets borrow timeout ms.
         *
         * @param borrowTimeoutMS the borrow timeout ms
         * @return the borrow timeout ms
         */
        public BasicSftpClientConnectionManagerBuilder setBorrowTimeoutMS(int borrowTimeoutMS) {
            connectionManagerConfig.setBorrowTimeoutMS(borrowTimeoutMS);
            return this;
        }

        /**
         * Sets idle timeout second.
         *
         * @param idleTimeoutSecond the idle timeout second
         * @return the idle timeout second
         */
        public BasicSftpClientConnectionManagerBuilder setIdleTimeoutSecond(int idleTimeoutSecond) {
            connectionManagerConfig.setIdleTimeoutMS(idleTimeoutSecond);
            return this;
        }

        /**
         * Sets connection timeout ms.
         *
         * @param connectionTimeoutMs the connection timeout ms
         * @return the connection timeout ms
         */
        public BasicSftpClientConnectionManagerBuilder setConnectionTimeoutMs(int connectionTimeoutMs) {
            connectionManagerConfig.setConnectionTimeoutMs(connectionTimeoutMs);
            return this;
        }

        /**
         * Sets schedule period time ms.
         *
         * @param schedulePeriodTimeMS the schedule period time ms
         * @return the schedule period time ms
         */
        public BasicSftpClientConnectionManagerBuilder setSchedulePeriodTimeMS(long schedulePeriodTimeMS) {
            connectionManagerConfig.setSchedulePeriodTimeMS(schedulePeriodTimeMS);
            connectionMonitor.setIntervalTimeSecond(schedulePeriodTimeMS);
            return this;
        }

        /**
         * Sets auto inspect.
         *
         * @param autoInspect the auto inspect
         * @return the auto inspect
         */
        public BasicSftpClientConnectionManagerBuilder setAutoInspect(boolean autoInspect) {
            connectionManagerConfig.setAutoInspect(autoInspect);
            connectionMonitor.setAutoInspect(autoInspect);
            return this;
        }

        /**
         * Build basic sftp client connection manager.
         *
         * @return the basic sftp client connection manager
         */
        public BasicClientConnectionManager build() {
            return new BasicClientConnectionManager();
        }
    }

    /**
     * Builder basic sftp client connection manager builder.
     *
     * @return the basic sftp client connection manager builder
     */
    public static BasicSftpClientConnectionManagerBuilder builder() {
        return new BasicSftpClientConnectionManagerBuilder();
    }
}