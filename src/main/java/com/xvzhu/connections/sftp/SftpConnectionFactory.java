package com.xvzhu.connections.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.ISftpConnection;
import lombok.Builder;
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
@Builder
public class SftpConnectionFactory extends BasePooledObjectFactory<ISftpConnection> {
    private static final Logger LOG = LoggerFactory.getLogger(SftpConnectionFactory.class);
    private static final int DEFAULT_TIME_OUT_MILLI = 15000;
    private static final String CHANNEL_TYPE = "sftp";
    private static Properties sshConfig = new Properties();
    @Builder.Default
    private JSch jsch = new JSch();
    @Builder.Default
    private int timeoutMilliSecond = DEFAULT_TIME_OUT_MILLI;
    private ConnectionBean connectionBean;

    static {
        sshConfig.put("StrictHostKeyChecking", "no");
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
            Session sshSession = jsch.getSession(connectionBean.getUsername(),
                    connectionBean.getHost(),
                    connectionBean.getPort());
            sshSession.setPassword(connectionBean.getPassword());
            sshSession.setConfig(sshConfig);
            sshSession.setTimeout(timeoutMilliSecond);
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
        if (sftp == null) {
            LOG.warn("The sftp is null");
            return;
        }
        ChannelSftp channelSftp = sftp.getChannelSftp();
        if (channelSftp != null) {
            channelSftp.disconnect();
            try {
                channelSftp.getSession().disconnect();
            } catch (JSchException e) {
                LOG.error("Failed to close the session", e);
            }
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