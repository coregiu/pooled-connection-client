package com.xvzhu.connections.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ISftpConnection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.Properties;
import java.util.concurrent.CompletionException;

/**
 * The type Sftp connection factory.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 16:13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SftpConnectionFactory extends BasePooledObjectFactory<ISftpConnection> {
    private static final Logger LOG = LoggerFactory.getLogger(SftpConnectionFactory.class);
    private static final String CHANNEL_TYPE = "sftp";
    private static Properties sshConfig = new Properties();
    private ConnectionBean connectionBean;

    static {
        sshConfig.put("StrictHostKeyChecking", "no");
    }

    /**
     * Create sftp connection.
     *
     * @return the sftp connection
     */
    @Override
    public ISftpConnection create() {
        try {
            JSch jsch = new JSch();
            Session sshSession = jsch.getSession(connectionBean.getUsername(),
                    connectionBean.getHost(),
                    connectionBean.getPort());
            sshSession.setPassword(connectionBean.getPassword());
            sshSession.setConfig(sshConfig);
            sshSession.connect();
            ChannelSftp channel = (ChannelSftp) sshSession.openChannel(CHANNEL_TYPE);
            channel.connect();
            return new SftpImpl(channel);
        } catch (JSchException e) {
            throw new CompletionException("Failed to connect the ftp server", e);
        }
    }

    /**
     * Wrap pooled object.
     *
     * @param sftp the sftp
     * @return the pooled object
     */
    @Override
    public PooledObject<ISftpConnection> wrap(ISftpConnection sftp) {
        return new DefaultPooledObject<>(sftp);
    }

    /**
     * Destroy object.
     *
     * @param p the p
     */
    @Override
    public void destroyObject(PooledObject<ISftpConnection> p) {
        if (p != null) {
            ISftpConnection sftp = p.getObject();
            if (sftp != null) {
                ChannelSftp channelSftp = sftp.getChannelSftp();
                if (channelSftp != null) {
                    channelSftp.disconnect();
                }
            }
        }
    }

    /**
     * Validate object boolean.
     *
     * @param p the p
     * @return the boolean
     */
    @Override
    public boolean validateObject(PooledObject<ISftpConnection> p) {
        if (p != null) {
            ISftpConnection sftp = p.getObject();
            if (sftp != null) {
                try {
                    sftp.getChannelSftp().pwd();
                    return true;
                } catch (SftpException e) {
                    return false;
                }
            }
        }
        return false;
    }
}