/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.logging.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deobfuscates stack traces on the server side. This class requires that you
 * have turned on emulated stack traces and moved your symbolMap files to a
 * place accessible by your server. More concretely, you must compile with the
 * -extra command line option, copy the symbolMaps directory to somewhere your
 * server side code has access to it, and then set the symbolMapsDirectory in
 * this class through the constructor, or the setter method.
 * For example, this variable could be set to "WEB-INF/classes/symbolMaps/"
 * if you copied the symbolMaps directory to there.
 *
 * TODO(unnurg): Combine this code with similar code in JUnitHostImpl
 */
public class StackTraceDeobfuscator {
  
  private static class SymbolMap extends HashMap<String, String> { }
  
  // From JsniRef class, which is in gwt-dev and so can't be accessed here
  // TODO(unnurg) once there is a place for shared code, move this to there.
  private static Pattern JsniRefPattern =
    Pattern.compile("@?([^:]+)::([^(]+)(\\((.*)\\))?");
  
  private String symbolMapsDirectory;
  
  private Map<String, SymbolMap> symbolMaps =
    new HashMap<String, SymbolMap>();
  
  public StackTraceDeobfuscator(String symbolMapsDirectory) {
    this.symbolMapsDirectory = symbolMapsDirectory;
  }
  
  public LogRecord deobfuscateLogRecord(LogRecord lr, String strongName) {
    if (lr.getThrown() != null && strongName != null) {
      lr.setThrown(deobfuscateThrowable(lr.getThrown(), strongName));
    }
    return lr;
  }
  
  public void setSymbolMapsDirectory(String dir) {
    // Switching the directory should clear the symbolMaps variable (which 
    // is read in lazily), but causing the symbolMaps variable to be re-read
    // is somewhat expensive, so we only want to do this if the directory is
    // actually different.
    if (!dir.equals(symbolMapsDirectory)) {
      symbolMapsDirectory = dir;
      symbolMaps = new HashMap<String, SymbolMap>();
    }
  }
  
  private StackTraceElement[] deobfuscateStackTrace(
      StackTraceElement[] st, String strongName) {
    StackTraceElement[] newSt = new StackTraceElement[st.length];
    for (int i = 0; i < st.length; i++) {
      newSt[i] = resymbolize(st[i], strongName);
    }
    return newSt;
  }
  
  private Throwable deobfuscateThrowable(Throwable t, String strongName) {
    if (t.getStackTrace() != null) {
      t.setStackTrace(deobfuscateStackTrace(t.getStackTrace(), strongName));
    }
    if (t.getCause() != null) {
      t.initCause(deobfuscateThrowable(t.getCause(), strongName));
    }
    return t;
  }
  
  private SymbolMap loadSymbolMap(
      String strongName) {
    SymbolMap toReturn = symbolMaps.get(strongName);
    if (toReturn != null) {
      return toReturn;
    }
    toReturn = new SymbolMap();
    String line;
    String filename = symbolMapsDirectory + strongName + ".symbolMap";
    try {
      BufferedReader bin = new BufferedReader(new FileReader(filename));
      while ((line = bin.readLine()) != null) {
        if (line.charAt(0) == '#') {
          continue;
        }
        int idx = line.indexOf(',');
        toReturn.put(new String(line.substring(0, idx)),
            line.substring(idx + 1));
      }
    } catch (IOException e) {
      toReturn = null;
    }

    symbolMaps.put(strongName, toReturn);
    return toReturn;
  }
  
  private String[] parse(String refString) {
    Matcher matcher = JsniRefPattern.matcher(refString);
    if (!matcher.matches()) {
      return null;
    }
    String className = matcher.group(1);
    String memberName = matcher.group(2);
    String[] toReturn = new String[] {className, memberName};
    return toReturn;
  }
  
  private StackTraceElement resymbolize(StackTraceElement ste,
      String strongName) {
    SymbolMap map = loadSymbolMap(strongName);
    String symbolData = map == null ? null : map.get(ste.getMethodName());

    if (symbolData != null) {
      // jsniIdent, className, memberName, sourceUri, sourceLine
      String[] parts = symbolData.split(",");
      if (parts.length == 5) {
        String[] ref = parse(
            parts[0].substring(0, parts[0].lastIndexOf(')') + 1));
        return new StackTraceElement(
            ref[0], ref[1], ste.getFileName(), ste.getLineNumber());
      }
    }
    // If anything goes wrong, just return the unobfuscated element
    return ste;
  }
}
