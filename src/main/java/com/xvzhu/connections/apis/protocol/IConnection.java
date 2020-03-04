/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.apis.protocol;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;

/**
 * Connection API.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 15:33
 */
public interface IConnection {
    /**
     * Connect.
     *
     * @param connectionBean     the connection bean
     * @param timeoutMilliSecond the timeout milli second
     * @throws ConnectionException the connection exception
     */
    void connect(ConnectionBean connectionBean, int timeoutMilliSecond) throws ConnectionException;

    /**
     * Disconnect.
     *
     * @throws ConnectionException the connection exception
     */
    void disconnect() throws ConnectionException;

    /**
     * Is connection valid.
     *
     * @return the boolean
     */
    boolean isValid();

    /**
     * Was connection closed.
     *
     * @return the boolean
     */
    boolean isClosed();
}