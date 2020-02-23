package com.xvzhu.connections;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.ConnectionManagerConfig;
import com.xvzhu.connections.apis.protocol.IConnection;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IConnectionMonitor;
import com.xvzhu.connections.apis.IObserver;
import com.xvzhu.connections.apis.protocol.ISftpConnection;
import com.xvzhu.connections.apis.ConnectionManagerBean;
import com.xvzhu.connections.monitor.ConnectionMonitor;
import com.xvzhu.connections.operation.OperationFactory;
import com.xvzhu.connections.sftp.SftpConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * <p>Single connection scene.</p>
 * Each host, thread has a connection.<Br>
 * See the connection {@link IConnectionMonitor}
 *
 * @param <T> the connection type parameter.
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 14:22
 */
public class BasicClientConnectionManager<T extends IConnection> implements IConnectionManager {
    private static final Logger LOG = LoggerFactory.getLogger(BasicClientConnectionManager.class);
    private static final Object LOCK = new Object();
    private static final int DEFAULT_MAX_CONNECTION_SIZE = 8;

    /**
     * monitor container.<br>
     * Static container for monitor all connections for each host, thread.
     */
    private static Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections
            = new ConcurrentHashMap<>(DEFAULT_MAX_CONNECTION_SIZE);

    private static ConnectionManagerConfig connectionManagerConfig = ConnectionManagerConfig.builder().build();

    private static OperationFactory operationFactory = new OperationFactory(connectionManagerConfig);

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
    public T borrowConnection(ConnectionBean connectionBean) throws ConnectionException {
        connectionMonitor.notifyObservers(this, connectionBean, connections);
        Map<Thread, ConnectionManagerBean> threadManagerBeanMap = connections.get(connectionBean);
        if (null == threadManagerBeanMap) {
            LOG.info("Then host {}, thread {} 's do not has any connections!",
                    connectionBean.getHost(),
                    Thread.currentThread().getName());
            return getAndRegisterNewConnection(connectionBean);
        }
        ConnectionManagerBean managerBean = threadManagerBeanMap.get(Thread.currentThread());
        if (null != managerBean) {
            return reuseConnection(connectionBean, managerBean);
        } else {
            if (threadManagerBeanMap.size() >= connectionManagerConfig.getMaxConnectionSize()) {
                LOG.error("The host:{}, thread:{}' connection is {}, more than the limit:{}.",
                        connectionBean.getHost(), Thread.currentThread(),
                        threadManagerBeanMap.size(), connectionManagerConfig.getMaxConnectionSize());
                throw new ConnectionException("Failed to borrow connection because of to much connections.");
            } else {
                return getAndRegisterNewConnection(connectionBean);
            }
        }
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
        synchronized (managerBean.getLock()) {
            if (isShutdown) {
                operationFactory.closeAConnection(Thread.currentThread(), threadManagerBeanMap);
            } else {
                operationFactory.releaseAConnection(managerBean);
            }
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

    private T reuseConnection(ConnectionBean connectionBean, ConnectionManagerBean managerBean)
            throws ConnectionException {
        synchronized (managerBean.getLock()) {
            if (managerBean.isConnectionBorrowed()) {
                LOG.error("The host : {}, thread :{}'s connection has been borrowed!",
                        connectionBean.getHost(), Thread.currentThread().getName());
                throw new ConnectionException("The host : %s, thread :%s's connection has been borrowed!",
                        connectionBean.getHost(), Thread.currentThread().getName());
            }
            managerBean.setConnectionBorrowed(true);
            managerBean.setBorrowTime(Calendar.getInstance().getTimeInMillis());
            LOG.debug("Reuse the connection for host {}, thread {}",
                    connectionBean.getHost(), Thread.currentThread().getName());
            return (T) managerBean.getConnectionClient();
        }
    }

    private T getAndRegisterNewConnection(ConnectionBean connectionBean) throws ConnectionException {
        ConnectionManagerBean managerBean;
        synchronized (LOCK) {
            ISftpConnection connection =
                    SftpConnectionFactory.builder()
                            .connectionBean(connectionBean)
                            .timeoutMilliSecond(connectionManagerConfig.getConnectionTimeoutMs()).build().create();

            managerBean = ConnectionManagerBean.builder()
                    .isConnectionBorrowed(true)
                    .connectionClient(connection)
                    .build();
            Map<Thread, ConnectionManagerBean> managerBeanThreadLocal = new ConcurrentHashMap<>(DEFAULT_MAX_CONNECTION_SIZE);
            managerBeanThreadLocal.put(Thread.currentThread(), managerBean);
            connections.put(connectionBean, managerBeanThreadLocal);
            LOG.debug("New a connection for host {}, thread {}",
                    connectionBean.getHost(), Thread.currentThread().getName());
            return (T) connection;
        }
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