/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.monitor;

import com.xvzhu.connections.BasicClientConnectionManager;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.IInspect;
import com.xvzhu.connections.apis.ConnectionManagerBean;
import lombok.NonNull;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


/**
 * The type Basic inspect.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 11:31
 */
public class BasicInspectImpl implements IInspect {

    private Consumer<Map.Entry<ConnectionBean, Map<Thread, ConnectionManagerBean>>> releaseConsumer;
    private BiConsumer<ConnectionBean, Map<Thread, ConnectionManagerBean>> releaseBiConsumer;

    /**
     * <p>Close timed out and closed connection, clear the manager bean in thread local.<br>
     * If connections number exceed the max of connection limit, close unborrowed connections.</p>
     *
     * @param connectionBean the connection bean
     */
    @Override
    public void inspect(@NonNull ConnectionBean connectionBean, @NonNull Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections) {

        if (releaseBiConsumer == null) {
            releaseBiConsumer =  BasicClientConnectionManager.getReleaseBiConsumer();
        }
        releaseBiConsumer.accept(connectionBean, connections.get(connectionBean));
    }

    /**
     * <p>Close all timed out and closed connection of connection manager, clear the manager bean in thread local.</p>
     */
    @Override
    public void inspect(@NonNull Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections) {
        if (releaseConsumer == null) {
            releaseConsumer = BasicClientConnectionManager.getReleaseConsumer();
        }
        connections.entrySet().stream().forEach(releaseConsumer);
    }
}
