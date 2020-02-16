package com.xvzhu.connections.apis;

/**
 * Observer Interface.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 14:22
 */
public interface IObserver {
    /**
     * <p>Observer model, the interface of observer.</p>
     *
     * @param connectionManager the connection manager
     * @param connectionBean    the connection bean
     */
    void visit(IConnectionManager connectionManager, ConnectionBean connectionBean);
}
