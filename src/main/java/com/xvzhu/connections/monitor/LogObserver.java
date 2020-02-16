package com.xvzhu.connections.monitor;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IObserver;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Log observer.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 14:17
 */
public class LogObserver implements IObserver {
    private static Logger LOG = LoggerFactory.getLogger(LogObserver.class);

    /**
     * <p>The monitor for log print.</p>
     *
     * @param connectionManager the connection manager
     */
    @Override
    public void visit(@NonNull IConnectionManager connectionManager, @NonNull ConnectionBean connectionBean) {
        LOG.warn("Begin to inspect the connection manager: {}, {}", connectionBean.getHost(), Thread.currentThread());
    }

    @Override
    public void run() {
        LOG.warn("Begin to inspect the connection manager");
    }
}
