/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.apis;

/**
 * Connection Exception.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 15:38
 */
public class ConnectionException extends Exception {
    /**
     * Instantiates a new Connection exception.
     *
     * @param message the message
     */
    public ConnectionException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Connection exception.
     *
     * @param messageFormat the message format
     * @param args          the args
     */
    public ConnectionException(String messageFormat, String... args) {
        super(String.format(messageFormat, args));
    }

    /**
     * Instantiates a new Connection exception.
     *
     * @param message the message
     * @param e       the e
     */
    public ConnectionException(String message, Exception e) {
        super(message, e);
    }
}
