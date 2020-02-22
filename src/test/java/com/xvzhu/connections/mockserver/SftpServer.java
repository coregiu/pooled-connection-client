package com.xvzhu.connections.mockserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.xvzhu.connections.apis.ConnectionException;
import com.xvzhu.connections.data.ConnectionBeanBuilder;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.session.SessionFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The sftp real mock server.
 *
 * @author : xvzhu
 * @version V1.0
 * @since Date : 2020-02-16 1:35
 */
public class SftpServer {
    private static final Logger LOG = LoggerFactory.getLogger(SftpServer.class);
    private static final int MIN_PORT = 5000;
    private static final int MAX_PORT = 5000;
    private static final int DEFAULT_RETRY_TIMES = 10;
    private static final BiMap<Integer, String> PORT_MAP = Maps.synchronizedBiMap(HashBiMap.create());

    private static AtomicInteger startCount = new AtomicInteger();
    private final SshServer sshd = SshServer.setUpDefaultServer();


    /**
     * Sets sftp server.
     *
     * @param uuid           the uuid
     * @param countDownLatch the count down latch
     * @throws Exception the exception
     */
    public void setupSftpServer(String uuid, CountDownLatch countDownLatch) {
        Thread thread = new Thread(() -> {
            int retryTimes = 0;
            while (retryTimes < DEFAULT_RETRY_TIMES) {
                int port = getPort();
                try {
                    setupSftpServer(port);
                    PORT_MAP.put(port, uuid);
                    break;
                } catch (ConnectionException e) {
                    LOG.error("Failed to startup sftp server, uuid:{}, port:{}, retrytimes:{}", uuid, port, retryTimes);
                }
            }
            if (PORT_MAP.inverse().get(uuid) != null) {
                countDownLatch.countDown();
            }
        });
        thread.setName("Start sftp server " + uuid);
        thread.start();
    }

    public String getUuid() {
        return UUID.randomUUID().toString();
    }

    public int getPort(String uuid) {
        return PORT_MAP.inverse().get(uuid);
    }

    private void setupSftpServer(int port) throws ConnectionException {
        startCount.getAndAdd(1);
        sshd.setPort(port);
        sshd.setHost(ConnectionBeanBuilder.HOST);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshd.setCommandFactory(new ScpCommandFactory());
        List<NamedFactory<Command>> namedFactoryList = new ArrayList<>();
        namedFactoryList.add(new SftpSubsystemFactory());
        sshd.setSubsystemFactories(namedFactoryList);
        sshd.setPasswordAuthenticator((username, password, serverSession) -> username.equals(ConnectionBeanBuilder.USERNAME) && password.equals(ConnectionBeanBuilder.PASSWORD));
        sshd.setPublickeyAuthenticator((s, publicKey, serverSession) -> false);
        sshd.setSessionFactory(new SessionFactory(sshd));
        sshd.setScheduledExecutorService(Executors.newSingleThreadScheduledExecutor());
        try {
            sshd.start();
        } catch (Exception e) {
            LOG.error("Failed to startup the sftp server, the start id is {}.", startCount.get(), e);
            throw new ConnectionException("Failed to startup the sftp server.");
        }
    }

    /**
     * Shutdown.
     */
    public void shutdown() {
        try {
            sshd.close();
        } catch (IOException e) {
            LOG.error("Failed to shutdown sftp server", e);
        }
    }

    private int getPort() {
        for (int i = MIN_PORT; i < MAX_PORT; i++) {
            if (PORT_MAP.get(i) != null) {
                continue;
            }
            LOG.error("Assign port : {}", i);
            return i;
        }
        return ConnectionBeanBuilder.PORT;
    }
}