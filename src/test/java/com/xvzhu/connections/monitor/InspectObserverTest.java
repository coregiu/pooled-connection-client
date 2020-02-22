package com.xvzhu.connections.monitor;

import com.xvzhu.connections.BasicSftpClientConnectionManager;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionManagerConfig;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IObserver;
import com.xvzhu.connections.apis.ManagerBean;
import com.xvzhu.connections.data.ConnectionBeanBuilder;
import org.junit.Test;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 12:44
 */
public class InspectObserverTest {
    private ConnectionManagerConfig connectionManagerConfig = ConnectionManagerConfig.builder().build();

    private IObserver observer = new InspectObserver();

    @Test
    public void should_release_connection_when_borrow_timed_out() {
        IConnectionManager connectionManager = BasicSftpClientConnectionManager.builder().build();
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ManagerBean managerBean = ManagerBean.builder().borrowTime(timeNow - connectionManagerConfig.getBorrowTimeoutMS() - 100000).build();
        managerMap.put(Thread.currentThread(), managerBean);

        Map<ConnectionBean, Map<Thread, ManagerBean>> connections = new HashMap<>();
        connections.put(connectionBean, managerMap);

        observer.visit(connectionManager, connectionBean, connections);
        assertFalse(managerBean.isConnectionBorrowed());
    }

    @Test
    public void should_schedule_reset_connection_container_when_basic_connection_is_closed() {
        IConnectionManager connectionManager = BasicSftpClientConnectionManager.builder().build();
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ManagerBean managerBean = ManagerBean.builder().borrowTime(timeNow - connectionManagerConfig.getBorrowTimeoutMS() + 10000).build();
        managerMap.put(Thread.currentThread(), managerBean);

        Map<ConnectionBean, Map<Thread, ManagerBean>> connections = new HashMap<>();
        connections.put(connectionBean, managerMap);

        observer.visit(connectionManager, connectionBean, connections);
        assertTrue(managerBean.isConnectionBorrowed());
    }
}