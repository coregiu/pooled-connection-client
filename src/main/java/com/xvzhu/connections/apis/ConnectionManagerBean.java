package com.xvzhu.connections.apis;

import com.xvzhu.connections.apis.protocol.ISftpConnection;
import lombok.Builder;
import lombok.Data;
import java.util.Calendar;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-22 10:28
 */
@Data
@Builder
public class ConnectionManagerBean {
    @Builder.Default
    private Object lock = new Object();
    private ISftpConnection sftpConnection;
    @Builder.Default
    private long borrowTime = Calendar.getInstance().getTimeInMillis();
    private long releaseTime;
    @Builder.Default
    private boolean isConnectionBorrowed = true;
}
