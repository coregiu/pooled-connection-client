/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.protocol.ISftpConnection;
import lombok.Builder;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The sftp client implements.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 15:42
 */
public class SftpImpl implements ISftpConnection {
    private static final Logger LOG = LoggerFactory.getLogger(SftpImpl.class);
    private static final String SEPARATOR = "/";
    /**
     * The constant DIRECTORY_NOT_EXISTS.
     */
    private static final String DIRECTORY_NOT_EXISTS = "Directory not exists!";
    private static Properties sshConfig = new Properties();
    private static final String CHANNEL_TYPE = "sftp";
    private ChannelSftp channelSftp;
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
    public ChannelSftp getChannelSftp() {
        return channelSftp;
    }

    /**
     * Current directory string.
     *
     * @return the string
     * @throws ConnectionException the connection exception
     */
    @Override
    public String currentDirectory() throws ConnectionException {
        try {
            return channelSftp.pwd();
        } catch (SftpException e) {
            throw new ConnectionException("Failed to get current directory");
        }
    }

    /**
     * List list.
     *
     * @param path the path
     * @return the list
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<String> list(@NonNull String path) {
        try {
            return channelSftp.ls(path);
        } catch (SftpException e) {
            LOG.error("Failed to get the files!");
            return new ArrayList<>(0);
        }
    }

    /**
     * Rename.
     *
     * @param oldPath the old path
     * @param newPath the new path
     * @throws ConnectionException the connection exception
     */
    @Override
    public void rename(@NonNull String oldPath, @NonNull String newPath) throws ConnectionException {
        try {
            channelSftp.rename(oldPath, newPath);
        } catch (SftpException e) {
            LOG.error("Failed to rename the file!");
            throw new ConnectionException("Failed to rename the file!");
        }
    }

    /**
     * Download input stream.
     *
     * @param dir  the dir
     * @param name the name
     * @return the input stream
     * @throws ConnectionException the connection exception
     */
    @Override
    public InputStream download(@NonNull String dir, @NonNull String name) throws ConnectionException {
        if (!isExist(dir)) {
            LOG.error("The directory: {} is not existed!", dir);
            throw new ConnectionException(String.format("The directory: %s is not existed!", dir));
        }
        String filePath = dir + SEPARATOR + name;
        if (!isExist(filePath)) {
            LOG.error("The file: {} is not existed!", filePath);
            throw new ConnectionException(String.format("The file: %s is not existed!", filePath));
        }
        try {
            channelSftp.cd(dir);
            return channelSftp.get(name);
        } catch (SftpException e) {
            throw new ConnectionException("Failed to get the file from ftp server!");
        }
    }

    /**
     * Upload.
     *
     * @param dir  the dir
     * @param name the name
     * @param in   the in
     * @throws ConnectionException the connection exception
     */
    @Override
    public void upload(@NonNull String dir, @NonNull String name, @NonNull InputStream in) throws ConnectionException {
        try {
            if (!isDirectory(dir)) {
                LOG.error("Directory not exists, make new directory!");
                mkdirs(dir);
            }
            channelSftp.cd(dir);
            channelSftp.put(in, name);
        } catch (SftpException e) {
            LOG.error("Failed to upload the file");
            throw new ConnectionException("Failed to upload the file!");
        }
    }

    /**
     * Delete directory.
     *
     * @param dir the dir
     * @throws ConnectionException the connection exception
     */
    @Override
    public void deleteDirectory(@NonNull String dir) throws ConnectionException {
        if (!isDirectory(dir)) {
            LOG.error(DIRECTORY_NOT_EXISTS);
            return;
        }
        try {
            channelSftp.rmdir(dir);
        } catch (SftpException e) {
            LOG.error("Failed to delete the directory, please check the directory is empty!");
            throw new ConnectionException("Failed to delete the directory, please check the directory is empty!");
        }
    }

    /**
     * Delete file.
     *
     * @param dir  the dir
     * @param name the name
     * @throws ConnectionException the connection exception
     */
    @Override
    public void deleteFile(@NonNull String dir, @NonNull String name) throws ConnectionException {
        if (!isDirectory(dir)) {
            LOG.error(DIRECTORY_NOT_EXISTS);
            return;
        }
        String filePath = dir + SEPARATOR + name;
        if (!isExist(filePath)) {
            LOG.error("File not exists!");
            return;
        }
        try {
            channelSftp.cd(dir);
            channelSftp.rm(name);
        } catch (SftpException e) {
            LOG.error("Failed to delete the file");
            throw new ConnectionException("Failed to delete the file");
        }
    }

    /**
     * Mkdirs.
     *
     * @param dir the dir
     * @throws ConnectionException the connection exception
     */
    @Override
    public void mkdirs(@NonNull String dir) throws ConnectionException {
        try {
            if (isDirectory(dir)) {
                LOG.info("The directory is exists!");
                return;
            }
            int separatorPos = dir.lastIndexOf(SEPARATOR);
            if (separatorPos == 0) {
                channelSftp.mkdir(dir);
                channelSftp.cd(dir);
                return;
            }
            String preDir = dir.substring(0, dir.lastIndexOf(SEPARATOR));
            mkdirs(preDir);
            channelSftp.mkdir(dir);
            channelSftp.cd(dir);
        } catch (SftpException e) {
            LOG.error("Failed to create directory！");
            throw new ConnectionException("Failed to create directory");
        }
    }

    /**
     * Is exist boolean.
     *
     * @param path the path
     * @return the boolean
     */
    @Override
    public boolean isExist(@NonNull String path) {
        try {
            channelSftp.lstat(path);
            return true;
        } catch (SftpException e) {
            return false;
        }
    }

    /**
     * Is directory boolean.
     *
     * @param path the path
     * @return the boolean
     */
    @Override
    public boolean isDirectory(String path) {
        try {
            SftpATTRS attrs = channelSftp.lstat(path);
            return attrs.isDir();
        } catch (SftpException e) {
            LOG.error(DIRECTORY_NOT_EXISTS);
            return false;
        }
    }

    /**
     * Is file boolean.
     *
     * @param path the path
     * @return the boolean
     */
    @Override
    public boolean isFile(@NonNull String path) {
        try {
            SftpATTRS attrs = channelSftp.lstat(path);
            return (!attrs.isDir());
        } catch (SftpException e) {
            LOG.error("file not exists!");
            return false;
        }
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
            ChannelSftp channel = (ChannelSftp) sshSession.openChannel(CHANNEL_TYPE);
            channel.connect();
            this.channelSftp = channel;
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
     * Is connection valid.
     *
     * @return the boolean
     */
    @Override
    public boolean isValid() {
        return channelSftp != null && channelSftp.isConnected();
    }

    /**
     * Was connection closed.
     *
     * @return the boolean
     */
    @Override
    public boolean isClosed() {
        return channelSftp != null && channelSftp.isClosed();
    }
}