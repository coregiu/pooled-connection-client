package com.xvzhu.connections;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.IConnection;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IObserver;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 14:00
 */
public class PooledSftpClientConnectionManager<T extends IConnection> implements IConnectionManager {
    @Override
    public IConnection borrowConnection(ConnectionBean connectionBean) throws ConnectionException {
        return null;
    }

    @Override
    public void releaseConnection(ConnectionBean connectionBean) throws ConnectionException {

    }

    @Override
    public void closeConnection(ConnectionBean connectionBean) throws ConnectionException {

    }

    @Override
    public void accept(IObserver observer, ConnectionBean connectionBean) {

    }

    @Override
    public void attach(IObserver observer) {

    }
}
