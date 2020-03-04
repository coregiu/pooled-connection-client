/*
 * Copyright (c)  Xvzhu 2020.  All rights reserved.
 */

package com.xvzhu.connections.operation;

import com.xvzhu.connections.apis.ConnectionConst;
import com.xvzhu.connections.apis.ConnectionBean;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.apis.ConnectionManagerConfig;
import com.xvzhu.connections.apis.IOperation;
import com.xvzhu.connections.apis.protocol.IConnection;
import com.xvzhu.connections.apis.ConnectionManagerBean;
import lombok.NonNull;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * <p>The Operation factory.</p>
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 16:01
 */
public class OperationFactory implements IOperation {
    private static final Logger LOG = LoggerFactory.getLogger(OperationFactory.class);

    private static final Object LOCK = new Object();

    private ConnectionManagerConfig config;

    /**
     * Instantiates a new Operation factory.
     *
     * @param connectionManagerConfig the connection manager config
     */
    public OperationFactory(@NonNull ConnectionManagerConfig connectionManagerConfig) {
        this.config = connectionManagerConfig;
    }

    /**
     * Create connection t.
     *
     * @param <T>                     the type parameter
     * @param connectionBean          the connection bean
     * @param connectionManagerConfig the connection manager config
     * @param clazz                   the clazz
     * @return the t
     * @throws ConnectionException the connection exception
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends IConnection> T createConnection(ConnectionBean connectionBean,
                                                      ConnectionManagerConfig connectionManagerConfig,
                                                      Class<T> clazz) throws ConnectionException {
        Optional<ProtocolDefine> protocolDefine = ProtocolDefine.parseType(clazz.getName());
        if (!protocolDefine.isPresent()) {
            LOG.error("The protocol {} is not support now!", clazz.getName());
            throw new ConnectionException(String.format(Locale.ENGLISH, "The protocol %s is not support now!", clazz.getName()));
        }

        try {
            Class connectionClientClass = Class.forName(protocolDefine.get().getConnectionImpl());
            IConnection connectionClient = (IConnection) connectionClientClass.getConstructor().newInstance();
            connectionClient.connect(connectionBean, connectionManagerConfig.getConnectionTimeoutMs());
            return (T) connectionClient;
        } catch (InstantiationException
                | IllegalAccessException
                | InvocationTargetException
                | NoSuchMethodException
                | ClassNotFoundException e) {
            LOG.error("Failed to init the protocol class {}", protocolDefine.get().getConnectionImpl(), e);
            throw new ConnectionException("Failed to init the protocol class.");
        }
    }

    /**
     * Create connection factory t.
     *
     * @param <T>                     the type parameter
     * @param connectionBean          the connection bean
     * @param connectionManagerConfig the connection manager config
     * @param clazz                   the clazz
     * @return the t
     * @throws ConnectionException the connection exception
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BasePooledObjectFactory> T createConnectionFactory(ConnectionBean connectionBean,
                                                                         ConnectionManagerConfig connectionManagerConfig,
                                                                         Class<T> clazz) throws ConnectionException {
        Optional<ProtocolDefine> protocolDefine = ProtocolDefine.parseType(clazz.getName());
        if (!protocolDefine.isPresent()) {
            LOG.error("The protocol {} is not support now!", clazz.getName());
            throw new ConnectionException(String.format(Locale.ENGLISH, "The protocol %s is not support now!", clazz.getName()));
        }

        try {
            Class connectionFactoryClass = Class.forName(protocolDefine.get().getConnectionFactory());
            return (T)connectionFactoryClass.getConstructor(ConnectionBean.class, int.class).newInstance(connectionBean, connectionManagerConfig.getConnectionTimeoutMs());
        } catch (InstantiationException
                | IllegalAccessException
                | InvocationTargetException
                | NoSuchMethodException
                | ClassNotFoundException e) {
            LOG.error("Failed to init the protocol factory class {}", protocolDefine.get().getConnectionImpl(), e);
            throw new ConnectionException("Failed to init the protocol factory class.");
        }
    }

    /**
     * Gets release consumer.
     *
     * @return the release consumer
     */
    @Override
    public Consumer<Map.Entry<ConnectionBean, Map<Thread, ConnectionManagerBean>>> getReleaseConsumer() {
        return entry -> releaseConnection(entry.getKey(), entry.getValue());
    }

    /**
     * Gets release consumer.
     *
     * @return the release biConsumer
     */
    @Override
    public BiConsumer<ConnectionBean, Map<Thread, ConnectionManagerBean>> getReleaseBiConsumer() {
        return this::releaseConnection;
    }

    /**
     * Release to manager.
     *
     * @param managerBean the manager bean
     */
    @Override
    public void setConnection2Idle(@NonNull ConnectionManagerBean managerBean) {
        managerBean.setReleaseTime(Calendar.getInstance().getTimeInMillis());
        managerBean.setConnectionBorrowed(false);
        LOG.info("The connection of {} was set to idle", managerBean.hashCode());
    }

    /**
     * Close a connection.
     *
     * @param thread            the thread
     * @param hostConnectionMap the host connection map
     */
    @Override
    public void shutdownConnection(@NonNull Thread thread,
                                   @NonNull Map<Thread, ConnectionManagerBean> hostConnectionMap) {
        ConnectionManagerBean managerBean = hostConnectionMap.get(thread);
        // double check.
        if (managerBean == null) {
            LOG.info("Then thread {} 's connection has been closed!", thread);
            return;
        }
        IConnection connection = managerBean.getConnectionClient();
        if (null != connection) {
            try {
                connection.disconnect();
            } catch (ConnectionException e) {
                LOG.error("Failed to disconnect", e);
            }
        }
        LOG.info("The connection of thread {} was removed", thread.getName());
        hostConnectionMap.remove(thread);
    }

    private void releaseConnection(ConnectionBean connectionBean,
                                   Map<Thread, ConnectionManagerBean> hostConnectionMap) {
        if (Thread.currentThread().getName().equals(ConnectionConst.SCHEDULE_THREAD_NAME)) {
            synchronized (LOCK) {
                batchReleaseAction(connectionBean, hostConnectionMap);
            }
        } else {
            if (hostConnectionMap == null || hostConnectionMap.get(Thread.currentThread()) == null) {
                return;
            }
            ConnectionManagerBean managerBean = hostConnectionMap.get(Thread.currentThread());
            synchronized (managerBean.getLock()) {
                singleReleaseAction(hostConnectionMap);
            }
        }
    }

    private void batchReleaseAction(ConnectionBean connectionBean,
                                    Map<Thread, ConnectionManagerBean> hostConnectionMap) {
        LOG.info("begin to release all the connections of host: {}", connectionBean.getHost());
        long timeNow = Calendar.getInstance().getTimeInMillis();
        // 1. Release timed out and closed connections. Contains reuse and close time out.
        Set<Map.Entry<Thread, ConnectionManagerBean>> releaseSet
                = new HashSet<>(hostConnectionMap.size());
        hostConnectionMap.entrySet().stream()
                .filter(entry -> entry.getValue().isConnectionBorrowed() &&
                        isTimedOut(timeNow, entry.getValue().getBorrowTime(), config.getBorrowTimeoutMS()))
                .forEach(releaseSet::add);
        LOG.info("release size = {}", releaseSet.size());
        releaseSet.forEach(entry -> setConnection2Idle(entry.getValue()));

        Set<Map.Entry<Thread, ConnectionManagerBean>> closeSet
                = new HashSet<>(hostConnectionMap.size());
        hostConnectionMap.entrySet().stream()
                .filter(entry -> (!entry.getValue().isConnectionBorrowed()) &&
                        isTimedOut(timeNow, entry.getValue().getReleaseTime(), config.getIdleTimeoutMS()))
                .forEach(closeSet::add);
        LOG.info("shutdown size = {}", closeSet.size());
        closeSet.forEach(entry -> shutdownConnection(entry.getKey(), hostConnectionMap));

        // 2. Release unused connection if connections is exceed the max size.
        int hostConnectionRemainSize = hostConnectionMap.size() - config.getMaxConnectionSize();
        if (hostConnectionRemainSize <= 0) {
            return;
        }
        Set<Map.Entry<Thread, ConnectionManagerBean>> clearSet
                = new HashSet<>(hostConnectionMap.size());
        hostConnectionMap.entrySet().stream()
                .filter(entry -> (!entry.getValue().isConnectionBorrowed()))
                .forEach(clearSet::add);
        LOG.info("shutdown by connection size check, size = {}", clearSet.size());
        clearSet.forEach(entry -> shutdownConnection(entry.getKey(), hostConnectionMap));
    }

    private boolean isTimedOut(long timeNow, long checkTime, int timeout) {
        return timeNow - checkTime > timeout;
    }

    private void singleReleaseAction(Map<Thread, ConnectionManagerBean> hostConnectionMap) {
        Thread releaseThread = Thread.currentThread();
        ConnectionManagerBean managerBean = hostConnectionMap.get(Thread.currentThread());
        long timeNow = Calendar.getInstance().getTimeInMillis();

        if (managerBean.isConnectionBorrowed() && isTimedOut(timeNow,
                managerBean.getBorrowTime(), config.getBorrowTimeoutMS())) {
            setConnection2Idle(managerBean);
        } else if (!managerBean.isConnectionBorrowed() && isTimedOut(timeNow,
                managerBean.getReleaseTime(), config.getIdleTimeoutMS())) {
            shutdownConnection(releaseThread, hostConnectionMap);
        } else {
            LOG.info("Do nothing");
        }
    }
}