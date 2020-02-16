package com.xvzhu.connections.monitor;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IInspect;
import com.xvzhu.connections.apis.IObserver;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 11:31
 */
public class BasicInspectObserver implements IObserver, IInspect {
    @Override
    public void inspect(IConnectionManager connectionManager, ConnectionBean connectionBean) {

    }

    @Override
    public void visit(IConnectionManager connectionManager, ConnectionBean connectionBean) {

    }

    @Override
    public void run() {

    }
}
