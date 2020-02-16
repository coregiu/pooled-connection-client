package com.xvzhu.connections;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.IConnection;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IConnectionMonitor;
import com.xvzhu.connections.apis.IObserver;
import com.xvzhu.connections.apis.ISftpConnection;
import com.xvzhu.connections.monitor.ConnectionMonitor;
import com.xvzhu.connections.sftp.SftpConnectionFactory;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final int DEFAULT_REUSE_TIME_OUT_MILLI = 15000;
    private static final int DEFAULT_CLOSE_TIME_OUT_MILLI = 60000;
    private static final long DEFAULT_SCHEDULE_INTERVAL_TIME_SECOND = 60L;
    private static final int MAX_TIME_OUT_MILLI = 3600000;

    /**
     * monitor container.<br>
     * Static container for monitor all connections for each host, thread.
     */
    private static Map<ConnectionBean, ThreadLocal<ManagerBean>> connections
            = new ConcurrentHashMap<>(DEFAULT_MAX_CONNECTION_SIZE);

    private static IConnectionMonitor connectionMonitor = ConnectionMonitor.getInstance();

    /**
     * The max size of connections all of current process(ClassLoader).
     */
    private static int maxConnectionSize = DEFAULT_MAX_CONNECTION_SIZE;
    /**
     * Connection close timeout configuration.
     * Default is 60 seconds.
     * If time out, close the connection.
     */
    private int timeoutMilliSecond = DEFAULT_CLOSE_TIME_OUT_MILLI;
    /**
     * Connection reuse timeout configuration.
     * Default is 15 seconds.
     * If time out, release the connection to reuse.
     */
    private int reuseTimeoutMilliSecond = DEFAULT_REUSE_TIME_OUT_MILLI;
    /**
     * The interval of schedule(Second).
     */
    private long intervalTimeSecond = DEFAULT_SCHEDULE_INTERVAL_TIME_SECOND;

    private BasicSftpClientConnectionManager(int maxConnectionSize,
                                            int timeoutMilliSecond,
                                            int reuseTimeoutMilliSecond,
                                            long intervalTimeSecond) {
        this.timeoutMilliSecond = timeoutMilliSecond;
        this.reuseTimeoutMilliSecond = reuseTimeoutMilliSecond;
        this.intervalTimeSecond = intervalTimeSecond;
        BasicSftpClientConnectionManager.maxConnectionSize = maxConnectionSize;
        connectionMonitor.setIntervalTimeSecond(intervalTimeSecond);
    }

    /**
     * Get connections container.
     *
     * @return the connections
     */
    public static Map<ConnectionBean, ThreadLocal<ManagerBean>> getConnections() {
        return connections;
    }

    /**
     * Gets max size of all connections limit.
     *
     * @return the max connection size
     */
    public static int getMaxConnectionSize() {
        return maxConnectionSize;
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
        ThreadLocal<ManagerBean> threadLocal = connections.get(connectionBean);
        if (null == threadLocal) {
            LOG.info("Then host {}, thread {} 's do not has any connections!",
                    connectionBean.getHost(),
                    Thread.currentThread().getName());
            return getAndRegisterNewConnection(connectionBean);
        }
        ManagerBean managerBean = threadLocal.get();
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
    public void releaseConnection(ConnectionBean connectionBean) throws ConnectionException {
        connectionMonitor.notifyObservers(this, connectionBean);
        ThreadLocal<ManagerBean> threadLocal = connections.get(connectionBean);
        if (null == threadLocal) {
            LOG.info("Then host {}, thread {} 's do not has any connections!",
                    connectionBean.getHost(),
                    Thread.currentThread().getName());
            return;
        }

        ManagerBean managerBean = threadLocal.get();
        if (null == managerBean) {
            LOG.info("Then host {}, thread {} 's connection has been closed!",
                    connectionBean.getHost(),
                    Thread.currentThread().getName());
            return;
        }
        synchronized (managerBean.getLock()) {
            managerBean.setConnectionBorrowed(false);
            managerBean.setReleaseTime(Calendar.getInstance().getTimeInMillis());
        }
    }

    /**
     * Close connection.
     *
     * @param connectionBean the connection bean
     * @throws ConnectionException the connection exception
     */
    @Override
    public void closeConnection(ConnectionBean connectionBean) throws ConnectionException {
        connectionMonitor.notifyObservers(this, connectionBean);
        ThreadLocal<ManagerBean> threadLocal = connections.get(connectionBean);
        if (null == threadLocal) {
            LOG.info("Then host {}, thread {} 's do not has any connections!",
                    connectionBean.getHost(),
                    Thread.currentThread().getName());
            return;
        }

        ManagerBean managerBean = threadLocal.get();
        if (null == managerBean) {
            LOG.info("Then host {}, thread {} 's connection has been closed!",
                    connectionBean.getHost(),
                    Thread.currentThread().getName());
            return;
        }
        synchronized (managerBean.getLock()) {
            // double check.
            if (connections.get(connectionBean).get() == null) {
                LOG.info("Then host {}, thread {} 's connection has been closed!",
                        connectionBean.getHost(),
                        Thread.currentThread().getName());
                return;
            }
            ISftpConnection connection = managerBean.getSftpConnection();
            connection.getChannelSftp().disconnect();
            connections.get(connectionBean).remove();
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
                            .timeoutMilliSecond(timeoutMilliSecond).build().create();
            timeoutMilliSecond =
                    timeoutMilliSecond > 0 && timeoutMilliSecond < MAX_TIME_OUT_MILLI ?
                            DEFAULT_CLOSE_TIME_OUT_MILLI : timeoutMilliSecond;
            reuseTimeoutMilliSecond =
                    reuseTimeoutMilliSecond > 0 && reuseTimeoutMilliSecond < MAX_TIME_OUT_MILLI ?
                            DEFAULT_REUSE_TIME_OUT_MILLI : reuseTimeoutMilliSecond;
            managerBean = ManagerBean.builder()
                    .isConnectionBorrowed(true)
                    .sftpConnection(connection)
                    .timeOutMilli(timeoutMilliSecond)
                    .reuseTimeOutMilli(reuseTimeoutMilliSecond).build();
            ThreadLocal<ManagerBean> managerBeanThreadLocal = new ThreadLocal<>();
            managerBeanThreadLocal.set(managerBean);
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
        private long timeOutMilli = DEFAULT_CLOSE_TIME_OUT_MILLI;
        @Builder.Default
        private long reuseTimeOutMilli = DEFAULT_REUSE_TIME_OUT_MILLI;
        @Builder.Default
        private long borrowTime = Calendar.getInstance().getTimeInMillis();
        private long releaseTime;
        @Builder.Default
        private boolean isConnectionBorrowed = false;
    }

    /**
     * Builder basic sftp client connection manager builder.
     *
     * @return the basic sftp client connection manager builder
     */
    public static BasicSftpClientConnectionManagerBuilder builder() {
        return new BasicSftpClientConnectionManagerBuilder();
    }

    /**
     * The type Basic sftp client connection manager builder.
     */
    public static class BasicSftpClientConnectionManagerBuilder {
        /**
         * The max size of connections all of current process(ClassLoader).
         */
        private int maxConnectionSize = DEFAULT_MAX_CONNECTION_SIZE;
        /**
         * Connection close timeout configuration.
         * Default is 60 seconds.
         * If time out, close the connection.
         */
        private int timeoutMilliSecond = DEFAULT_CLOSE_TIME_OUT_MILLI;
        /**
         * Connection reuse timeout configuration.
         * Default is 15 seconds.
         * If time out, release the connection to reuse.
         */
        private int reuseTimeoutMilliSecond = DEFAULT_REUSE_TIME_OUT_MILLI;
        /**
         * The interval of schedule(Second).
         */
        private long intervalTimeSecond = DEFAULT_SCHEDULE_INTERVAL_TIME_SECOND;

        /**
         * Sets max connection size.
         *
         * @param maxConnectionSize the max connection size
         * @return the max connection size
         */
        public BasicSftpClientConnectionManagerBuilder setMaxConnectionSize(int maxConnectionSize) {
            this.maxConnectionSize = maxConnectionSize;
            return this;
        }

        /**
         * Sets timeout milli second.
         *
         * @param timeoutMilliSecond the timeout milli second
         * @return the timeout milli second
         */
        public BasicSftpClientConnectionManagerBuilder setTimeoutMilliSecond(int timeoutMilliSecond) {
            this.timeoutMilliSecond = timeoutMilliSecond;
            return this;
        }

        /**
         * Sets reuse timeout milli second.
         *
         * @param reuseTimeoutMilliSecond the reuse timeout milli second
         * @return the reuse timeout milli second
         */
        public BasicSftpClientConnectionManagerBuilder setReuseTimeoutMilliSecond(int reuseTimeoutMilliSecond) {
            this.reuseTimeoutMilliSecond = reuseTimeoutMilliSecond;
            return this;
        }

        /**
         * Sets interval time second.
         *
         * @param intervalTimeSecond the interval time second
         * @return the interval time second
         */
        public BasicSftpClientConnectionManagerBuilder setIntervalTimeSecond(long intervalTimeSecond) {
            this.intervalTimeSecond = intervalTimeSecond;
            return this;
        }

        /**
         * Build basic sftp client connection manager.
         *
         * @return the basic sftp client connection manager
         */
        public BasicSftpClientConnectionManager build() {
            return new BasicSftpClientConnectionManager(maxConnectionSize,
                    timeoutMilliSecond,
                    reuseTimeoutMilliSecond,
                    intervalTimeSecond);
        }
    }
}