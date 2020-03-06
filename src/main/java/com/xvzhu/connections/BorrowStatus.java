/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections;

/**
 * The enum Borrow status.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-03-07 0:11
 */
public enum BorrowStatus {
    /**
     * Init borrow status.
     */
    INIT,
    /**
     * No connection borrow status.
     */
    NO_CONNECTION,
    /**
     * Thread has connection borrow status.
     */
    THREAD_HAS_CONNECTION,
    /**
     * Thread has no connection borrow status.
     */
    THREAD_HAS_NO_CONNECTION,
    /**
     * Other thread has connection borrow status.
     */
    OTHER_THREAD_HAS_CONNECTION,
    /**
     * Need new connection borrow status.
     */
    NEED_NEW_CONNECTION,
    /**
     * Over limit borrow status.
     */
    OVER_LIMIT,
    /**
     * Final borrow status.
     */
    FINAL;
}
