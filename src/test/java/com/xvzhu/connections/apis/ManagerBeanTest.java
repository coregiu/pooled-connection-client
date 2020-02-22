package com.xvzhu.connections.apis;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-22 10:35
 */
public class ManagerBeanTest {
    @Test
    public void should_has_default_lock_when_init() {
        assertNotNull(ManagerBean.builder().build().getLock());
    }

    @Test
    public void should_borrow_time_great_than_0_when_init() {
        assertTrue(ManagerBean.builder().build().getBorrowTime() > 0);
    }

    @Test
    public void should_default_true_borrow_value_when_init() {
        assertNotNull(ManagerBean.builder().build().isConnectionBorrowed());
    }
}