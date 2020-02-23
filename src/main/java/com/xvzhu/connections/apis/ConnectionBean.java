/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.apis;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * The connection's input information.
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 16:13
 */
@Data
@EqualsAndHashCode()
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionBean {
    @NonNull
    private String host;

    private int port;

    @NonNull
    private String username;

    @NonNull
    private String password;
}