/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.monitor;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.IInspect;
import com.xvzhu.connections.apis.ConnectionManagerBean;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 11:30
 */
public class PooledInspectImpl implements IInspect {
    private static final Logger LOG = LoggerFactory.getLogger(PooledInspectImpl.class);

    @Override
    public void inspect(@NonNull ConnectionBean connectionBean, @NonNull Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections) {
        Map<Thread, ConnectionManagerBean> managerBeanMap = connections.get(connectionBean);
        if (managerBeanMap == null) {
            LOG.warn("No connection for host:{}", connectionBean.getHost());
            return;
        }
        LOG.error("Host: {} Port：{} -- Total connections: {}", connectionBean.getHost(), connectionBean.getPort(), getStatistic(managerBeanMap));
    }

    @Override
    public void inspect(@NonNull Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections) {
         connections.forEach((key, value) -> LOG.error("Host: {} Port：{} -- Total connections: {}", key.getHost(), key.getPort(), getStatistic(value)));
    }

    private int getStatistic(Map<Thread, ConnectionManagerBean> connectionManagerBeanMap) {
        if (null == connectionManagerBeanMap) {
            return 0;
        }
        AtomicInteger sum = new AtomicInteger();
        connectionManagerBeanMap.forEach((key, value) -> sum.addAndGet(value.getConnectionPool().getNumActive() + value.getConnectionPool().getNumIdle()));
        return sum.get();
    }
}
