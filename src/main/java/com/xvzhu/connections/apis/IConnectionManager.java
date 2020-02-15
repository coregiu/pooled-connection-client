package com.xvzhu.connections.apis;

/**
 * Manager API.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 14:22
 */
public interface IConnectionManager {
    /**
     * <p>Borrow connection connection.</p>
     * If there is no connection, create a new connection, and return.<br>
     * If connection was used, or pool has no connection, throw ConnectionException.<br>
     *
     * @param connectionBean the connection bean
     * @return the connection
     * @throws ConnectionException the connection exception
     */
    IConnection borrowConnection(ConnectionBean connectionBean) throws ConnectionException;

    /**
     * <p>Release connection.</p>
     * Used for Basic Connection API.<br>
     * Release connection to manager, don't close.<br>
     * If a pooled manager, do not need to invoke this API, you can reuse directly.<br>
     *
     * @param connectionBean the connection bean
     * @throws ConnectionException the connection exception
     */
    void releaseConnection(ConnectionBean connectionBean) throws ConnectionException;

    /**
     * <p>Close connection.</p>
     * Close connection or connection pool.<Br>
     *
     * @param connectionBean the connection bean
     * @throws ConnectionException the connection exception
     */
    void closeConnection(ConnectionBean connectionBean) throws ConnectionException;

    /**
     * <p>Accept the visitor of monitor module.</p>
     *
     * @param observer the observer
     */
    void accept(IObserver observer);

    /**
     * <p>Attach the user's observer to monitor's observer list.</p>
     *
     * @param observer the observer
     */
    void attach(IObserver observer);

    /**
     * <p>Inspect the connection or pool, close the connection，
     * if connection timed out or grow too fast.</p>
     */
    void inspect();
}
