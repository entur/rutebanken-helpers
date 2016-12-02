package org.rutebanken.hazelcasthelper.service;

/**
 *
 */
public class RutebankenHazelcastException extends RuntimeException {

    public RutebankenHazelcastException(String str, Exception e) {
        super(str,e);
    }
}
