/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.monitor;

import com.xvzhu.connections.BasicClientConnectionManager;
import com.xvzhu.connections.PooledClientConnectionManager;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IInspect;
import com.xvzhu.connections.apis.IObserver;
import com.xvzhu.connections.apis.ConnectionManagerBean;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 12:01
 */
public class InspectObserver implements IObserver, Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(InspectObserver.class);
    private IInspect basicInspect = new BasicInspectImpl();
    private IInspect pooledInspect = new PooledInspectImpl();
    private Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections;

    @Override
    public void visit(@NonNull IConnectionManager connectionManager,
                      @NonNull  ConnectionBean connectionBean,
                      @NonNull Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections) {
        this.connections = connections;
        if (connectionManager instanceof BasicClientConnectionManager) {
            basicInspect.inspect(connectionBean, connections);
        } else if (connectionManager instanceof PooledClientConnectionManager){
            pooledInspect.inspect(connectionBean, connections);
        }
    }

    @Override
    public void run() {
        try {
            if (null == connections) {
                LOG.warn("There is no connections.");
                return;
            }
            basicInspect.inspect(connections);
            basicInspect.inspect(connections);
        } catch (Exception e) {
            LOG.error("Failed to inspect the managers!", e);
        }
    }
}
