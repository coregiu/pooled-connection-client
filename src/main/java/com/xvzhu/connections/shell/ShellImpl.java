/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.shell;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.protocol.IShellConnection;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Properties;

/**
 * The sftp client implements.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 15:42
 */
public class ShellImpl implements IShellConnection {
    private static final Logger LOG = LoggerFactory.getLogger(ShellImpl.class);
    /**
     * The constant DIRECTORY_NOT_EXISTS.
     */
    private static Properties sshConfig = new Properties();
    private static final String CHANNEL_TYPE = "shell";
    private ChannelShell channelShell;
    @Builder.Default
    private JSch jsch = new JSch();

    static {
        sshConfig.put("StrictHostKeyChecking", "no");
    }

    /**
     * Gets channel sftp.
     *
     * @return the channel sftp
     */
    @Override
    public ChannelShell getChannelShell() {
        return channelShell;
    }

    /**
     * Connect sftp connection.
     *
     * @param connectionBean     the connection bean
     * @param timeoutMilliSecond the timeout milli second
     * @throws ConnectionException the connection exception
     */
    @Override
    public void connect(ConnectionBean connectionBean, int timeoutMilliSecond) throws ConnectionException {
        try {
            Session sshSession = jsch.getSession(connectionBean.getUsername(),
                    connectionBean.getHost(),
                    connectionBean.getPort());
            sshSession.setPassword(connectionBean.getPassword());
            sshSession.setConfig(sshConfig);
            sshSession.setTimeout(timeoutMilliSecond);
            sshSession.connect();
            ChannelShell channel = (ChannelShell) sshSession.openChannel(CHANNEL_TYPE);
            channel.connect();
            this.channelShell = channel;
        } catch (JSchException e) {
            LOG.error("Failed to create connection");
            throw new ConnectionException("Failed to connect the ftp server", e);
        }
    }

    /**
     * Disconnect.
     */
    @Override
    public void disconnect() {
        if (channelShell != null) {
            channelShell.disconnect();
            try {
                channelShell.getSession().disconnect();
            } catch (JSchException e) {
                LOG.error("Failed to close the session", e);
            }
        }
    }

    /**
     * Is connection valid.
     *
     * @return the boolean
     */
    @Override
    public boolean isValid() {
        return channelShell != null && channelShell.isConnected();
    }

    /**
     * Was connection closed.
     *
     * @return the boolean
     */
    @Override
    public boolean isClosed() {
        return channelShell != null && channelShell.isClosed();
    }
}