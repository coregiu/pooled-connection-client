/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.operation;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

/**
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-23 10:07
 */
public class ProtocolDefineTest {

    public static final String COM_XVZHU_CONNECTIONS_APIS_PROTOCOL_SFTP_CONNECTION = "com.xvzhu.connections.apis.protocol.ISftpConnection";
    public static final String COM_XVZHU_CONNECTIONS_APIS_PROTOCOL_SHELL_CONNECTION = "com.xvzhu.connections.apis.protocol.IShellConnection";

    @Test
    public void should_return_sftp_when_input_sftp_connection_class() {
        Optional<ProtocolDefine> protocolDefine = ProtocolDefine.parseType(COM_XVZHU_CONNECTIONS_APIS_PROTOCOL_SFTP_CONNECTION);
        assertFalse(!protocolDefine.isPresent());
        assertThat(protocolDefine.get().getConnectionType(), is(COM_XVZHU_CONNECTIONS_APIS_PROTOCOL_SFTP_CONNECTION));
        assertThat(protocolDefine.get().getConnectionFactory(), is("com.xvzhu.connections.sftp.SftpConnectionFactory"));
        assertThat(protocolDefine.get().getConnectionImpl(), is("com.xvzhu.connections.sftp.SftpImpl"));
    }

    @Test
    public void should_return_shell_when_input_shell_connection_class() {
        Optional<ProtocolDefine> protocolDefine = ProtocolDefine.parseType(COM_XVZHU_CONNECTIONS_APIS_PROTOCOL_SHELL_CONNECTION);
        assertFalse(!protocolDefine.isPresent());
        assertThat(protocolDefine.get().getConnectionType(), is(COM_XVZHU_CONNECTIONS_APIS_PROTOCOL_SHELL_CONNECTION));
        assertThat(protocolDefine.get().getConnectionFactory(), is("com.xvzhu.connections.shell.ShellConnectionFactory"));
        assertThat(protocolDefine.get().getConnectionImpl(), is("com.xvzhu.connections.shell.ShellImpl"));
    }

    @Test
    public void should_return_enpty_when_input_http_class() {
        Optional<ProtocolDefine> protocolDefine = ProtocolDefine.parseType("com.xvzhu.connections.apis.protocol.IHttpConnection");
        assertTrue(!protocolDefine.isPresent());
    }
}