package com.xvzhu.connections;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IConnectionMonitor;
import com.xvzhu.connections.apis.IObserver;
import com.xvzhu.connections.apis.ISftpConnection;
import com.xvzhu.connections.monitor.ConnectionMonitor;
import com.xvzhu.connections.sftp.SftpConnectionFactory;
import lombok.AllArgsConstructor;
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
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 14:22
 */
@Builder
@AllArgsConstructor
public class BasicSftpClientConnectionManager<T> implements IConnectionManager {
    private static final Logger LOG = LoggerFactory.getLogger(BasicSftpClientConnectionManager.class);
    private static final int DEFAULT_CONNECTION_SIZE = 8;
    private static final int DEFAULT_TIME_OUT_MILLI = 15000;
    private static final long DEFAULT_SCHEDULE_INTERVAL_TIME_SECOND = 60L;
    private static final int MAX_TIME_OUT_MILLI = 3600000;

    /**
     * monitor container.<br>
     * Static container for monitor all connections for each host, thread.
     */
    private static Map<ConnectionBean, ThreadLocal<ManagerBean>> connections
            = new ConcurrentHashMap<>(DEFAULT_CONNECTION_SIZE);

    private static IConnectionMonitor connectionMonitor = ConnectionMonitor.getInstance();

    private BasicSftpClientConnectionManager() {
        connectionMonitor.setIntervalTimeSecond(intervalTimeSecond);
    }

    /**
     * Connection timeout.
     * Default is 10 seconds.
     */
    @Builder.Default
    private int timeoutMilliSecond = DEFAULT_TIME_OUT_MILLI;
    @Builder.Default
    private long intervalTimeSecond = DEFAULT_SCHEDULE_INTERVAL_TIME_SECOND;

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

    @Override
    public void accept(IObserver observer, ConnectionBean connectionBean) {
        observer.visit(this, connectionBean);
    }

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
            return (T)managerBean.getSftpConnection();
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
                    timeoutMilliSecond > 0 && timeoutMilliSecond < MAX_TIME_OUT_MILLI ? DEFAULT_TIME_OUT_MILLI : timeoutMilliSecond;
            managerBean = ManagerBean.builder()
                    .isConnectionBorrowed(true)
                    .sftpConnection(connection)
                    .timeOutMilli(timeoutMilliSecond).build();
            ThreadLocal<ManagerBean> managerBeanThreadLocal = new ThreadLocal<>();
            managerBeanThreadLocal.set(managerBean);
            connections.put(connectionBean, managerBeanThreadLocal);
            LOG.debug("New a connection for host {}, thread {}",
                    connectionBean.getHost(), Thread.currentThread().getName());
            return (T) connection;
        }
    }

    @Data
    @Builder
    static class ManagerBean {
        @Builder.Default
        private Object lock = new Object();
        private ISftpConnection sftpConnection;
        @Builder.Default
        private long timeOutMilli = DEFAULT_TIME_OUT_MILLI;
        @Builder.Default
        private long borrowTime = Calendar.getInstance().getTimeInMillis();
        private long releaseTime;
        @Builder.Default
        private boolean isConnectionBorrowed = false;
    }
}