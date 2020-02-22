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
public class ConnectBeanBuilder {
    public static final int PORT = 2222;
    public static final String HOST = "127.0.0.1";
    public static final String USERNAME = "huawei";
    public static final String PASSWORD = "huawei";

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
