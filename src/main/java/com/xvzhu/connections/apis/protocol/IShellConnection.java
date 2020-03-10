/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.apis.protocol;

import com.jcraft.jsch.ChannelShell;

/**
 * The interface Shell connection.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-03-10 23:19
 */
public interface IShellConnection extends IConnection {
    /**
     * Gets channel shell.
     *
     * @return the channel shell
     */
    ChannelShell getChannelShell();
}
