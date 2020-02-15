package com.xvzhu.connections.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.ISftpConnection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The sftp client implements.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 15:42
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SftpImpl implements ISftpConnection {
    private static final Logger LOG = LoggerFactory.getLogger(SftpImpl.class);
    private static final int BYTE_DEFAULT_SIZE = 4096;
    private static final String SEPARATOR = "/";

    private ChannelSftp channelSftp;

    @Override
    public String currentDirectory() throws ConnectionException {
        try {
            return channelSftp.pwd();
        } catch (SftpException e) {
            throw new ConnectionException("Failed to get current directory");
        }
    }

    @Override
    public List<String> list(@NonNull String path) {
        try {
            return channelSftp.ls(path);
        } catch (SftpException e) {
            LOG.error("Failed to get the files!");
            return new ArrayList<>(0);
        }
    }

    @Override
    public void rename(@NonNull String oldPath, @NonNull String newPath) throws ConnectionException {
        try {
            channelSftp.rename(oldPath, newPath);
        } catch (SftpException e) {
            LOG.error("Failed to rename the file!");
            throw new ConnectionException("Failed to rename the file!");
        }
    }

    @Override
    public byte[] download(@NonNull String dir, @NonNull String name) throws ConnectionException {
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
            InputStream in = channelSftp.get(name);
            return inputStreamToByteArray(in);
        } catch (IOException e) {
            throw new ConnectionException("Failed to convert the file stream!");
        } catch (SftpException e) {
            throw new ConnectionException("Failed to get the file from ftp server!");
        }
    }

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

    @Override
    public void deleteDirectory(@NonNull String dir) throws ConnectionException {
        if (!isDirectory(dir)) {
            LOG.error("Directory not exists!");
            return;
        }
        try {
            channelSftp.rmdir(dir);
        } catch (SftpException e) {
            LOG.error("Failed to delete the directory, please check the directory is empty!");
            throw new ConnectionException("Failed to delete the directory, please check the directory is empty!");
        }
    }

    @Override
    public void deleteFile(@NonNull String dir, @NonNull String name) throws ConnectionException {
        if (!isDirectory(dir)) {
            LOG.error("Directory not exists!");
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

    @Override
    public boolean isExist(@NonNull String path) {
        try {
            channelSftp.lstat(path);
            return true;
        } catch (SftpException e) {
            return false;
        }
    }

    @Override
    public boolean isDirectory(String path) {
        try {
            SftpATTRS attrs = channelSftp.lstat(path);
            return attrs.isDir();
        } catch (SftpException e) {
            LOG.error("Directory not exists!");
            return false;
        }
    }

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
     * convert input stream to byte array.
     *
     * @param in input
     * @return byte[]
     * @throws IOException the io exception.
     */
    private byte[] inputStreamToByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[BYTE_DEFAULT_SIZE];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }
}