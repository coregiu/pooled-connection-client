package com.xvzhu.connections.apis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * The configuration of Connections.
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 17:39
 */
@Builder
@Data
@EqualsAndHashCode()
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionManagerConfig {
    private static final int DEFAULT_REUSE_TIME_OUT_SECOND = 3600;
    private static final int DEFAULT_CLOSE_TIME_OUT_SECOND = 300;
    private static final long DEFAULT_SCHEDULE_INTERVAL_TIME_SECOND = 60L;
    private static final int DEFAULT_MAX_CONNECTION_SIZE = 8;

    /**
     * The max size of connections all of current process(ClassLoader).
     */
    @Builder.Default
    private int maxConnectionSize = DEFAULT_MAX_CONNECTION_SIZE;
    /**
     * Connection using timeout configuration(Second).
     * Default is 1 hour(3600 s).
     * If time out, release the connection to reuse.
     */
    @Builder.Default
    private int reuseTimeoutSecond = DEFAULT_REUSE_TIME_OUT_SECOND;
    /**
     * Connection reuse timeout configuration(Second).
     * Default is 5 minutes(300 second).
     * If time out, close the connection.
     */
    @Builder.Default
    private int closeTimeoutSecond = DEFAULT_CLOSE_TIME_OUT_SECOND;
    /**
     * Connect timeout configuration for jsch(Million Second).
     * Default is 5000 million seconds.
     * If time out, close the connection.
     */
    @Builder.Default
    private int connectionTimeoutMs = ConnectionConst.DEFAULT_CONNECT_TIME_OUT_MS;
    /**
     * The interval of schedule(Second).
     */
    @Builder.Default
    private long intervalTimeSecond = DEFAULT_SCHEDULE_INTERVAL_TIME_SECOND;
}
