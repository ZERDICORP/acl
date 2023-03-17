package com.zerdicorp.acl;

public class ACLException extends RuntimeException {
    public String text;

    public ACLException(String text) {
        this.text = text;
    }
}
