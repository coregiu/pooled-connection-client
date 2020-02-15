package com.xvzhu.connections.sftp;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.ISftpConnection;
import com.xvzhu.connections.mockserver.SftpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 0:39
 */
public class SftpImplTest {
    private static final Logger LOG = LoggerFactory.getLogger(SftpImplTest.class);
    private SftpServer sftpServer;
    private ISftpConnection sftpConnection;
    @Before
    public void sftpImplTest() throws InterruptedException{
        LOG.error("Begin to start server.");
        sftpServer = new SftpServer();
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sftpServer.setupSftpServer();
                }
            }).start();
        } catch (Exception e) {
            LOG.error("Failed to init test.", e);
        }
        Thread.sleep(10);
        ConnectionBean connectionBean = new ConnectionBean("127.0.0.1", 2222, "huawei", "huawei");
        sftpConnection = SftpConnectionFactory.builder().connectionBean(connectionBean).build().create();
    }

    @Test
    public void should_return_current_directory_when_query_pwd() {
        try {
            File file = new File("");
            LOG.error("current file : {}", file.getAbsolutePath());
            LOG.error("Sftp path is : {}", sftpConnection.currentDirectory());
            String localFilePath = file.getAbsolutePath().replace("\\", "").replace("/", "");
            String sftpFilePath = sftpConnection.currentDirectory().replace("\\", "").replace("/", "");
            assertThat(localFilePath, is(sftpFilePath));
        } catch (ConnectionException e) {
            LOG.error("Failed to query path", e);
        }
    }

    @Test
    public void should_return_true_when_check_current_directory_is_directory() {
        try {
            LOG.error("Sftp path is : {}", sftpConnection.currentDirectory());
            assertThat(sftpConnection.isDirectory(sftpConnection.currentDirectory()), is(true));
        } catch (ConnectionException e) {
            LOG.error("Failed to query path", e);
        }
    }

    @After
    public void shutdownSftp(){
        LOG.error("Begin to shutdown server.");
        sftpServer.shutdown();
    }
}
