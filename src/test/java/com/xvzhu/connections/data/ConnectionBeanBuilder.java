/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.data;

import com.xvzhu.connections.apis.ConnectionBean;
import lombok.Builder;
import lombok.NonNull;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-22 9:13
 */
@Builder
public class ConnectionBeanBuilder {
    public static final int PORT = 2222;
    public static final String HOST = "127.0.0.1";
    public static final String USERNAME = "test";
    public static final String PASSWORD = "test";

    @NonNull
    @Builder.Default
    private String host = HOST;

    @Builder.Default
    private int port = PORT;

    @NonNull
    @Builder.Default
    private String username = USERNAME;

    @NonNull
    @Builder.Default
    private String password = PASSWORD;

    public ConnectionBean getConnectionBean(){
        return new ConnectionBean(host, port, username, password);
    }
}
