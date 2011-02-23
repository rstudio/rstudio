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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deobfuscates stack traces on the server side. This class requires that you
 * have turned on emulated stack traces, via
 * <code>&lt;set-property name="compiler.stackMode" value="emulated" /&gt;</code>
 * in your <code>.gwt.xml</code> module file, and moved your symbol map files to
 * a location accessible by your server sever side code. You can use the GWT
 * compiler <code>-deploy</code> command line argument to specify the location
 * of the folder into which the generated <code>symbolMaps</code> directory is
 * written. By default, the final <code>symbolMaps</code> directory is
 * <code>war/WEB-INF/deploy/<i>yourmodulename</i>/symbolMaps/</code>. Pass the
 * resulting directory location into this class'
 * {@link StackTraceDeobfuscator#symbolMapsDirectory} constructor or
 * {@link #setSymbolMapsDirectory(String)} setter method.
 * 
 * TODO(unnurg): Combine this code with similar code in JUnitHostImpl
 */
public class StackTraceDeobfuscator {
  
  private static class SymbolMap extends HashMap<String, String> { }
  
  // From JsniRef class, which is in gwt-dev and so can't be accessed here
  // TODO(unnurg) once there is a place for shared code, move this to there.
  private static Pattern JsniRefPattern =
    Pattern.compile("@?([^:]+)::([^(]+)(\\((.*)\\))?");
  
  private File symbolMapsDirectory;
  
  private Map<String, SymbolMap> symbolMaps =
    new HashMap<String, SymbolMap>();

  /**
   * Constructor, which takes a <code>symbolMaps</code> directory as its
   * argument. Symbol maps are generated into the location specified by the
   * GWT compiler <code>-deploy</code> command line argument.
   * 
   * @param symbolMapsDirectory the <code>symbolMaps</code> directory with, or
   *          without trailing directory separator character
   */
  public StackTraceDeobfuscator(String symbolMapsDirectory) {
    setSymbolMapsDirectory(symbolMapsDirectory);
  }
  
  /**
   * Best effort resymbolization of a log record's stack trace.
   * 
   * @param lr the log record to resymbolize
   * @param strongName the GWT permutation strong name
   * @return the best effort resymbolized log record
   */
  public LogRecord deobfuscateLogRecord(LogRecord lr, String strongName) {
    if (lr.getThrown() != null && strongName != null) {
      lr.setThrown(deobfuscateThrowable(lr.getThrown(), strongName));
    }
    return lr;
  }
  
  /**
   * Convenience method which resymbolizes an entire stack trace to extent
   * possible.
   *
   * @param st the stack trace to resymbolize
   * @param strongName the GWT permutation strong name
   * @return a best effort resymbolized stack trace
   */
  public StackTraceElement[] deobfuscateStackTrace(
      StackTraceElement[] st, String strongName) {
    StackTraceElement[] newSt = new StackTraceElement[st.length];
    for (int i = 0; i < st.length; i++) {
      newSt[i] = resymbolize(st[i], strongName);
    }
    return newSt;
  }
  
  /**
   * Best effort resymbolization of a a single stack trace element.
   *
   * @param ste the stack trace element to resymbolize
   * @param strongName the GWT permutation strong name
   * @return the best effort resymbolized stack trace element
   */
  public StackTraceElement resymbolize(StackTraceElement ste,
      String strongName) {
    SymbolMap map = loadSymbolMap(strongName);
    String symbolData = map == null ? null : map.get(ste.getMethodName());

    if (symbolData != null) {
      // jsniIdent, className, memberName, sourceUri, sourceLine
      String[] parts = symbolData.split(",");
      if (parts.length == 5) {
        String[] ref = parse(
            parts[0].substring(0, parts[0].lastIndexOf(')') + 1));

        String declaringClass;
        String methodName;
        if (ref != null) {
          declaringClass = ref[0];
          methodName = ref[1];
        } else {
          declaringClass = ste.getClassName();
          methodName = ste.getMethodName();
        }
        
        // parts[3] contains the source file URI or "Unknown"
        String filename = "Unknown".equals(parts[3]) ? null
            : parts[3].substring(parts[3].lastIndexOf('/') + 1);
        
        int lineNumber = ste.getLineNumber();
        /*
         * When lineNumber is zero, either because compiler.stackMode is not
         * emulated or compiler.emulatedStack.recordLineNumbers is false, use
         * the method declaration line number from the symbol map.
         */
        if (lineNumber == 0) {
          lineNumber = Integer.parseInt(parts[4]);
        }
        
        return new StackTraceElement(
            declaringClass, methodName, filename, lineNumber);
      }
    }
    // If anything goes wrong, just return the unobfuscated element
    return ste;
  }
  
  public void setSymbolMapsDirectory(String symbolMapsDirectory) {
    // permutations are unique, no need to clear the symbolMaps hash map
    this.symbolMapsDirectory = new File(symbolMapsDirectory);
  }

  /**
   * Retrieves a new {@link InputStream} for the given permutation strong name.
   * This implementation, which subclasses may override, returns a
   * {@link InputStream} for the <code>
   * <i>permutation-strong-name</i>.symbolMap</code> file in the
   * <code>symbolMaps</code> directory.
   *
   * @param permutationStrongName the GWT permutation strong name
   * @return a new {@link InputStream}
   * @throws IOException
   */
  protected InputStream getSymbolMapInputStream(String permutationStrongName)
      throws IOException {
    String filename = symbolMapsDirectory.getCanonicalPath()
        + File.separatorChar + permutationStrongName + ".symbolMap";
    return new FileInputStream(filename);
  }
  
  private Throwable deobfuscateThrowable(Throwable old, String strongName) {
    Throwable t = new Throwable(old.getMessage());
    if (old.getStackTrace() != null) {
      t.setStackTrace(deobfuscateStackTrace(old.getStackTrace(), strongName));
    } else {
      t.setStackTrace(new StackTraceElement[0]);
    }
    if (old.getCause() != null) {
      t.initCause(deobfuscateThrowable(old.getCause(), strongName));
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

    try {
      BufferedReader bin = new BufferedReader(
          new InputStreamReader(getSymbolMapInputStream(strongName)));
      try {
        while ((line = bin.readLine()) != null) {
          if (line.charAt(0) == '#') {
            continue;
          }
          int idx = line.indexOf(',');
          toReturn.put(new String(line.substring(0, idx)),
                       line.substring(idx + 1));
        }
      } finally {
        bin.close();
      }
    } catch (IOException e) {
      //  use empty symbol map to avoid repeated lookups
      toReturn = new SymbolMap();
    }

    symbolMaps.put(strongName, toReturn);
    return toReturn;
  }

  /**
   * Extracts the declaring class and method name from a JSNI ref, or null if
   * the information cannot be extracted.
   *
   * @see com.google.gwt.dev.util.JsniRef
   * @param refString symbol map reference string
   * @return a string array contains the declaring class and method name, or
   *         null when the regex match fails
   */
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
}
