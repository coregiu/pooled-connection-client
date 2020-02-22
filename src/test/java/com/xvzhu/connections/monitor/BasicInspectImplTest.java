package com.xvzhu.connections.monitor;

import com.xvzhu.connections.BasicSftpClientConnectionManager;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionManagerConfig;
import com.xvzhu.connections.apis.IInspect;
import com.xvzhu.connections.apis.ISftpConnection;
import com.xvzhu.connections.apis.ManagerBean;
import com.xvzhu.connections.data.ConnectionBeanBuilder;
import mockit.Capturing;
import org.junit.Test;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 11:35
 */
public class BasicInspectImplTest {
    private IInspect inspect = new BasicInspectImpl();

    private ConnectionManagerConfig connectionManagerConfig = ConnectionManagerConfig.builder().build();

    @Capturing
    private ISftpConnection sftpConnection;

    @Test
    public void should_release_connection_to_manager_when_borrow_timed_out() {
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ManagerBean managerBean = ManagerBean.builder().borrowTime(timeNow - connectionManagerConfig.getBorrowTimeoutMS() - 100000).build();
        managerMap.put(Thread.currentThread(), managerBean);

        Map<ConnectionBean, Map<Thread, ManagerBean>> connections = new HashMap<>();
        connections.put(connectionBean, managerMap);

        inspect.inspect(connectionBean, connections);
        assertFalse(managerBean.isConnectionBorrowed());
    }

    @Test
    public void should_not_release_connection_to_manager_when_borrow_time_not_out() {
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ManagerBean managerBean = ManagerBean.builder().borrowTime(timeNow - connectionManagerConfig.getBorrowTimeoutMS() + 1000).build();
        managerMap.put(Thread.currentThread(), managerBean);

        Map<ConnectionBean, Map<Thread, ManagerBean>> connections = new HashMap<>();
        connections.put(connectionBean, managerMap);

        inspect.inspect(connectionBean, connections);
        assertTrue(managerBean.isConnectionBorrowed());
    }

    @Test
    public void should_shutdown_connection_to_manager_when_idle_timed_out() {
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ManagerBean managerBean = ManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutSecond() - 10000)
                .sftpConnection(sftpConnection)
                .build();
        managerMap.put(Thread.currentThread(), managerBean);

        Map<ConnectionBean, Map<Thread, ManagerBean>> connections = new HashMap<>();
        connections.put(connectionBean, managerMap);

        inspect.inspect(connectionBean, connections);
        assertNull(managerMap.get(Thread.currentThread()));
    }

    @Test
    public void should_not_shutdown_connection_to_manager_when_idle_time_not_out() {
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ManagerBean managerBean = ManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutSecond() + 1000)
                .build();
        managerMap.put(Thread.currentThread(), managerBean);

        Map<ConnectionBean, Map<Thread, ManagerBean>> connections = new HashMap<>();
        connections.put(connectionBean, managerMap);

        inspect.inspect(connectionBean, connections);
        assertNotNull(managerMap.get(Thread.currentThread()));
    }

    @Test
    public void should_release_all_connection_to_manager_when_borrow_timed_out() {
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().host("192.168.0.1").build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ManagerBean managerBean = ManagerBean.builder().borrowTime(timeNow - connectionManagerConfig.getBorrowTimeoutMS() - 100000).build();
        managerMap.put(Thread.currentThread(), managerBean);

        ConnectionBean connectionBean2 = ConnectionBeanBuilder.builder().host("192.168.0.2").build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap2 = new HashMap<>();
        ManagerBean managerBean2 = ManagerBean.builder().borrowTime(timeNow - connectionManagerConfig.getBorrowTimeoutMS() - 100000).build();
        managerMap2.put(Thread.currentThread(), managerBean2);

        ConnectionBean connectionBean3 = ConnectionBeanBuilder.builder().host("192.168.0.3").build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap3 = new HashMap<>();
        ManagerBean managerBean3 = ManagerBean.builder().borrowTime(timeNow - connectionManagerConfig.getBorrowTimeoutMS() + 1000).build();
        managerMap3.put(Thread.currentThread(), managerBean3);

        Map<ConnectionBean, Map<Thread, ManagerBean>> connections = new HashMap<>();
        connections.put(connectionBean, managerMap);
        connections.put(connectionBean2, managerMap2);
        connections.put(connectionBean3, managerMap3);

        inspect.inspect(connections);
        assertFalse(managerBean.isConnectionBorrowed());
        assertFalse(managerBean2.isConnectionBorrowed());
        assertTrue(managerBean3.isConnectionBorrowed());
    }

    @Test
    public void should_shutdown_all_connection_to_manager_when_idle_timed_out() {
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().host("192.168.0.1").build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ManagerBean managerBean = ManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutSecond() - 10000)
                .sftpConnection(sftpConnection)
                .build();
        managerMap.put(Thread.currentThread(), managerBean);

        ConnectionBean connectionBean2 = ConnectionBeanBuilder.builder().host("192.168.0.2").build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap2 = new HashMap<>();
        ManagerBean managerBean2 = ManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutSecond() - 10000)
                .sftpConnection(sftpConnection)
                .build();
        managerMap2.put(Thread.currentThread(), managerBean2);

        ConnectionBean connectionBean3 = ConnectionBeanBuilder.builder().host("192.168.0.3").build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap3 = new HashMap<>();
        ManagerBean managerBean3 = ManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutSecond() + 1000)
                .sftpConnection(sftpConnection)
                .build();
        managerMap3.put(Thread.currentThread(), managerBean3);

        Map<ConnectionBean, Map<Thread, ManagerBean>> connections = new HashMap<>();
        connections.put(connectionBean, managerMap);
        connections.put(connectionBean2, managerMap2);
        connections.put(connectionBean3, managerMap3);

        inspect.inspect(connections);
        assertNull(managerMap.get(Thread.currentThread()));
        assertNull(managerMap2.get(Thread.currentThread()));
        assertNotNull(managerMap3.get(Thread.currentThread()));
    }

    @Test
    public void should_shutdown_all_connection_to_manager_when_connections_more_than_limited_size() {
        BasicSftpClientConnectionManager.builder().setMaxConnectionSize(3);
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().host("192.168.0.1").build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ManagerBean managerBean = ManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutSecond() - 10000)
                .sftpConnection(sftpConnection)
                .build();
        managerMap.put(Thread.currentThread(), managerBean);

        ConnectionBean connectionBean2 = ConnectionBeanBuilder.builder().host("192.168.0.2").build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap2 = new HashMap<>();
        ManagerBean managerBean2 = ManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutSecond() - 10000)
                .sftpConnection(sftpConnection)
                .build();
        managerMap2.put(Thread.currentThread(), managerBean2);

        ConnectionBean connectionBean3 = ConnectionBeanBuilder.builder().host("192.168.0.3").build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap3 = new HashMap<>();
        ManagerBean managerBean3 = ManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutSecond() + 1000)
                .sftpConnection(sftpConnection)
                .build();
        managerMap3.put(Thread.currentThread(), managerBean3);

        ConnectionBean connectionBean4 = ConnectionBeanBuilder.builder().host("192.168.0.4").build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap4 = new HashMap<>();
        ManagerBean managerBean4 = ManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutSecond() + 1000)
                .sftpConnection(sftpConnection)
                .build();
        managerMap4.put(Thread.currentThread(), managerBean4);

        Map<ConnectionBean, Map<Thread, ManagerBean>> connections = new HashMap<>();
        connections.put(connectionBean, managerMap);
        connections.put(connectionBean2, managerMap2);
        connections.put(connectionBean3, managerMap3);
        connections.put(connectionBean4, managerMap4);

        inspect.inspect(connections);
        assertNull(managerMap.get(Thread.currentThread()));
        assertNull(managerMap2.get(Thread.currentThread()));
        assertNotNull(managerMap3.get(Thread.currentThread()));
        assertNotNull(managerMap4.get(Thread.currentThread()));
    }
}