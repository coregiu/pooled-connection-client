/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.apis;

import com.xvzhu.connections.apis.protocol.IConnection;
import lombok.NonNull;
import org.apache.commons.pool2.BasePooledObjectFactory;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * <p>The operation API</p>
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-03-04 18:38
 */
public interface IOperation {
    /**
     * Create connection t.
     *
     * @param <T>                     the type parameter
     * @param connectionBean          the connection bean
     * @param connectionManagerConfig the connection manager config
     * @param clazz                   the clazz
     * @return the t
     * @throws ConnectionException the connection exception
     */
    <T extends IConnection> T createConnection(ConnectionBean connectionBean,
                                               ConnectionManagerConfig connectionManagerConfig,
                                               Class<T> clazz) throws ConnectionException;

    /**
     * Create connection factory t.
     *
     * @param <T>                     the type parameter
     * @param connectionBean          the connection bean
     * @param connectionManagerConfig the connection manager config
     * @param clazz                   the clazz
     * @return the t
     * @throws ConnectionException the connection exception
     */
    <T extends BasePooledObjectFactory> T createConnectionFactory(ConnectionBean connectionBean,
                                                                  ConnectionManagerConfig connectionManagerConfig,
                                                                  Class<T> clazz) throws ConnectionException;

    /**
     * Gets release consumer.
     *
     * @return the release consumer
     */
    Consumer<Map.Entry<ConnectionBean, Map<Thread, ConnectionManagerBean>>> getReleaseConsumer();

    /**
     * Gets release bi consumer.
     *
     * @return the release bi consumer
     */
    BiConsumer<ConnectionBean, Map<Thread, ConnectionManagerBean>> getReleaseBiConsumer();

    /**
     * Sets connection 2 idle.
     *
     * @param managerBean the manager bean
     */
    void setConnection2Idle(@NonNull ConnectionManagerBean managerBean);

    /**
     * Shutdown connection.
     *
     * @param thread            the thread
     * @param hostConnectionMap the host connection map
     */
    void shutdownConnection(@NonNull Thread thread,
                            @NonNull Map<Thread, ConnectionManagerBean> hostConnectionMap);
}
