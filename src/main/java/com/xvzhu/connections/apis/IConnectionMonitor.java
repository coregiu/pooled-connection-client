package com.xvzhu.connections.apis;

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
    void attach(IObserver observer);

    /**
     * Notify observers.
     *
     * @param connectionManager the connection manager
     * @param connectionBean    the connection bean
     */
    void notifyObservers(IConnectionManager connectionManager, ConnectionBean connectionBean);

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
}
