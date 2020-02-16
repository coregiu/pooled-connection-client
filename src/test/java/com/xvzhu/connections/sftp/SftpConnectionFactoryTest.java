package com.xvzhu.connections.sftp;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.ISftpConnection;
import com.xvzhu.connections.mockserver.SftpServer;
import org.apache.commons.pool2.PooledObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test for sft connection factory.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 0:39
 */
public class SftpConnectionFactoryTest {
    private static final Logger LOG = LoggerFactory.getLogger(SftpImplTest.class);
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private SftpServer sftpServer;

    @Before
    public void sftpImplTest() throws InterruptedException {
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
    }

    @After
    public void shutdownSftp() {
        LOG.error("Begin to shutdown server.");
        sftpServer.shutdown();
    }

    @Test
    public void should_successfully_when_create_connection_using_correct_info() throws ConnectionException {
        ConnectionBean connectionBean = new ConnectionBean("127.0.0.1", 2222, "huawei", "huawei");
        ISftpConnection sftpConnection = SftpConnectionFactory.builder().setConnectionBean(connectionBean).build().create();
        try {
            assertThat(sftpConnection.currentDirectory() != null, is(true));
        } finally {
            sftpConnection.getChannelSftp().disconnect();
        }
    }

    @Test
    public void should_failed_when_create_connection_using_wrong_info() throws ConnectionException {
        expectedException.expect(ConnectionException.class);
        expectedException.expectMessage("Failed to connect the ftp server");
        ConnectionBean connectionBean = new ConnectionBean("127.0.0.1", 2222, "huawei", "");
        ISftpConnection sftpConnection = SftpConnectionFactory.builder().setConnectionBean(connectionBean).build().create();
        try {
            assertThat(sftpConnection.currentDirectory() != null, is(true));
        } finally {
            if (sftpConnection != null) {
                sftpConnection.getChannelSftp().disconnect();
            }
        }
    }

    @Test
    public void should_successfully_when_destroy_connection() throws ConnectionException {
        ConnectionBean connectionBean = new ConnectionBean("127.0.0.1", 2222, "huawei", "huawei");
        SftpConnectionFactory sftpConnectionFactory = SftpConnectionFactory.builder().setConnectionBean(connectionBean).build();
        ISftpConnection sftpConnection = sftpConnectionFactory.create();
        try {
            PooledObject<ISftpConnection> connectionPool = sftpConnectionFactory.wrap(sftpConnection);
            assertThat(sftpConnectionFactory.validateObject(connectionPool), is(true));

            sftpConnectionFactory.destroyObject(connectionPool);
            assertThat(sftpConnectionFactory.validateObject(connectionPool), is(false));
        } finally {
            if (sftpConnection != null) {
                sftpConnection.getChannelSftp().disconnect();
            }
        }
    }
}
