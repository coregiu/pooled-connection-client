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
    private static final int DEFAULT_REUSE_TIME_OUT_MS = 3600000;
    private static final int DEFAULT_CLOSE_TIME_OUT_MS = 300000;
    private static final long DEFAULT_SCHEDULE_INTERVAL_TIME_MS = 600000L;
    private static final int DEFAULT_MAX_CONNECTION_SIZE = 8;

    /**
     * The max size of connections all of current process(ClassLoader).
     */
    @Builder.Default
    private int maxConnectionSize = DEFAULT_MAX_CONNECTION_SIZE;
    /**
     * Connection borrow timeout configuration(Millisecond).
     * Default is 1 hour(3600000 ms).
     * If time out, release the connection to reuse.
     */
    @Builder.Default
    private int borrowTimeoutMS = DEFAULT_REUSE_TIME_OUT_MS;
    /**
     * Connection reuse timeout configuration(Millisecond).
     * Default is 5 minutes(300000 ms).
     * If time out, close the connection.
     */
    @Builder.Default
    private int idleTimeoutSecond = DEFAULT_CLOSE_TIME_OUT_MS;
    /**
     * Connect timeout configuration for jsch(Millisecond).
     * Default is 5000 million seconds.
     * If time out, close the connection.
     */
    @Builder.Default
    private int connectionTimeoutMs = ConnectionConst.DEFAULT_CONNECT_TIME_OUT_MS;

    /**
     * The interval of schedule(Millisecond).
     * Default is 10 minutes.(600000 ms)
     */
    @Builder.Default
    private long intervalTimeMS = DEFAULT_SCHEDULE_INTERVAL_TIME_MS;
}
