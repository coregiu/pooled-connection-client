package com.xvzhu.connections.monitor;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.IInspect;
import com.xvzhu.connections.apis.ConnectionManagerBean;
import lombok.NonNull;

import java.util.Map;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 11:30
 */
public class PooledInspectImpl implements IInspect {
    @Override
    public void inspect(@NonNull ConnectionBean connectionBean, @NonNull Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections) {

    }

    @Override
    public void inspect(@NonNull Map<ConnectionBean, Map<Thread, ConnectionManagerBean>> connections) {

    }
}
