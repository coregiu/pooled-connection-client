/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.sftp;

import com.jcraft.jsch.JSchException;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.protocol.ISftpConnection;
import com.xvzhu.connections.data.ConnectionBeanBuilder;
import com.xvzhu.connections.mockserver.SftpServer;
import org.apache.commons.pool2.PooledObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
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
    private ConnectionBean connectionBean;

    @Before
    public void sftpImplTest() throws InterruptedException {
        LOG.error("Begin to start server.");
        sftpServer = new SftpServer();
        String uuid = sftpServer.getUuid();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        sftpServer.setupSftpServer(uuid, countDownLatch);
        countDownLatch.await();
        int port = sftpServer.getPort(uuid);
        connectionBean = ConnectionBeanBuilder.builder().port(port).build().getConnectionBean();
    }

    @After
    public void shutdownSftp() {
        LOG.error("Begin to shutdown server.");
        sftpServer.shutdown();
    }

    @Test
    public void should_successfully_when_create_connection_using_correct_info() throws ConnectionException, JSchException {
        SftpConnectionFactory build = SftpConnectionFactory.builder().connectionBean(connectionBean).build();
        ISftpConnection sftpConnection = build.create();
        try {
            assertThat(sftpConnection.currentDirectory() != null, is(true));
        } finally {
            PooledObject<ISftpConnection> pooledObject = build.wrap(sftpConnection);
            build.destroyObject(pooledObject);
        }
    }

    @Test
    public void should_failed_when_create_connection_using_wrong_info() throws ConnectionException, JSchException {
        expectedException.expect(ConnectionException.class);
        expectedException.expectMessage("Failed to connect the ftp server");
        ConnectionBean connectionBean1 = ConnectionBeanBuilder.builder().password("").build().getConnectionBean();
        SftpConnectionFactory build = SftpConnectionFactory.builder().connectionBean(connectionBean1).build();
        ISftpConnection sftpConnection = build.create();
        try {
            assertThat(sftpConnection.currentDirectory() != null, is(true));
        } finally {
            PooledObject<ISftpConnection> pooledObject = build.wrap(sftpConnection);
            build.destroyObject(pooledObject);
        }
    }

    @Test
    public void should_successfully_when_destroy_connection() throws ConnectionException, JSchException {
        SftpConnectionFactory sftpConnectionFactory = SftpConnectionFactory.builder().connectionBean(connectionBean).build();
        ISftpConnection sftpConnection = sftpConnectionFactory.create();
        try {
            PooledObject<ISftpConnection> connectionPool = sftpConnectionFactory.wrap(sftpConnection);
            assertThat(sftpConnectionFactory.validateObject(connectionPool), is(true));

            sftpConnectionFactory.destroyObject(connectionPool);
            assertThat(sftpConnectionFactory.validateObject(connectionPool), is(false));
        } finally {
            PooledObject<ISftpConnection> pooledObject = sftpConnectionFactory.wrap(sftpConnection);
            sftpConnectionFactory.destroyObject(pooledObject);
        }
    }

    @Test
    public void should_init_by_two_parameter_when_invoke_by_operation() {
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().build().getConnectionBean();
        SftpConnectionFactory sftpConnectionFactory = new SftpConnectionFactory(connectionBean, 1000);
        assertNotNull(sftpConnectionFactory);
    }
}
