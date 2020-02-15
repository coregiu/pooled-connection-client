package com.xvzhu.connections.monitor;

import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IConnectionMonitor;
import com.xvzhu.connections.apis.IObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Connection Monitor.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 14:22
 */
public class ConnectionMonitor implements IConnectionMonitor {
    private static final int DEFAULT_OBSERVERS = 2;
    private static class ConnectionMonitorHolder {
        private static final ConnectionMonitor INSTANCE = new ConnectionMonitor();
    }

    /**
     * The Observers.
     */
    private List<IObserver> observers = new ArrayList<>(DEFAULT_OBSERVERS);

    private ConnectionMonitor() {

    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static ConnectionMonitor getInstance() {
        return ConnectionMonitorHolder.INSTANCE;
    }

    /**
     * Attach.
     *
     * @param observer the observer
     */
    public void attach(IObserver observer) {
        observers.add(observer);
    }


    /**
     * Notify observers.
     *
     * @param connectionManager the connection manager
     */
    public void notifyObservers(IConnectionManager connectionManager) {
        for (IObserver observer : observers) {
            observer.visit(connectionManager);
        }
    }
}