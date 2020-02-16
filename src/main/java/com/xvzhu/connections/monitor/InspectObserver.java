package com.xvzhu.connections.monitor;

import com.xvzhu.connections.BasicSftpClientConnectionManager;
import com.xvzhu.connections.PooledSftpClientConnectionManager;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IInspect;
import com.xvzhu.connections.apis.IObserver;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 12:01
 */
public class InspectObserver implements IObserver, Runnable {
    private static Logger LOG = LoggerFactory.getLogger(InspectObserver.class);
    private IInspect basicInspect = new BasicInspectImpl();
    private IInspect pooledInspect = new PooledInspectImpl();

    @Override
    public void visit(@NonNull IConnectionManager connectionManager, @NonNull  ConnectionBean connectionBean) {
        if (connectionManager instanceof BasicSftpClientConnectionManager) {
            basicInspect.inspect(connectionBean);
        } else if (connectionManager instanceof PooledSftpClientConnectionManager){
            pooledInspect.inspect(connectionBean);
        }
    }

    @Override
    public void run() {
        try {
            basicInspect.inspect();
            basicInspect.inspect();
        } catch (Exception e) {
            LOG.error("Failed to inspect the managers!", e);
        }
    }
}
