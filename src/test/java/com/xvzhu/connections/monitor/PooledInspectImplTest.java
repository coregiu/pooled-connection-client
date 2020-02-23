/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.monitor;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.ConnectionManagerBean;
import com.xvzhu.connections.apis.protocol.IConnection;
import com.xvzhu.connections.apis.protocol.ISftpConnection;
import com.xvzhu.connections.data.ConnectionBeanBuilder;
import com.xvzhu.connections.mockserver.SftpServer;
import com.xvzhu.connections.sftp.SftpConnectionFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 11:36
 */
public class PooledInspectImplTest {
    private static final Logger LOG = LoggerFactory.getLogger(PooledInspectImplTest.class);

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
    @SuppressWarnings("unchecked")
    public void should_print_logs_when_manager_is_correct() throws Exception {

        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().port(port).build().getConnectionBean();
        PooledInspectImpl pooledInspect = new PooledInspectImpl();

        GenericObjectPool<IConnection> connectionPool;
        SftpConnectionFactory sftpConnectionFactory = SftpConnectionFactory.builder().connectionBean(connectionBean).build();
        connectionPool = new GenericObjectPool(sftpConnectionFactory, new GenericObjectPoolConfig(), new AbandonedConfig());
        connectionPool.borrowObject();
        connectionPool.borrowObject();
        connectionPool.borrowObject();

        ConnectionManagerBean managerBean = ConnectionManagerBean.builder().connectionPool(connectionPool).build();
        Map<Thread, ConnectionManagerBean> managerBeanMap = new HashMap<>();
        managerBeanMap.put(Thread.currentThread(), managerBean);

        Method statisticMethod = pooledInspect.getClass().getDeclaredMethod("getStatistic", Map.class);


        Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections = new HashMap<>();
        connections.put(connectionBean, managerBeanMap);

        statisticMethod.setAccessible(true);
        int result = (int)statisticMethod.invoke(pooledInspect, managerBeanMap);
        pooledInspect.inspect(connections);
        pooledInspect.inspect(connectionBean, connections);
        assertThat(result, is(3));
    }
}
