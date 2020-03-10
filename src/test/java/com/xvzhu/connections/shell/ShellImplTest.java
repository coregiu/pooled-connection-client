/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.shell;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.protocol.IShellConnection;
import com.xvzhu.connections.data.ConnectionBeanBuilder;
import com.xvzhu.connections.mockserver.SftpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 0:39
 */
public class ShellImplTest {
    private static final Logger LOG = LoggerFactory.getLogger(ShellImplTest.class);
    private static final int BYTE_DEFAULT_SIZE = 4096;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private SftpServer sftpServer;
    private IShellConnection shellConnection;

    @Before
    public void sftpImplTest() throws InterruptedException, ConnectionException {
        LOG.error("Begin to start server.");
        sftpServer = new SftpServer();
        String uuid = sftpServer.getUuid();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        sftpServer.setupSftpServer(uuid, countDownLatch);
        countDownLatch.await();
        int port = sftpServer.getPort(uuid);
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().port(port).build().getConnectionBean();
        shellConnection = new ShellImpl();
        shellConnection.connect(connectionBean, 10000);
    }

    @After
    public void shutdownSftp() throws ConnectionException{
        LOG.error("Begin to shutdown server.");
        sftpServer.shutdown();
        shellConnection.disconnect();
    }

    @Test
    public void should_create_connection_when_with_correct_information() throws ConnectionException{
        assertNotNull(shellConnection.getChannelShell());
        assertTrue(shellConnection.isValid());
    }

    @Test
    public void should_shutdown_connection_when_close_it() throws ConnectionException{
        assertNotNull(shellConnection.getChannelShell());
        assertTrue(shellConnection.isValid());
        assertFalse(shellConnection.isClosed());
        shellConnection.disconnect();
        assertFalse(shellConnection.isValid());
        assertTrue(shellConnection.isClosed());
    }
}