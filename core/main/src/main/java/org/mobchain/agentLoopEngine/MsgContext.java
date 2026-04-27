package org.mobchain.agentLoopEngine;

import java.util.HashMap;
import java.util.Map;

public class MsgContext {

    private  Map<String, Object> data ;

    public MsgContext( ) {

        data = new HashMap<>();

    }

    public MsgContext( Map<String , Object> data ) {

        this.data = data;

    }





    public void put(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) data.get(key);
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    public MsgContext clone( ) {

        return new MsgContext( new HashMap<>(this.data) );

    }

    public boolean remove( String key ) {

        try{

            data.remove( key );
            return true;

        } catch( Exception e ) {

            return false;

        }


    }






}
