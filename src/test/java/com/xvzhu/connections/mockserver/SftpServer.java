package com.xvzhu.connections.mockserver;

import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;
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
    private final SshServer sshd = SshServer.setUpDefaultServer();

    /**
     * Sets sftp server.
     *
     * @throws Exception the exception
     */
    public void setupSftpServer(){
        sshd.setPort(2222);
        sshd.setHost("127.0.0.1");
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshd.setCommandFactory(new ScpCommandFactory());
        List<NamedFactory<Command>> namedFactoryList = new ArrayList<>();
        namedFactoryList.add(new SftpSubsystemFactory());
        sshd.setSubsystemFactories(namedFactoryList);
        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(String username, String password, ServerSession serverSession) throws PasswordChangeRequiredException {
                return username.equals("huawei") && password.equals("huawei");
            }
        });
        sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
            @Override
            public boolean authenticate(String s, PublicKey publicKey, ServerSession serverSession) {
                return false;
            }
        });
        try {
            sshd.start();
            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            LOG.error("Failed to start sftp server", e);
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

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {
        new SftpServer().setupSftpServer();
    }
}