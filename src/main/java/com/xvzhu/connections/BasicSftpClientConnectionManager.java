package com.xvzhu.connections;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.ConnectionManagerConfig;
import com.xvzhu.connections.apis.IConnection;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IConnectionMonitor;
import com.xvzhu.connections.apis.IObserver;
import com.xvzhu.connections.apis.ISftpConnection;
import com.xvzhu.connections.monitor.ConnectionMonitor;
import com.xvzhu.connections.operation.OperationFactory;
import com.xvzhu.connections.sftp.SftpConnectionFactory;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * <p>Single connection scene.</p>
 * Each host, thread has a connection.<Br>
 * See the connection {@link ISftpConnection}
 *
 * @param <T> the connection type parameter.
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 14:22
 */
public class BasicSftpClientConnectionManager<T extends IConnection> implements IConnectionManager {
    private static final Logger LOG = LoggerFactory.getLogger(BasicSftpClientConnectionManager.class);
    private static final int DEFAULT_MAX_CONNECTION_SIZE = 8;

    /**
     * monitor container.<br>
     * Static container for monitor all connections for each host, thread.
     */
    private static Map<ConnectionBean, Map<Thread, ManagerBean>> connections
            = new ConcurrentHashMap<>(DEFAULT_MAX_CONNECTION_SIZE);

    private static IConnectionMonitor connectionMonitor = ConnectionMonitor.getInstance();

    private static OperationFactory operationFactory;

    private static ConnectionManagerConfig connectionManagerConfig;

    private BasicSftpClientConnectionManager(ConnectionManagerConfig connectionManagerConfig) {
        BasicSftpClientConnectionManager.connectionManagerConfig = connectionManagerConfig;
        connectionMonitor.setIntervalTimeSecond(connectionManagerConfig.getIntervalTimeMS());
        operationFactory = new OperationFactory(connectionManagerConfig);
    }

    /**
     * Get connections container.
     *
     * @return the connections
     */
    public static Map<ConnectionBean, Map<Thread, ManagerBean>> getConnections() {
        return connections;
    }

    /**
     * Gets operation factory.
     *
     * @return the operation factory
     */
    public static Consumer<Map.Entry<ConnectionBean, Map<Thread, ManagerBean>>> getReleaseConsumer() {
        return operationFactory.getReleaseConsumer();
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
        connectionMonitor.notifyObservers(this, connectionBean);
        Map<Thread, ManagerBean> threadManagerBeanMap = connections.get(connectionBean);
        if (null == threadManagerBeanMap) {
            LOG.info("Then host {}, thread {} 's do not has any connections!",
                    connectionBean.getHost(),
                    Thread.currentThread().getName());
            return getAndRegisterNewConnection(connectionBean);
        }
        ManagerBean managerBean = threadManagerBeanMap.get(Thread.currentThread());
        if (null != managerBean) {
            return reuseConnection(connectionBean, managerBean);
        } else {
            return getAndRegisterNewConnection(connectionBean);
        }
    }

    /**
     * Release connection.
     *
     * @param connectionBean the connection bean
     * @throws ConnectionException the connection exception
     */
    @Override
    public void releaseConnection(ConnectionBean connectionBean) {
        connectionMonitor.notifyObservers(this, connectionBean);
        Map<Thread, ManagerBean> threadManagerBeanMap = connections.get(connectionBean);
        if (null == threadManagerBeanMap) {
            LOG.info("Then host {}, thread {} 's do not has any connections!",
                    connectionBean.getHost(),
                    Thread.currentThread().getName());
            return;
        }

        ManagerBean managerBean = threadManagerBeanMap.get(Thread.currentThread());
        if (null == managerBean) {
            LOG.info("Then host {}, thread {} 's connection has been closed!",
                    connectionBean.getHost(),
                    Thread.currentThread().getName());
            return;
        }
        synchronized (managerBean.getLock()) {
            operationFactory.releaseAConnection(managerBean);
        }
    }

    /**
     * Close connection.
     *
     * @param connectionBean the connection bean
     * @throws ConnectionException the connection exception
     */
    @Override
    public void closeConnection(ConnectionBean connectionBean) {
        connectionMonitor.notifyObservers(this, connectionBean);
        Map<Thread, ManagerBean> threadManagerBeanMap = connections.get(connectionBean);
        if (null == threadManagerBeanMap) {
            LOG.info("Then host {}, thread {} 's do not has any connections!",
                    connectionBean.getHost(),
                    Thread.currentThread().getName());
            return;
        }

        ManagerBean managerBean = threadManagerBeanMap.get(Thread.currentThread());
        if (null == managerBean) {
            LOG.info("Then host {}, thread {} 's connection has been closed!",
                    connectionBean.getHost(),
                    Thread.currentThread().getName());
            return;
        }
        synchronized (managerBean.getLock()) {
            operationFactory.closeAConnection(Thread.currentThread(), threadManagerBeanMap);
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
        observer.visit(this, connectionBean);
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

    private T reuseConnection(ConnectionBean connectionBean, ManagerBean managerBean)
            throws ConnectionException {
        synchronized (managerBean.getLock()) {
            if (managerBean.isConnectionBorrowed) {
                LOG.error("The host : {}, thread :{}'s connection has been borrowed!",
                        connectionBean.getHost(), Thread.currentThread().getName());
                throw new ConnectionException("The host : %s, thread :%s's connection has been borrowed!",
                        connectionBean.getHost(), Thread.currentThread().getName());
            }
            managerBean.setConnectionBorrowed(true);
            managerBean.setBorrowTime(Calendar.getInstance().getTimeInMillis());
            LOG.debug("Reuse the connection for host {}, thread {}",
                    connectionBean.getHost(), Thread.currentThread().getName());
            return (T) managerBean.getSftpConnection();
        }
    }

    private T getAndRegisterNewConnection(ConnectionBean connectionBean) throws ConnectionException {
        ManagerBean managerBean;
        synchronized (this) {
            ISftpConnection connection =
                    SftpConnectionFactory.builder()
                            .connectionBean(connectionBean)
                            .timeoutMilliSecond(connectionManagerConfig.getConnectionTimeoutMs()).build().create();

            managerBean = ManagerBean.builder()
                    .isConnectionBorrowed(true)
                    .sftpConnection(connection)
                    .build();
            Map<Thread, ManagerBean> managerBeanThreadLocal = new ConcurrentHashMap<>(DEFAULT_MAX_CONNECTION_SIZE);
            managerBeanThreadLocal.put(Thread.currentThread(), managerBean);
            connections.put(connectionBean, managerBeanThreadLocal);
            LOG.debug("New a connection for host {}, thread {}",
                    connectionBean.getHost(), Thread.currentThread().getName());
            return (T) connection;
        }
    }

    /**
     * The type Manager bean.
     */
    @Data
    @Builder
    public static class ManagerBean {
        @Builder.Default
        private Object lock = new Object();
        private ISftpConnection sftpConnection;
        @Builder.Default
        private long borrowTime = Calendar.getInstance().getTimeInMillis();
        private long releaseTime;
        @Builder.Default
        private boolean isConnectionBorrowed = false;
    }

    /**
     * The type Basic sftp client connection manager builder.
     */
    public static class BasicSftpClientConnectionManagerBuilder {
        private ConnectionManagerConfig connectionManagerConfig;

        /**
         * Sets connection manager config.
         *
         * @param connectionManagerConfig the connection manager config
         * @return the connection manager config
         */
        public BasicSftpClientConnectionManagerBuilder setConnectionManagerConfig(
                ConnectionManagerConfig connectionManagerConfig) {
            this.connectionManagerConfig = connectionManagerConfig;
            return this;
        }

        /**
         * Build basic sftp client connection manager.
         *
         * @return the basic sftp client connection manager
         */
        public BasicSftpClientConnectionManager build() {
            return new BasicSftpClientConnectionManager(connectionManagerConfig);
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