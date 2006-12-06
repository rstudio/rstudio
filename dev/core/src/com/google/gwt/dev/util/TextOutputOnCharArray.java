/*
 * Copyright 2006 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.util;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.Arrays;

public class TextOutputOnCharArray implements TextOutput {

    private final CharArrayWriter fCharArrayWriter;
    private final boolean fCompact;
    private int fIndent = 0;
    private int fIndentGranularity;
    private char[][] fIndents = new char[][] {
        new char[0]
    };
    private boolean fJustNewlined;
    private final PrintWriter p;

    public TextOutputOnCharArray(boolean compact) {
        fIndentGranularity = 2;
        fCharArrayWriter = new CharArrayWriter(50 * 1024);
        p = new PrintWriter(fCharArrayWriter);
        fCompact = compact;
    }

    public int getIndent() {
        return fIndent;
    }

    public char[] getText() {
        return fCharArrayWriter.toCharArray();
    }

    public void indentIn() {
        ++fIndent;
        if (fIndent >= fIndents.length) {
            // Cache a new level of indentation string.
            //
            char[] newIndentLevel = new char[fIndent * fIndentGranularity];
            Arrays.fill(newIndentLevel, ' ');
            char[][] newIndents = new char[fIndents.length + 1][];
            System.arraycopy(fIndents, 0, newIndents, 0, fIndents.length);
            newIndents[fIndent] = newIndentLevel;
            fIndents = newIndents;
        }
    }

    public void indentOut() {
        --fIndent;
    }

    private void maybeIndent() {
        if (fJustNewlined && !fCompact) {
            p.print(fIndents[fIndent]);
            // TODO: remove flush calls
            p.flush();
            fJustNewlined = false;
        }
    }

    public void newline() {
        p.print('\n');
        // TODO: remove flush calls
        p.flush();
        fJustNewlined = true;
    }

    public void newlineOpt() {
        if (!fCompact) {
            p.print('\n');
            // TODO: remove flush calls
            p.flush();
        }
        fJustNewlined = true;
    }

    public void print(char c) {
        maybeIndent();
        p.print(c);
        // TODO: remove flush calls
        p.flush();
        fJustNewlined = false;
    }

    public void print(char[] s) {
        maybeIndent();
        p.print(s);
        // TODO: remove flush calls
        p.flush();
        fJustNewlined = false;
    }

    public void print(String s) {
        maybeIndent();
        p.print(s);
        // TODO: remove flush calls
        p.flush();
        fJustNewlined = false;
    }

    public void printOpt(char c) {
        if (!fCompact) {
            maybeIndent();
            p.print(c);
            // TODO: remove flush calls
            p.flush();
        }
    }

    public void printOpt(char[] s) {
        if (!fCompact) {
            maybeIndent();
            p.print(s);
            // TODO: remove flush calls
            p.flush();
        }
    }

    public void printOpt(String s) {
        if (!fCompact) {
            maybeIndent();
            p.print(s);
            // TODO: remove flush calls
            p.flush();
        }
    }

    public void setIndent(int indent) {
        fIndent = indent;
    }
}
