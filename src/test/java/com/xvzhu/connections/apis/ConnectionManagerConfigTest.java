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

    @Test
    public void should_equal_when_config_bean_has_same_value() {
        ConnectionManagerConfig connectionManagerConfig = new ConnectionManagerConfig();
        ConnectionManagerConfig connectionManagerConfig1 = ConnectionManagerConfig.builder().build();
        assertTrue(connectionManagerConfig.canEqual(connectionManagerConfig1));
        assertNotNull(connectionManagerConfig.toString());
        assertEquals(connectionManagerConfig.hashCode(), connectionManagerConfig1.hashCode());
        assertEquals(connectionManagerConfig, connectionManagerConfig1);
    }

    @Test
    public void should_using_new_value_when_config_value_changed() {
        ConnectionManagerConfig connectionManagerConfig = ConnectionManagerConfig.builder()
                .maxConnectionSize(10)
                .borrowMaxWaitTimeMS(10000)
                .borrowTimeoutMS(5000)
                .connectionTimeoutMs(3000)
                .idleTimeoutMS(1800000)
                .isAutoInspect(false)
                .schedulePeriodTimeMS(12000L)
                .build();
        assertNotNull(connectionManagerConfig.toString());
        assertThat(connectionManagerConfig.isAutoInspect(), is(false));
        assertThat(connectionManagerConfig.getSchedulePeriodTimeMS(), is(12000L));
        assertThat(connectionManagerConfig.getConnectionTimeoutMs(), is(3000));
        assertThat(connectionManagerConfig.getBorrowTimeoutMS(), is(5000));
        assertThat(connectionManagerConfig.getMaxConnectionSize(), is(10));
        assertThat(connectionManagerConfig.getIdleTimeoutMS(), is(1800000));
        assertThat(connectionManagerConfig.getBorrowMaxWaitTimeMS(), is(10000L));
    }
}