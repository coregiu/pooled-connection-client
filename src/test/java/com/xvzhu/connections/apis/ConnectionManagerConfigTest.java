/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.apis;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-22 12:05
 */
public class ConnectionManagerConfigTest {

    @Test
    public void should_init_default_max_connection_size_value_when_default_created() {
        assertThat(ConnectionManagerConfig.builder().build().getMaxConnectionSize(), is(8));
    }

    @Test
    public void should_init_default_borrow_time_value_when_default_created() {
        assertThat(ConnectionManagerConfig.builder().build().getBorrowTimeoutMS(), is(3600000));
    }

    @Test
    public void should_init_default_idle_time_value_when_default_created() {
        assertThat(ConnectionManagerConfig.builder().build().getIdleTimeoutMS(), is(300000));
    }

    @Test
    public void should_init_default_connection_timeout_value_when_default_created() {
        assertThat(ConnectionManagerConfig.builder().build().getConnectionTimeoutMs(), is(5000));
    }

    @Test
    public void should_init_default_schedule_period_value_when_default_created() {
        assertThat(ConnectionManagerConfig.builder().build().getSchedulePeriodTimeMS(), is(600000L));
    }

    @Test
    public void should_init_default_auto_inspect_value_when_default_created() {
        assertTrue(ConnectionManagerConfig.builder().build().isAutoInspect());
    }

    @Test
    public void should_init_default_borrow_wait_time_value_when_default_created() {
        assertThat(ConnectionManagerConfig.builder().build().getBorrowMaxWaitTimeMS(), is(60000L));
    }
}