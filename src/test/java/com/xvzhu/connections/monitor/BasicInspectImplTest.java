package com.xvzhu.connections.monitor;

import com.xvzhu.connections.BasicClientConnectionManager;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionManagerConfig;
import com.xvzhu.connections.apis.IInspect;
import com.xvzhu.connections.apis.protocol.ISftpConnection;
import com.xvzhu.connections.apis.ConnectionManagerBean;
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
        Map<Thread, ConnectionManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ConnectionManagerBean managerBean = ConnectionManagerBean.builder().borrowTime(timeNow - connectionManagerConfig.getBorrowTimeoutMS() - 100000).build();
        managerMap.put(Thread.currentThread(), managerBean);

        Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections = new HashMap<>();
        connections.put(connectionBean, managerMap);

        inspect.inspect(connectionBean, connections);
        assertFalse(managerBean.isConnectionBorrowed());
    }

    @Test
    public void should_not_release_connection_to_manager_when_borrow_time_not_out() {
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().build().getConnectionBean();
        Map<Thread, ConnectionManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ConnectionManagerBean managerBean = ConnectionManagerBean.builder().borrowTime(timeNow).build();
        managerMap.put(Thread.currentThread(), managerBean);

        Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections = new HashMap<>();
        connections.put(connectionBean, managerMap);

        inspect.inspect(connectionBean, connections);
        assertTrue(managerBean.isConnectionBorrowed());
    }

    @Test
    public void should_shutdown_connection_to_manager_when_idle_timed_out() {
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().build().getConnectionBean();
        Map<Thread, ConnectionManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ConnectionManagerBean managerBean = ConnectionManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutMS() - 10000)
                .sftpConnection(sftpConnection)
                .build();
        managerMap.put(Thread.currentThread(), managerBean);

        Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections = new HashMap<>();
        connections.put(connectionBean, managerMap);

        inspect.inspect(connectionBean, connections);
        assertNull(managerMap.get(Thread.currentThread()));
    }

    @Test
    public void should_not_shutdown_connection_to_manager_when_idle_time_not_out() {
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().build().getConnectionBean();
        Map<Thread, ConnectionManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ConnectionManagerBean managerBean = ConnectionManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutMS() + 1000)
                .build();
        managerMap.put(Thread.currentThread(), managerBean);

        Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections = new HashMap<>();
        connections.put(connectionBean, managerMap);

        inspect.inspect(connectionBean, connections);
        assertNotNull(managerMap.get(Thread.currentThread()));
    }

    @Test
    public void should_release_all_connection_to_manager_when_borrow_timed_out() {
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().host("192.168.0.1").build().getConnectionBean();
        Map<Thread, ConnectionManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ConnectionManagerBean managerBean = ConnectionManagerBean.builder().borrowTime(timeNow - connectionManagerConfig.getBorrowTimeoutMS() - 100000).build();
        managerMap.put(Thread.currentThread(), managerBean);

        ConnectionBean connectionBean2 = ConnectionBeanBuilder.builder().host("192.168.0.2").build().getConnectionBean();
        Map<Thread, ConnectionManagerBean> managerMap2 = new HashMap<>();
        ConnectionManagerBean managerBean2 = ConnectionManagerBean.builder().borrowTime(timeNow - connectionManagerConfig.getBorrowTimeoutMS() - 100000).build();
        managerMap2.put(Thread.currentThread(), managerBean2);

        ConnectionBean connectionBean3 = ConnectionBeanBuilder.builder().host("192.168.0.3").build().getConnectionBean();
        Map<Thread, ConnectionManagerBean> managerMap3 = new HashMap<>();
        ConnectionManagerBean managerBean3 = ConnectionManagerBean.builder().borrowTime(timeNow).build();
        managerMap3.put(Thread.currentThread(), managerBean3);

        Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections = new HashMap<>();
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
        Map<Thread, ConnectionManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ConnectionManagerBean managerBean = ConnectionManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutMS() - 10000)
                .sftpConnection(sftpConnection)
                .build();
        managerMap.put(Thread.currentThread(), managerBean);

        ConnectionBean connectionBean2 = ConnectionBeanBuilder.builder().host("192.168.0.2").build().getConnectionBean();
        Map<Thread, ConnectionManagerBean> managerMap2 = new HashMap<>();
        ConnectionManagerBean managerBean2 = ConnectionManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutMS() - 10000)
                .sftpConnection(sftpConnection)
                .build();
        managerMap2.put(Thread.currentThread(), managerBean2);

        ConnectionBean connectionBean3 = ConnectionBeanBuilder.builder().host("192.168.0.3").build().getConnectionBean();
        Map<Thread, ConnectionManagerBean> managerMap3 = new HashMap<>();
        ConnectionManagerBean managerBean3 = ConnectionManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutMS() + 1000)
                .sftpConnection(sftpConnection)
                .build();
        managerMap3.put(Thread.currentThread(), managerBean3);

        Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections = new HashMap<>();
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
        BasicClientConnectionManager.builder().setMaxConnectionSize(3);
        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().host("192.168.0.1").build().getConnectionBean();
        Map<Thread, ConnectionManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ConnectionManagerBean managerBean = ConnectionManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutMS() - 10000)
                .sftpConnection(sftpConnection)
                .build();
        managerMap.put(Thread.currentThread(), managerBean);

        ConnectionBean connectionBean2 = ConnectionBeanBuilder.builder().host("192.168.0.2").build().getConnectionBean();
        Map<Thread, ConnectionManagerBean> managerMap2 = new HashMap<>();
        ConnectionManagerBean managerBean2 = ConnectionManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutMS() - 10000)
                .sftpConnection(sftpConnection)
                .build();
        managerMap2.put(Thread.currentThread(), managerBean2);

        ConnectionBean connectionBean3 = ConnectionBeanBuilder.builder().host("192.168.0.3").build().getConnectionBean();
        Map<Thread, ConnectionManagerBean> managerMap3 = new HashMap<>();
        ConnectionManagerBean managerBean3 = ConnectionManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutMS() + 1000)
                .sftpConnection(sftpConnection)
                .build();
        managerMap3.put(Thread.currentThread(), managerBean3);

        ConnectionBean connectionBean4 = ConnectionBeanBuilder.builder().host("192.168.0.4").build().getConnectionBean();
        Map<Thread, ConnectionManagerBean> managerMap4 = new HashMap<>();
        ConnectionManagerBean managerBean4 = ConnectionManagerBean.builder()
                .isConnectionBorrowed(false)
                .releaseTime(timeNow - connectionManagerConfig.getIdleTimeoutMS() + 1000)
                .sftpConnection(sftpConnection)
                .build();
        managerMap4.put(Thread.currentThread(), managerBean4);

        Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections = new HashMap<>();
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