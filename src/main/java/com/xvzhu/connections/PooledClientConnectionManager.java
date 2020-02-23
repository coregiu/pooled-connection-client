/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.ConnectionManagerBean;
import com.xvzhu.connections.apis.ConnectionManagerConfig;
import com.xvzhu.connections.apis.protocol.IConnection;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IConnectionMonitor;
import com.xvzhu.connections.apis.IObserver;
import com.xvzhu.connections.monitor.ConnectionMonitor;
import com.xvzhu.connections.operation.OperationFactory;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The type Pooled sftp client connection manager.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 14:00
 */
public class PooledClientConnectionManager implements IConnectionManager {
    private static final Logger LOG = LoggerFactory.getLogger(PooledClientConnectionManager.class);
    private static final int DEFAULT_MAX_CONNECTION_SIZE = 8;

    private static ConnectionManagerConfig connectionManagerConfig = ConnectionManagerConfig.builder().build();

    private static OperationFactory operationFactory = new OperationFactory(connectionManagerConfig);

    private static IConnectionMonitor connectionMonitor = ConnectionMonitor.getInstance();

    private GenericObjectPool<IConnection> connectionPool;

    /**
     * monitor container.<br>
     * Static container for monitor all connections for each host, thread.
     */
    private static Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections
            = new ConcurrentHashMap<>(DEFAULT_MAX_CONNECTION_SIZE);

    /**
     * Instantiates a new Pooled sftp client connection manager.
     *
     * @param connectionPool the connection pool
     */
    public PooledClientConnectionManager(GenericObjectPool<IConnection> connectionPool) {
        this.connectionPool = connectionPool;
    }

    /**
     * Borrow connection connection.
     *
     * @param <T>            the type parameter
     * @param connectionBean the connection bean
     * @param clazz          the clazz
     * @return the connection
     * @throws ConnectionException the connection exception
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends IConnection> T borrowConnection(ConnectionBean connectionBean, Class<T> clazz)
            throws ConnectionException {
        connectionMonitor.notifyObservers(this, connectionBean, connections);
        try {
            return (T) connectionPool.borrowObject(connectionManagerConfig.getBorrowMaxWaitTimeMS());
        } catch (Exception e) {
            LOG.error("Failed to borrow connection", e);
            throw new ConnectionException("Failed to borrow connection.");
        }
    }

    /**
     * Release connection.
     *
     * @param connectionBean the connection bean
     */
    @Override
    public void releaseConnection(ConnectionBean connectionBean) {
        LOG.error("Not need to release.");
    }

    /**
     * Close connection.
     *
     * @param connectionBean the connection bean
     */
    @Override
    public void closeConnection(ConnectionBean connectionBean) {
        LOG.warn("Close the connection pool{}", connectionBean.getHost());
        connectionPool.close();
        connections.remove(connectionBean);
        connectionMonitor.notifyObservers(this, connectionBean, connections);
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

    /**
     * Builder pooled sftp client connection manager builder.
     *
     * @return the pooled sftp client connection manager builder
     */
    public static PooledSftpClientConnectionManagerBuilder builder() {
        return new PooledSftpClientConnectionManagerBuilder();
    }

    /**
     * The type Pooled sftp client connection manager builder.
     */
    public static class PooledSftpClientConnectionManagerBuilder {
        private GenericObjectPoolConfig<IConnection> connectionConfig = new GenericObjectPoolConfig<>();
        private AbandonedConfig abandonedConfig = new AbandonedConfig();

        /**
         * Sets connection config.
         *
         * @param connectionConfig the connection config
         * @return the connection config
         */
        public PooledSftpClientConnectionManagerBuilder setConnectionConfig(GenericObjectPoolConfig<IConnection> connectionConfig) {
            this.connectionConfig = connectionConfig;
            return this;
        }

        /**
         * Sets abandoned config.
         *
         * @param abandonedConfig the abandoned config
         * @return the abandoned config
         */
        public PooledSftpClientConnectionManagerBuilder setAbandonedConfig(AbandonedConfig abandonedConfig) {
            this.abandonedConfig = abandonedConfig;
            return this;
        }

        /**
         * Sets borrow max wait time ms.
         *
         * @param borrowMaxWaitTimeMS the borrow max wait time ms
         * @return the borrow max wait time ms
         */
        public PooledSftpClientConnectionManagerBuilder setBorrowMaxWaitTimeMS(long borrowMaxWaitTimeMS) {
            connectionManagerConfig.setBorrowMaxWaitTimeMS(borrowMaxWaitTimeMS);
            return this;
        }

        /**
         * Sets schedule period time ms.
         *
         * @param schedulePeriodTimeMS the schedule period time ms
         * @return the schedule period time ms
         */
        public PooledSftpClientConnectionManagerBuilder setSchedulePeriodTimeMS(long schedulePeriodTimeMS) {
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
        public PooledSftpClientConnectionManagerBuilder setAutoInspect(boolean autoInspect) {
            connectionManagerConfig.setAutoInspect(autoInspect);
            connectionMonitor.setAutoInspect(autoInspect);
            return this;
        }

        /**
         * Build pooled sftp client connection manager.
         *
         * @param connectionBean the connection bean
         * @param type           the type
         * @return the pooled sftp client connection manager
         * @throws ConnectionException the connection exception
         */
        public PooledClientConnectionManager build(ConnectionBean connectionBean, Class type) throws ConnectionException{
            GenericObjectPool<IConnection> connectionPool;
            if (connections.get(connectionBean) != null) {
                Map<Thread, ConnectionManagerBean> managerBeanMap = connections.get(connectionBean);
                if (managerBeanMap.get(Thread.currentThread()) != null) {
                    connectionPool = connections.get(connectionBean).get(Thread.currentThread()).getConnectionPool();
                } else {
                    connectionPool = generatePool(connectionBean, type, managerBeanMap);
                }
            } else {
                Map<Thread, ConnectionManagerBean> managerBeanMap = new ConcurrentHashMap<>();
                connectionPool = generatePool(connectionBean, type, managerBeanMap);
            }

            return new PooledClientConnectionManager(connectionPool);
        }

        @SuppressWarnings("unchecked")
        private GenericObjectPool<IConnection> generatePool(ConnectionBean connectionBean,
                                                            Class type,
                                                            Map<Thread, ConnectionManagerBean> managerBeanMap)
                throws ConnectionException {
            GenericObjectPool<IConnection> connectionPool;
            BasePooledObjectFactory connectionFactory
                    = operationFactory.createConnectionFactory(connectionBean, connectionManagerConfig, type);
            connectionPool = new GenericObjectPool(connectionFactory, connectionConfig, abandonedConfig);
            ConnectionManagerBean managerBean = ConnectionManagerBean.builder().connectionPool(connectionPool).build();
            managerBeanMap.put(Thread.currentThread(), managerBean);
            return connectionPool;
        }
    }
}
