package com.xvzhu.connections.apis;

/**
 * Connection Monitor API.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 14:22
 */
public interface IConnectionMonitor {
    void attach(IObserver observer);
    void notifyObservers(IConnectionManager connectionManager);
}
