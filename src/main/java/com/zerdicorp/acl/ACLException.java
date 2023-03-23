package com.zerdicorp.acl;

import com.intellij.openapi.actionSystem.AnAction;

import java.util.ArrayList;
import java.util.List;

public class ACLException extends RuntimeException {
    public String text;
    public List<AnAction> actions = new ArrayList<>();

    public ACLException(String text) {
        this.text = text;
    }

    public ACLException(String text, List<AnAction> actions) {
        this.text = text;
        this.actions = actions;
    }

}
