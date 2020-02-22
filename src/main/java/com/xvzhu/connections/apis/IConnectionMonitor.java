package com.xvzhu.connections.apis;

import lombok.NonNull;

import java.util.Map;

/**
 * Connection Monitor API.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 14:22
 */
public interface IConnectionMonitor {
    /**
     * <p>Subject the notify event.</p>
     *
     * @param observer the observer
     */
    void attach(@NonNull IObserver observer);

    /**
     * Notify observers.
     *
     * @param connectionManager the connection manager
     * @param connectionBean    the connection bean
     * @param connections       the connections
     */
    void notifyObservers(@NonNull IConnectionManager connectionManager,
                         @NonNull ConnectionBean connectionBean,
                         @NonNull Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections);

    /**
     * <p>Get schedule interval time (second).</p>
     *
     * @return the interval time second
     */
    long getIntervalTimeSecond();

    /**
     * <p>Set schedule interval time (second).</p>
     *
     * @param intervalTimeSecond the interval time second
     */
    void setIntervalTimeSecond(long intervalTimeSecond);

    /**
     * Sets auto inspect.
     *
     * @param isAutoInspect is auto inspect
     */
    void setAutoInspect(boolean isAutoInspect);
}
