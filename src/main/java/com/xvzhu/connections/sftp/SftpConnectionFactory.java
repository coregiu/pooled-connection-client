package com.xvzhu.connections.sftp;

import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.protocol.ISftpConnection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * The type Sftp connection factory.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-15 16:13
 */
@Builder
@Data
@AllArgsConstructor
public class SftpConnectionFactory extends BasePooledObjectFactory<ISftpConnection> {
    private static final Logger LOG = LoggerFactory.getLogger(SftpConnectionFactory.class);
    private static final int DEFAULT_TIME_OUT_MILLI = 15000;

    private ConnectionBean connectionBean;
    @Builder.Default
    private int timeoutMilliSecond = DEFAULT_TIME_OUT_MILLI;


    /**
     * <p>Create sftp connection.</p>
     *
     * @return the sftp connection
     * @throws ConnectionException the connection exception
     */
    @Override
    public ISftpConnection create() throws ConnectionException {
        ISftpConnection sftpConnection = new SftpImpl();
        sftpConnection.connect(connectionBean, timeoutMilliSecond);
        return sftpConnection;
    }

    /**
     * <p>Wrap pooled object.</p>
     *
     * @param connection the sftp
     * @return the pooled object
     */
    @Override
    public PooledObject<ISftpConnection> wrap(ISftpConnection connection) {
        return new DefaultPooledObject<>(connection);
    }

    /**
     * <p>Close the connection.</p>
     *
     * @param connectionPool the connection pool.
     */
    @Override
    public void destroyObject(PooledObject<ISftpConnection> connectionPool) {
        if (connectionPool == null) {
            LOG.warn("The connection pool is null");
            return;
        }
        ISftpConnection sftp = connectionPool.getObject();
        if (sftp == null) {
            LOG.warn("The sftp is null");
            return;
        }
        try {
            sftp.disconnect();
        } catch (ConnectionException e) {
            LOG.error("Failed to disconnect the connection.", e);
        }
    }

    /**
     * <p>Validate the connection is connected.</p>
     *
     * @param connectionPool the the connection pool.
     * @return the boolean
     */
    @Override
    public boolean validateObject(PooledObject<ISftpConnection> connectionPool) {
        if (connectionPool == null) {
            LOG.warn("The connection pool is null");
            return false;
        }
        ISftpConnection sftp = connectionPool.getObject();
        return sftp != null && sftp.isValid();
    }
}