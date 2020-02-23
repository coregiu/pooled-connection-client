/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.apis;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Bean test.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 17:03
 */
public class ConnectionBeanTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void should_return_true_when_input_same_connection_info() {
        ConnectionBean beanFst = new ConnectionBean("192.168.0.1", 80, "huawei", "huawei");
        ConnectionBean beanSnd = new ConnectionBean("192.168.0.1", 80, "huawei", "huawei");
        assertThat(beanFst.hashCode() == beanSnd.hashCode(), is(true));
    }

    @Test
    public void should_return_false_when_input_not_same_connection_info() {
        ConnectionBean beanFst = new ConnectionBean("192.168.0.1", 80, "huawei", "huawei");
        ConnectionBean beanSnd = new ConnectionBean("192.168.0.2", 80, "huawei", "huawei");
        assertThat(beanFst.hashCode() == beanSnd.hashCode(), is(false));
    }

    @Test
    public void should_get_same_object_when_input_same_host() {
        Map<ConnectionBean, String> map = new HashMap<>();
        map.put(new ConnectionBean("192.168.0.1", 80, "huawei", "huawei"), "huawei");
        assertThat(map.get(new ConnectionBean("192.168.0.1", 80, "huawei", "huawei")), is("huawei"));
    }

    @Test
    public void should_throw_null_point_exception_when_input_null_username() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("username is marked @NonNull but is null");
        new ConnectionBean("192.168.0.1", 80, null, "huawei");
    }

    @Test
    public void should_throw_null_point_exception_when_input_null_host() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("host is marked @NonNull but is null");
        new ConnectionBean(null, 80, "huawei", "huawei");
    }

    @Test
    public void should_throw_null_point_exception_when_input_null_password() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("password is marked @NonNull but is null");
        new ConnectionBean("192.168.0.1", 80, "huawei", null);
    }
}
