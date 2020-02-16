package com.xvzhu.connections.monitor;

import com.xvzhu.connections.BasicSftpClientConnectionManager;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.IInspect;
import lombok.NonNull;
import java.util.Map;


/**
 * The type Basic inspect.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 11:31
 */
public class BasicInspectImpl implements IInspect {
    /**
     * <p>Close timed out and closed connection, clear the manager bean in thread local.<br>
     * If connections number exceed the max of connection limit, close unborrowed connections.</p>
     *
     * @param connectionBean the connection bean
     */
    @Override
    public void inspect(@NonNull ConnectionBean connectionBean) {

    }

    /**
     * <p>Close all timed out and closed connection of connection manager, clear the manager bean in thread local.</p>
     */
    @Override
    public void inspect() {
        Map<ConnectionBean, ThreadLocal<BasicSftpClientConnectionManager.ManagerBean>> connections
                = BasicSftpClientConnectionManager.getConnections();
        connections.entrySet().stream().forEach(entry -> closeConnection(entry.getKey(), entry.getValue()));
    }

    private void closeConnection(ConnectionBean connectionBean,
                                 ThreadLocal<BasicSftpClientConnectionManager.ManagerBean> managerBeanThreadLocal) {

    }
}
