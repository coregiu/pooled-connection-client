/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.monitor;

import com.xvzhu.connections.apis.ConnectionConst;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IConnectionMonitor;
import com.xvzhu.connections.apis.IObserver;
import com.xvzhu.connections.apis.ConnectionManagerBean;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <p>Connection Monitor.</p>
 * Contains two type to inspect the manager.<br>
 * One is triggered by get, release or close connection. Inspect the connection.<br>
 * Another is scheduled by special thread. The field of intervalTimeSecond is the schedule time.<Br>
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 14:22
 */
public class ConnectionMonitor implements IConnectionMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionMonitor.class);
    private static final int DEFAULT_OBSERVERS = 2;
    private static final long DEFAULT_INTERVAL_TIME_SECOND = 60L;
    private IObserver inspectObserver = new InspectObserver();
    private Future<?> scheduleFuture;

    private static class ConnectionMonitorHolder {
        private static final ConnectionMonitor INSTANCE = new ConnectionMonitor();
    }

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        LOG.info("Begin to inspect the mangers by schedule thread.");
        Thread scheduledThread = new Thread(r);
        scheduledThread.setName(ConnectionConst.SCHEDULE_THREAD_NAME);
        return scheduledThread;
    });

    /**
     * The Observers.
     */
    private List<IObserver> observers = new ArrayList<>(DEFAULT_OBSERVERS);

    private long intervalTimeSecond = DEFAULT_INTERVAL_TIME_SECOND;

    private ConnectionMonitor() {
        observers.add(new LogObserver());
        observers.add(inspectObserver);
        executor.scheduleAtFixedRate(inspectObserver, 0, intervalTimeSecond, TimeUnit.MILLISECONDS);
    }

    /**
     * <p>Get schedule interval time (second).</p>
     *
     * @return the interval time second
     */
    @Override
    public long getIntervalTimeSecond() {
        return intervalTimeSecond;
    }

    /**
     * <p>Set schedule interval time (second).</p>
     *
     * @param intervalTimeSecond the interval time second
     */
    @Override
    public void setIntervalTimeSecond(long intervalTimeSecond) {
        this.intervalTimeSecond = intervalTimeSecond;
    }

    @Override
    public void setAutoInspect(boolean isAutoInspect) {
        if (isAutoInspect) {
            scheduleFuture.cancel(true);
            scheduleFuture = executor.scheduleAtFixedRate(inspectObserver, 0, intervalTimeSecond, TimeUnit.MILLISECONDS);
        } else {
            scheduleFuture.cancel(true);
        }
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
    public void attach(@NonNull IObserver observer) {
        observers.add(observer);
    }


    /**
     * Notify observers.
     *
     * @param connectionManager the connection manager
     * @param connectionBean    the connection bean
     */
    public void notifyObservers(@NonNull IConnectionManager connectionManager,
                                @NonNull ConnectionBean connectionBean,
                                @NonNull Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections) {
        LOG.info("Begin to inspect the connection by notify.");
        if (connections.isEmpty()) {
            LOG.warn("Not connections to handle.");
            return;
        }
        for (IObserver observer : observers) {
            observer.visit(connectionManager, connectionBean, connections);
        }
    }
}