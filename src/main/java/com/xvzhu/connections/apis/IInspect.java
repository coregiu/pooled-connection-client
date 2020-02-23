/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.apis;

import lombok.NonNull;

import java.util.Map;

/**
 * The interface Inspect.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 11:22
 */
public interface IInspect {
    /**
     * <p>Inspect the connection, close the connection if it's timed out or closed.</p>
     *
     * @param connectionBean the connection bean
     * @param connections    the connections
     */
    void inspect(@NonNull ConnectionBean connectionBean, @NonNull Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections);

    /**
     * <p>Inspect the manager, close the connection if it's timed out or closed.</p>
     *
     * @param connections the connections
     */
    void inspect(@NonNull Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections);
}
