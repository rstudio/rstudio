// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.util.TextOutput;

public class TextOutputVisitor extends JVisitor implements TextOutput {

    private final TextOutput fTextOutput;

    public TextOutputVisitor(TextOutput textOutput) {
        fTextOutput = textOutput;
    }

    public void indentIn() {
        fTextOutput.indentIn();
    }

    public void indentOut() {
        fTextOutput.indentOut();
    }

    public void newline() {
        fTextOutput.newline();
    }

    public void newlineOpt() {
        fTextOutput.newlineOpt();
    }

    public void print(char c) {
        fTextOutput.print(c);
    }

    public void print(char[] s) {
        fTextOutput.print(s);
    }

    public void print(String s) {
        fTextOutput.print(s);
    }

    public void printOpt(char c) {
        fTextOutput.printOpt(c);
    }

    public void printOpt(char[] s) {
        fTextOutput.printOpt(s);
    }

    public void printOpt(String s) {
        fTextOutput.printOpt(s);
    }
}
