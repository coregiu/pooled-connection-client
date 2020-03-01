/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.apis;

import com.xvzhu.connections.apis.protocol.ISftpConnection;
import com.xvzhu.connections.sftp.SftpImpl;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-22 10:35
 */
public class ConnectionManagerBeanTest {
    @Test
    public void should_has_default_lock_when_init() {
        assertNotNull(ConnectionManagerBean.builder().build().getLock());
    }

    @Test
    public void should_borrow_time_great_than_0_when_init() {
        assertTrue(ConnectionManagerBean.builder().build().getBorrowTime() > 0);
    }

    @Test
    public void should_default_true_borrow_value_when_init() {
        assertTrue(ConnectionManagerBean.builder().build().isConnectionBorrowed());
    }

    @Test
    public void should_equal_when_manager_bean_has_same_value() {
        Object object = new Object();
        ISftpConnection sftpConnection = new SftpImpl();
        ConnectionManagerBean connectionManagerBean = ConnectionManagerBean.builder().build();
        connectionManagerBean.setLock(object);
        connectionManagerBean.setConnectionClient(sftpConnection);
        connectionManagerBean.setConnectionPool(null);


        ConnectionManagerBean connectionManagerBean1 = ConnectionManagerBean.builder().lock(object).connectionClient(sftpConnection).build();
        assertNotNull(connectionManagerBean.toString());
        assertTrue(connectionManagerBean.canEqual(connectionManagerBean1));
        assertTrue(connectionManagerBean.equals(connectionManagerBean1));
    }
}