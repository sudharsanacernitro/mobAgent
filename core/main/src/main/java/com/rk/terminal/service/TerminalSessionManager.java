package com.rk.terminal.service;

import com.example.myapplication.MainActivity;
import com.example.myapplication.utils.exceptions.NotFoundException;
import com.rk.terminal.ui.screens.terminal.MkSession;
import com.rk.terminal.utils.SessionCodes;
import com.termux.terminal.TerminalAsynchronousSessionHandler;
import com.termux.terminal.TerminalSessionHandler;

import java.util.HashMap;

public class TerminalSessionManager {

    private  HashMap<String,TerminalSessionHandler> sessionMapper = new HashMap();
    private static TerminalSessionManager instance = null;

    private MainActivity activity;

    private TerminalSessionManager( MainActivity activity ) {

        this.activity = activity;

        initSessions();

    }

    public void initSessions(){
        addession(SessionCodes.ssh);
        addession(SessionCodes.llamaCppServer);
        addession(SessionCodes.tools);
        addession(SessionCodes.tempAsync);
        addession(SessionCodes.tempSync);
        addession(SessionCodes.userTerminalSession);
    }



    public static synchronized TerminalSessionManager getInstance(MainActivity activity) {

        if( instance == null ) {

            instance = new TerminalSessionManager( activity );

        }

        return instance;

    }

    public static TerminalSessionManager getInstance() {

        return instance;

    }

    public void addession( int sessionCode ) {

        TerminalSessionHandler session;

        switch ( sessionCode ) {

            case SessionCodes.ssh:
                session = MkSession.INSTANCE.createSession( activity, "1", 0 , 1);
                session.initializeEmulator();

                sessionMapper.put("ssh",session);

                break;

            case SessionCodes.llamaCppServer:
                session = MkSession.INSTANCE.createSession(activity,"2",0,1);
                session.initializeEmulator();

                sessionMapper.put("llama_cpp_server",session);

                break;

            case SessionCodes.tools:
                session = MkSession.INSTANCE.createSession(activity,"3",0,0);
                session.initializeEmulator();

                sessionMapper.put("tools",session);
                break;

            case SessionCodes.tempAsync:
                session = MkSession.INSTANCE.createSession(activity,"4",0,1);
                session.initializeEmulator();

                sessionMapper.put("temp_async",session);
                break;

            case SessionCodes.tempSync:
                session = MkSession.INSTANCE.createSession(activity,"5",0,0);
                session.initializeEmulator();

                sessionMapper.put("temp_sync",session);
                break;

            case SessionCodes.userTerminalSession:
                session = MkSession.INSTANCE.createSession(activity,"6",0,1);
                session.initializeEmulator();

                sessionMapper.put("user_terminal_session",session);
                break;

             default:
                 throw new IllegalArgumentException("Invalid session code");


        }



    }



    public TerminalSessionHandler getSession( String sessionType ) throws NotFoundException {

        if( sessionMapper.containsKey( sessionType) ) {

            return sessionMapper.get( sessionType );

        }else {

            throw new NotFoundException("Requested session not found");

        }

    }
}
