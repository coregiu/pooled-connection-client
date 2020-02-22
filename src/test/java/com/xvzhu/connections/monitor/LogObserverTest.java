package com.xvzhu.connections.monitor;

import com.xvzhu.connections.BasicSftpClientConnectionManager;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionManagerConfig;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IObserver;
import com.xvzhu.connections.apis.ManagerBean;
import com.xvzhu.connections.data.ConnectionBeanBuilder;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Log print observer test.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 0:40
 */
public class LogObserverTest {
    @Test
    public void should_print_logs_when_manager_is_correct() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        LogObserver logObserver = new LogObserver();
        IConnectionManager connectionManager = BasicSftpClientConnectionManager.builder().build();
        Method statisticMethod = logObserver.getClass().getDeclaredMethod("getStatisticInfo", IConnectionManager.class, Map.class);
        Method visitMethod = logObserver.getClass().getDeclaredMethod("visit", IConnectionManager.class, ConnectionBean.class, Map.class);

        ConnectionBean connectionBean = ConnectionBeanBuilder.builder().build().getConnectionBean();
        Map<Thread, ManagerBean> managerMap = new HashMap<>();
        long timeNow = Calendar.getInstance().getTimeInMillis();
        ManagerBean managerBean = ManagerBean.builder().borrowTime(timeNow - 100000).build();
        managerMap.put(Thread.currentThread(), managerBean);

        Map<ConnectionBean, Map<Thread, ManagerBean>> connections = new HashMap<>();
        connections.put(connectionBean, managerMap);

        statisticMethod.setAccessible(true);
        Map<String, Object> result = (Map<String, Object>)statisticMethod.invoke(logObserver, connectionManager, connections);
        visitMethod.invoke(logObserver, connectionManager, connectionBean, connections);
        assertThat(result.size(), is(2));
    }
}
