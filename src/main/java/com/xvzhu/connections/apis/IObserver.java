package com.xvzhu.connections.apis;

import lombok.NonNull;

import java.util.Map;

/**
 * Observer Interface.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 14:22
 */
public interface IObserver extends Runnable {
    /**
     * <p>Observer model, the interface of observer.</p>
     *
     * @param connectionManager the connection manager
     * @param connectionBean    the connection bean
     * @param connections       the connections
     */
    void visit(@NonNull IConnectionManager connectionManager,
               @NonNull ConnectionBean connectionBean,
               @NonNull Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections);
}
