package com.xvzhu.connections.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.ISftpConnection;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.Properties;

/**
 * The type Sftp connection factory.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 16:13
 */
public class SftpConnectionFactory extends BasePooledObjectFactory<ISftpConnection> {
    private static final Logger LOG = LoggerFactory.getLogger(SftpConnectionFactory.class);
    private static final String CHANNEL_TYPE = "sftp";
    private static Properties sshConfig = new Properties();
    private JSch jsch = new JSch();
    private ConnectionBean connectionBean;
    private Session sshSession;

    static {
        sshConfig.put("StrictHostKeyChecking", "no");
    }

    /**
     * The type Sftp connection factory builder.
     */
    public static class SftpConnectionFactoryBuilder {
        private ConnectionBean connectionBean;

        /**
         * Sets connection bean.
         *
         * @param connectionBean the connection bean
         * @return the connection bean
         */
        public SftpConnectionFactoryBuilder setConnectionBean(ConnectionBean connectionBean) {
            this.connectionBean = connectionBean;
            return this;
        }

        /**
         * Build sftp connection factory.
         *
         * @return the sftp connection factory
         */
        public SftpConnectionFactory build() {
            return new SftpConnectionFactory(connectionBean);
        }
    }

    /**
     * Builder sftp connection factory builder.
     *
     * @return the sftp connection factory builder
     */
    public static SftpConnectionFactoryBuilder builder() {
        return new SftpConnectionFactoryBuilder();
    }

    private SftpConnectionFactory(ConnectionBean connectionBean) {
        this.connectionBean = connectionBean;
    }

    /**
     * Build sftp connection factory.
     *
     * @return the sftp connection factory
     */
    public SftpConnectionFactory build() {
        return new SftpConnectionFactory(connectionBean);
    }

    /**
     * <p>Create sftp connection.</p>
     *
     * @return the sftp connection
     * @throws ConnectionException the connection exception
     */
    @Override
    public ISftpConnection create() throws ConnectionException {
        try {
            sshSession = jsch.getSession(connectionBean.getUsername(),
                    connectionBean.getHost(),
                    connectionBean.getPort());
            sshSession.setPassword(connectionBean.getPassword());
            sshSession.setConfig(sshConfig);
            sshSession.connect();
            ChannelSftp channel = (ChannelSftp) sshSession.openChannel(CHANNEL_TYPE);
            channel.connect();
            return new SftpImpl(channel);
        } catch (JSchException e) {
            LOG.error("Failed to create connection");
            throw new ConnectionException("Failed to connect the ftp server", e);
        }
    }

    /**
     * <p>Wrap pooled object.</p>
     *
     * @param connection the sftp
     * @return the pooled object
     */
    @Override
    public PooledObject<ISftpConnection> wrap(ISftpConnection connection) {
        return new DefaultPooledObject<>(connection);
    }

    /**
     * <p>Close the connection.</p>
     *
     * @param connectionPool the connection pool.
     */
    @Override
    public void destroyObject(PooledObject<ISftpConnection> connectionPool) {
        if (connectionPool == null) {
            LOG.warn("The connection pool is null");
            return;
        }
        ISftpConnection sftp = connectionPool.getObject();
        if (sftp != null) {
            ChannelSftp channelSftp = sftp.getChannelSftp();
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
        }
        if (sshSession != null) {
            sshSession.disconnect();
        }
    }

    /**
     * <p>Validate the connection is connected.</p>
     *
     * @param connectionPool the the connection pool.
     * @return the boolean
     */
    @Override
    public boolean validateObject(PooledObject<ISftpConnection> connectionPool) {
        if (connectionPool == null) {
            LOG.warn("The connection pool is null");
            return false;
        }
        ISftpConnection sftp = connectionPool.getObject();
        return (sftp != null) && (!sftp.getChannelSftp().isClosed());
    }
}