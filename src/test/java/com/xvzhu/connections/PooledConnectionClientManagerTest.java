/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.protocol.ISftpConnection;
import com.xvzhu.connections.data.ConnectionBeanBuilder;
import com.xvzhu.connections.mockserver.SftpServer;
import com.xvzhu.connections.sftp.SftpConnectionFactory;
import com.xvzhu.connections.sftp.SftpImplTest;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
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
        IConnectionManager manager = PooledClientConnectionManager.builder()
                .setBorrowMaxWaitTimeMS(8000)
                .build(connectionBean, ISftpConnection.class);
        try {
            ISftpConnection sftpConnection = manager.borrowConnection(connectionBean, ISftpConnection.class);
            LOG.error("current path = {}", sftpConnection.currentDirectory());
            assertTrue(sftpConnection.currentDirectory().length() > 0);
        } finally {
            manager.releaseConnection(connectionBean);
            manager.closeConnection(connectionBean);
        }
    }

    @Test
    public void should_change_connection_configuration_when_create_pooled_manager_with_parameters() throws ConnectionException {
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().port(port).build().getConnectionBean();
        IConnectionManager manager = PooledClientConnectionManager.builder()
                .setBorrowMaxWaitTimeMS(8000)
                .setAbandonedConfig(new AbandonedConfig())
                .setConnectionConfig(new GenericObjectPoolConfig<>())
                .setSchedulePeriodTimeMS(10000)
                .setAutoInspect(false)
                .build(connectionBean, ISftpConnection.class);
        try {
            ISftpConnection sftpConnection = manager.borrowConnection(connectionBean, ISftpConnection.class);
            LOG.error("current path = {}", sftpConnection.currentDirectory());
            assertTrue(sftpConnection.currentDirectory().length() > 0);
        } finally {
            manager.closeConnection(connectionBean);
        }
    }
}
