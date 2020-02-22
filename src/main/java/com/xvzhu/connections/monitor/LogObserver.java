package com.xvzhu.connections.monitor;

import com.xvzhu.connections.BasicSftpClientConnectionManager;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.IConnectionManager;
import com.xvzhu.connections.apis.IObserver;
import com.xvzhu.connections.apis.ManagerBean;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

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
    public void visit(@NonNull IConnectionManager connectionManager,
                      @NonNull ConnectionBean connectionBean,
                      @NonNull Map<ConnectionBean, Map<Thread, ManagerBean>> connections) {
        LOG.warn("Begin to inspect the connection manager: {}, {}", connectionBean.getHost(), Thread.currentThread());
        getStatisticInfo(connectionManager, connections).forEach((key, value) -> LOG.warn("{} : {}", key, value));
    }

    private Map<String, Object> getStatisticInfo(IConnectionManager connectionManager,
                                                 Map<ConnectionBean, Map<Thread, ManagerBean>> connections) {
        Map<String, Object> statisticMap = new HashMap<>();
        if (connectionManager instanceof BasicSftpClientConnectionManager) {
            statisticMap.put("Total of host connections", connections.size());
            connections.forEach((key, value) ->
                    statisticMap.put("Total connection of the host-" + key.getHost(), value.size()));
        }
        return statisticMap;
    }

    @Override
    public void run() {
        LOG.warn("Begin to inspect the connection manager");
    }
}
