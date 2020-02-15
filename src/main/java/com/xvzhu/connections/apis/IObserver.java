package com.xvzhu.connections.apis;

/**
 * Observer Interface.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 14:22
 */
public interface IObserver {
    void visit(IConnectionManager connectionManager);
}
