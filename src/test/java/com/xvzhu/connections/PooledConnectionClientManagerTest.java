package com.xvzhu.connections;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.ISftpConnection;
import com.xvzhu.connections.data.ConnectionBeanBuilder;
import com.xvzhu.connections.mockserver.SftpServer;
import com.xvzhu.connections.sftp.SftpConnectionFactory;
import com.xvzhu.connections.sftp.SftpImplTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-22 16:59
 */
public class PooledConnectionClientManagerTest {
    private static final Logger LOG = LoggerFactory.getLogger(SftpImplTest.class);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private SftpServer sftpServer;
    private ISftpConnection sftpConnection;
    private int port;

    @Before
    public void sftpImplTest() throws InterruptedException, ConnectionException {
        LOG.error("Begin to start server.");
        sftpServer = new SftpServer();
        String uuid = sftpServer.getUuid();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        sftpServer.setupSftpServer(uuid, countDownLatch);
        countDownLatch.await();
        port = sftpServer.getPort(uuid);
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().port(port).build().getConnectionBean();
        sftpConnection = SftpConnectionFactory.builder().connectionBean(connectionBean).build().create();
    }

    @After
    public void shutdownSftp() {
        LOG.error("Begin to shutdown server.");
        sftpServer.shutdown();
        sftpConnection.getChannelSftp().disconnect();
    }

    @Test
    public void should_successfully_borrow_connection_when_create_new_pooled_manager() throws ConnectionException {
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().port(port).build().getConnectionBean();
        IConnectionManager<ISftpConnection> manager = PooledSftpClientConnectionManager.builder()
                .setBorrowMaxWaitTimeMS(8000)
                .setConnectionBean(connectionBean)
                .build(ISftpConnection.class);
        try {
            ISftpConnection sftpConnection = manager.borrowConnection(connectionBean);
            LOG.error("current path = {}", sftpConnection.currentDirectory());
            assertTrue(sftpConnection.currentDirectory().length() > 0);
        } finally {
            manager.closeConnection(connectionBean);
        }
    }
}
