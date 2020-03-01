/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.apis.protocol;

import com.xvzhu.connections.apis.ConnectionException;

import java.io.InputStream;
import java.util.List;

/**
 * Sftp Connection API.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 15:34
 */
public interface ISftpConnection extends IConnection{
    /**
     * Current directory string.
     *
     * @return the string
     * @throws ConnectionException the connection exception
     */
    String currentDirectory() throws ConnectionException;

    /**
     * List list.
     *
     * @param path the path
     * @return the list
     */
    List<String> list(String path);

    /**
     * Rename.
     *
     * @param oldName the old name
     * @param newName the new name
     * @throws ConnectionException the connection exception
     */
    void rename(String oldName, String newName) throws ConnectionException;

    /**
     * Download byte [ ].
     *
     * @param dir  the dir
     * @param name the name
     * @return the input stream
     * @throws ConnectionException the connection exception
     */
    InputStream download(String dir, String name) throws ConnectionException;

    /**
     * Upload.
     *
     * @param dir  the dir
     * @param name the name
     * @param in   the in
     * @throws ConnectionException the connection exception
     */
    void upload(String dir, String name, InputStream in) throws ConnectionException;

    /**
     * Delete directory.
     *
     * @param dir the dir
     * @throws ConnectionException the connection exception
     */
    void deleteDirectory(String dir) throws ConnectionException;

    /**
     * Delete file.
     *
     * @param dir  the dir
     * @param name the name
     * @throws ConnectionException the connection exception
     */
    void deleteFile(String dir, String name) throws ConnectionException;

    /**
     * Mkdirs.
     *
     * @param dir the dir
     * @throws ConnectionException the connection exception
     */
    void mkdirs(String dir) throws ConnectionException;

    /**
     * Is exist boolean.
     *
     * @param path the path
     * @return the boolean
     */
    boolean isExist(String path);

    /**
     * Is directory boolean.
     *
     * @param path the path
     * @return the boolean
     */
    boolean isDirectory(String path);

    /**
     * Is file boolean.
     *
     * @param path the path
     * @return the boolean
     */
    boolean isFile(String path);
}
