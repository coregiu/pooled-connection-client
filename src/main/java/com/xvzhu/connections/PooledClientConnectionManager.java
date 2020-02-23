package com.xvzhu.connections;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.ConnectionManagerConfig;
import com.xvzhu.connections.apis.protocol.IConnection;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IConnectionMonitor;
import com.xvzhu.connections.apis.IObserver;
import com.xvzhu.connections.apis.protocol.ISftpConnection;
import com.xvzhu.connections.monitor.ConnectionMonitor;
import com.xvzhu.connections.sftp.SftpConnectionFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * The type Pooled sftp client connection manager.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 14:00
 */
public class PooledClientConnectionManager implements IConnectionManager {
    private static final Logger LOG = LoggerFactory.getLogger(BasicClientConnectionManager.class);

    private static ConnectionManagerConfig connectionManagerConfig = ConnectionManagerConfig.builder().build();

    private IConnectionMonitor connectionMonitor = ConnectionMonitor.getInstance();

    private GenericObjectPool<IConnection> connectionPool;

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
    public <T> T borrowConnection(ConnectionBean connectionBean, Class<T> clazz) throws ConnectionException {
        try {
            return (T) connectionPool.borrowObject(connectionManagerConfig.getBorrowMaxWaitTimeMS());
        } catch (Exception e) {
            LOG.error("Failed to borrow connection", e);
            throw new ConnectionException("Failed to borrow connection.", e);
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
    }

    /**
     * Accept.
     *
     * @param observer       the observer
     * @param connectionBean the connection bean
     */
    @Override
    public void accept(IObserver observer, ConnectionBean connectionBean) {
        observer.visit(this, connectionBean, null);
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
     *
     * @param <T> the type parameter
     */
    public static class PooledSftpClientConnectionManagerBuilder<T extends IConnection> {
        private GenericObjectPoolConfig<T> connectionConfig = new GenericObjectPoolConfig<>();
        private AbandonedConfig abandonedConfig = new AbandonedConfig();
        private ConnectionBean connectionBean;

        /**
         * Sets connection config.
         *
         * @param connectionConfig the connection config
         * @return the connection config
         */
        public PooledSftpClientConnectionManagerBuilder setConnectionConfig(GenericObjectPoolConfig<T> connectionConfig) {
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
         * Sets connection bean.
         *
         * @param connectionBean the connection bean
         * @return the connection bean
         */
        public PooledSftpClientConnectionManagerBuilder setConnectionBean(ConnectionBean connectionBean) {
            this.connectionBean = connectionBean;
            return this;
        }

        /**
         * Build pooled sftp client connection manager.
         *
         * @param type the type
         * @return the pooled sftp client connection manager
         * @throws ConnectionException the connection exception
         */
        public PooledClientConnectionManager build(Class<T> type) throws ConnectionException {
            if (ISftpConnection.class.equals(type)) {
                SftpConnectionFactory sftpConnectionFactory = SftpConnectionFactory.builder().connectionBean(connectionBean).build();
                GenericObjectPool<IConnection> connectionPool = new GenericObjectPool(sftpConnectionFactory, connectionConfig, abandonedConfig);

                return new PooledClientConnectionManager(connectionPool);
            }
            throw new ConnectionException(String.format(Locale.ENGLISH, "Protocol %s was not supported", ISftpConnection.class.getName()));
        }
    }
}
