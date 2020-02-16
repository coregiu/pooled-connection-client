package com.xvzhu.connections.monitor;

import com.xvzhu.connections.BasicSftpClientConnectionManager;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionManagerConfig;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IObserver;
import org.junit.Test;

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
    public void should_print_logs_when_manager_is_correct() {
        IObserver logObserver = new LogObserver();
        IConnectionManager connectionManager = BasicSftpClientConnectionManager.builder().setConnectionManagerConfig(ConnectionManagerConfig.builder().build()).build();
        logObserver.visit(connectionManager, new ConnectionBean("127.0.0.1", 22, "huawei", "huawei"));
        assertThat(logObserver != null, is(true));
    }
}
