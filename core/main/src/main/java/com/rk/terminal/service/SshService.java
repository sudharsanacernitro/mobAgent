package com.rk.terminal.service;

import com.termux.terminal.TerminalAsynchronousSessionHandler;

public class SshService {

    public static void startSSHServer(TerminalAsynchronousSessionHandler session) {

        stopSSHServer(session);

        System.out.println("SSH Session started");

        byte[] cmd = "bash sshServerSetup.sh\n".getBytes();
        session.write(cmd , cmd.length);

    }

    public static void stopSSHServer( TerminalAsynchronousSessionHandler session ) {

        System.out.println("stoping SSH session");

        byte[] cmd = "pkill sshd\n".getBytes();

        session.write( cmd , cmd.length );

    }
}
