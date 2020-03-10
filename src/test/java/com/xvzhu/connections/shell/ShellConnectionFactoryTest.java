/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.shell;

import com.jcraft.jsch.JSchException;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.protocol.IShellConnection;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test for sft connection factory.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 0:39
 */
public class ShellConnectionFactoryTest {
    private static final Logger LOG = LoggerFactory.getLogger(ShellImplTest.class);
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
    public void should_initialization_when_build_by_constructor() {
        ShellConnectionFactory build = new ShellConnectionFactory(connectionBean, 1000);
        build.setConnectionBean(connectionBean);
        build.setTimeoutMilliSecond(1000);
        ShellConnectionFactory build2 = ShellConnectionFactory.builder().connectionBean(connectionBean).timeoutMilliSecond(1000).build();

        assertTrue(build.canEqual(build2));
        assertNotNull(build.toString());
        assertEquals(build.hashCode(), build2.hashCode());
        assertEquals(build, build2);
    }

    @Test
    public void should_successfully_when_create_connection_using_correct_info() throws ConnectionException, JSchException {
        ShellConnectionFactory build = ShellConnectionFactory.builder().connectionBean(connectionBean).build();
        IShellConnection sftpConnection = build.create();
        try {
            assertThat(sftpConnection.isValid(), is(true));
        } finally {
            PooledObject<IShellConnection> pooledObject = build.wrap(sftpConnection);
            build.destroyObject(pooledObject);
        }
    }

    @Test
    public void should_failed_when_create_connection_using_wrong_info() throws ConnectionException, JSchException {
        expectedException.expect(ConnectionException.class);
        expectedException.expectMessage("Failed to connect the ftp server");
        ConnectionBean connectionBean1 = ConnectionBeanBuilder.builder().password("").build().getConnectionBean();
        ShellConnectionFactory build = ShellConnectionFactory.builder().connectionBean(connectionBean1).build();
        IShellConnection sftpConnection = build.create();
        try {
            assertThat(sftpConnection.isValid(), is(true));
        } finally {
            PooledObject<IShellConnection> pooledObject = build.wrap(sftpConnection);
            build.destroyObject(pooledObject);
        }
    }

    @Test
    public void should_successfully_when_destroy_connection() throws ConnectionException, JSchException {
        ShellConnectionFactory sftpConnectionFactory = ShellConnectionFactory.builder().connectionBean(connectionBean).build();
        IShellConnection sftpConnection = sftpConnectionFactory.create();
        try {
            PooledObject<IShellConnection> connectionPool = sftpConnectionFactory.wrap(sftpConnection);
            assertThat(sftpConnectionFactory.validateObject(connectionPool), is(true));

            sftpConnectionFactory.destroyObject(connectionPool);
            assertThat(sftpConnectionFactory.validateObject(connectionPool), is(false));
        } finally {
            PooledObject<IShellConnection> pooledObject = sftpConnectionFactory.wrap(sftpConnection);
            sftpConnectionFactory.destroyObject(pooledObject);
        }
    }

    @Test
    public void should_init_by_two_parameter_when_invoke_by_operation() {
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().build().getConnectionBean();
        ShellConnectionFactory sftpConnectionFactory = new ShellConnectionFactory(connectionBean, 1000);
        assertNotNull(sftpConnectionFactory);
    }
}
