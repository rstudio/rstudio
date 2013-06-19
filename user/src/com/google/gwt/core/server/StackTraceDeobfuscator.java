/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.core.server;

import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapConsumerFactory;
import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapping;
import com.google.gwt.thirdparty.debugging.sourcemap.proto.Mapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deobfuscates stack traces on the server side. This class requires that you have turned on
 * emulated stack traces, via <code>&lt;set-property name="compiler.stackMode" value="emulated"
 * /&gt;</code> in your <code>.gwt.xml</code> module file for browsers that don't support
 * sourcemaps or <code>&lt;set-property name="compiler.useSourceMaps" value="true"/&gt;</code> for
 * browsers that support it (e.g. Chrome), and moved your symbol map files to a location accessible
 * by your server-side code. You can use the GWT compiler <code>-deploy</code> command line
 * argument to specify the location of the folder into which the generated <code>symbolMaps</code>
 * directory is written. By default, the final <code>symbolMaps</code> directory is
 * <code>war/WEB-INF/deploy/<i>yourmodulename</i>/symbolMaps/</code>.
 */
public abstract class StackTraceDeobfuscator {

  /**
   * Creates a deobfuscator that loads symbol and source map files under given resource path. Uses
   * StackTraceObfuscator's {@link ClassLoader}.
   */
  public static StackTraceDeobfuscator fromResource(String symbolMapsPath) {
    final String basePath = symbolMapsPath.endsWith("/") ? symbolMapsPath : symbolMapsPath + "/";
    final ClassLoader classLoader = StackTraceDeobfuscator.class.getClassLoader();
    return new StackTraceDeobfuscator() {
      protected InputStream openInputStream(String fileName) throws IOException {
        String filePath = basePath + fileName;
        InputStream inputStream = classLoader.getResourceAsStream(filePath);
        if (inputStream == null) {
          throw new IOException("Missing resource: " + filePath);
        }
        return inputStream;
      }
    };
  }

  /**
   * Creates a deobfuscator that loads symbol and source map files from the given directory.
   */
  public static StackTraceDeobfuscator fromFileSystem(final String symbolMapsDirectory) {
    return new StackTraceDeobfuscator() {
      protected InputStream openInputStream(String fileName) throws IOException {
        return new FileInputStream(new File(symbolMapsDirectory, fileName));
      }
    };
  }

  /**
   * Creates a deobfuscator that loads symbol and source map files beneath the given URL.
   */
  public static StackTraceDeobfuscator fromUrl(final URL urlPath) {
    return new StackTraceDeobfuscator() {
      protected InputStream openInputStream(String fileName) throws IOException {
        return new URL(urlPath, fileName).openStream();
      }
    };
  }

  /**
   * A cache that maps obfuscated symbols to arbitrary non-null string values. The cache can assume
   * each (strongName, symbol) pair always maps to the same value (never goes invalid), but must
   * treat data as an opaque string.
   */
  private static class SymbolCache {
    // TODO(srogoff): This SymbolCache implementation never drops old entries. If clients ever need
    // to cap memory usage even with lazy loading, consider making SymbolCache an interface.
    // This could allow clients to pass their own implementation to the StackTraceDeobfuscator
    // constructor, backed by a Guava Cache or other entry-evicting mapping.

    private final ConcurrentHashMap<String, HashMap<String, String>> symbolMaps;

    SymbolCache() {
      symbolMaps = new ConcurrentHashMap<String, HashMap<String, String>>();
    }

    /**
     * Adds some symbol data to the cache for the given strong name.
     */
    void putAll(String strongName, Map<String, String> symbolMap) {
      if (strongName == null || symbolMap.size() == 0) {
        return;
      }
      symbolMaps.putIfAbsent(strongName, new HashMap<String, String>());
      HashMap<String, String> existingMap = symbolMaps.get(strongName);
      synchronized (existingMap) {
        existingMap.putAll(symbolMap);
      }
    }

    /**
     * Returns the data for each of the specified symbols that's currently cached for the given
     * strong name. There will be no entry for symbols that are not in the cache. If none of the
     * symbols are cached, an empty Map is returned.
     */
    Map<String, String> getAll(String strongName, Set<String> symbols) {
      Map<String, String> toReturn = new HashMap<String, String>();
      if (strongName == null || !symbolMaps.containsKey(strongName) || symbols.isEmpty()) {
        return toReturn;
      }
      HashMap<String, String> existingMap = symbolMaps.get(strongName);
      synchronized (existingMap) {
        for (String symbol : symbols) {
          if (existingMap.containsKey(symbol)) {
            toReturn.put(symbol, existingMap.get(symbol));
          }
        }
      }
      return toReturn;
    }
  }

  private static final Pattern JsniRefPattern = Pattern.compile("@?([^:]+)::([^(]+)(\\((.*)\\))?");
  private static final Pattern fragmentIdPattern = Pattern.compile(".*(\\d+)\\.js");
  private static final int LINE_NUMBER_UNKNOWN = -1;
  private static final String SYMBOL_DATA_UNKNOWN = "";

  private final Map<String, SourceMapping> sourceMaps = new HashMap<String, SourceMapping>();
  private final SymbolCache symbolCache = new SymbolCache();
  private boolean lazyLoad = false;

  /**
   * If set to {@code true}, only symbols requested to be deobfuscated are cached and the rest is
   * discarded. This provides a large memory savings at the expense of occasional extra disk reads.
   * Note that, this will only have effect on symbol maps that haven't been fully loaded yet.
   */
  public void setLazyLoad(boolean lazyLoad) {
    this.lazyLoad = lazyLoad;
  }

  /**
   * Replaces the stack traces in the given Throwable and its causes with deobfuscated stack traces
   * wherever possible.
   *
   * @param throwable the Throwable that needs its stack trace to be deobfuscated
   * @param strongName the GWT permutation strong name
   */
  public final void deobfuscateStackTrace(Throwable throwable, String strongName) {
    throwable.setStackTrace(resymbolize(throwable.getStackTrace(), strongName));
    if (throwable.getCause() != null) {
      deobfuscateStackTrace(throwable.getCause(), strongName);
    }
  }

  /**
   * Convenience method which resymbolizes an entire stack trace to extent possible.
   *
   * @param st the stack trace to resymbolize
   * @param strongName the GWT permutation strong name
   * @return a best effort resymbolized stack trace
   */
  public final StackTraceElement[] resymbolize(StackTraceElement[] st, String strongName) {
    if (st == null) {
      return null;
    }
    // Warm the symbol cache for all symbols in this stack trace.
    Set<String> requiredSymbols = new HashSet<String>();
    for (StackTraceElement ste : st) {
      requiredSymbols.add(ste.getMethodName());
    }
    loadSymbolMap(strongName, requiredSymbols);

    StackTraceElement[] newSt = new StackTraceElement[st.length];
    for (int i = 0; i < st.length; i++) {
      newSt[i] = resymbolize(st[i], strongName);
    }
    return newSt;
  }

  /**
   * Best effort resymbolization of a single stack trace element.
   *
   * @param ste the stack trace element to resymbolize
   * @param strongName the GWT permutation strong name
   * @return the best effort resymbolized stack trace element
   */
  public final StackTraceElement resymbolize(StackTraceElement ste, String strongName) {
    String declaringClass = null;
    String methodName = null;
    String filename = null;
    int lineNumber = -1;
    int fragmentId = -1;

    String steFilename = ste.getFileName();
    String symbolData = loadOneSymbol(strongName, ste.getMethodName());

    boolean sourceMapCapable = false;

    int column = 1;
    // column information is encoded in filename after '@' for sourceMap capable browsers
    if (steFilename != null) {
      int columnMarkerIndex = steFilename.indexOf("@");
      if (columnMarkerIndex != -1) {
        try {
          column = Integer.parseInt(steFilename.substring(columnMarkerIndex + 1));
          sourceMapCapable = true;
        } catch (NumberFormatException nfe) {
        }
        steFilename = steFilename.substring(0, columnMarkerIndex);
      }
    }

    // first use symbolMap, then refine via sourceMap if possible
    if (!symbolData.isEmpty()) {
      // jsniIdent, className, memberName, sourceUri, sourceLine, fragmentId
      String[] parts = symbolData.split(",");
      if (parts.length == 6) {
        String[] ref = parse(
            parts[0].substring(0, parts[0].lastIndexOf(')') + 1));

        if (ref != null) {
          declaringClass = ref[0];
          methodName = ref[1];
        } else {
          declaringClass = ste.getClassName();
          methodName = ste.getMethodName();
        }

        // parts[3] contains the source file URI or "Unknown"
        filename = "Unknown".equals(parts[3]) ? null
            : parts[3].substring(parts[3].lastIndexOf('/') + 1);

        lineNumber = ste.getLineNumber();

        /*
         * When lineNumber is LINE_NUMBER_UNKNOWN, either because
         * compiler.stackMode is not emulated or
         * compiler.emulatedStack.recordLineNumbers is false, use the method
         * declaration line number from the symbol map.
         */
        if (lineNumber == LINE_NUMBER_UNKNOWN || (sourceMapCapable && column == -1)) {
          // Safari will send line numbers, with col == -1, we need to use symbolMap in this case
          lineNumber = Integer.parseInt(parts[4]);
        }

        fragmentId = Integer.parseInt(parts[5]);
      }
    }

    // anonymous function, try to use <fragmentNum>.js:line to determine fragment id
    if (fragmentId == -1 && steFilename != null) {
      // fragment identifier encoded in filename
      Matcher matcher = fragmentIdPattern.matcher(steFilename);
      if (matcher.matches()) {
        String fragment = matcher.group(1);
        try {
          fragmentId = Integer.parseInt(fragment);
        } catch (Exception e) {
        }
      } else if (steFilename.contains(strongName)) {
        // else it's <strongName>.cache.js which is the 0th fragment
        fragmentId = 0;
      }
    }

    int jsLineNumber = ste.getLineNumber();

    // try to refine location via sourcemap
    if (sourceMapCapable && fragmentId != -1 && column != -1) {
      SourceMapping sourceMapping = loadSourceMap(strongName, fragmentId);
      if (sourceMapping != null && ste.getLineNumber() > -1) {
        Mapping.OriginalMapping mappingForLine = sourceMapping
            .getMappingForLine(jsLineNumber, column);
        if (mappingForLine != null) {

          if (declaringClass == null || declaringClass.equals(ste.getClassName())) {
            declaringClass = mappingForLine.getOriginalFile();
            methodName = mappingForLine.getIdentifier();
          }
          filename = mappingForLine.getOriginalFile();
          lineNumber = mappingForLine.getLineNumber();
        }
      }
    }

    if (declaringClass != null) {
      return new StackTraceElement(declaringClass, methodName, filename, lineNumber);
    }

    // If anything goes wrong, just return the unobfuscated element
    return ste;
  }

  protected InputStream getSourceMapInputStream(String permutationStrongName, int fragmentNumber)
      throws IOException {
    return openInputStream(permutationStrongName + "_sourceMap" + fragmentNumber + ".json");
  }

  /**
   * Retrieves a new {@link InputStream} for the given permutation strong name. This implementation,
   * which subclasses may override, returns a {@link InputStream} for the <code>
   * <i>permutation-strong-name</i>.symbolMap</code> file.
   *
   * @param permutationStrongName the GWT permutation strong name
   * @return a new {@link InputStream}
   */
  protected InputStream getSymbolMapInputStream(String permutationStrongName) throws IOException {
    return openInputStream(permutationStrongName + ".symbolMap");
  }

  /**
   * Opens a new {@link InputStream} for a symbol or source map file.
   *
   * @param fileName name of the symbol or source map file
   * @return an input stream for reading the file (doesn't need to be buffered).
   * @exception IOException if an I/O error occurs while creating the input stream.
   */
  protected abstract InputStream openInputStream(String fileName) throws IOException;

  private SourceMapping loadSourceMap(String permutationStrongName, int fragmentId) {
    SourceMapping toReturn = sourceMaps.get(permutationStrongName + fragmentId);
    if (toReturn == null) {
      try {
        String sourceMapString = loadStreamAsString(
            getSourceMapInputStream(permutationStrongName, fragmentId));
        toReturn = SourceMapConsumerFactory.parse(sourceMapString);
        sourceMaps.put(permutationStrongName + fragmentId, toReturn);
      } catch (Exception e) {
      }
    }
    return toReturn;
  }

  private String loadStreamAsString(InputStream stream) {
    return new Scanner(stream).useDelimiter("\\A").next();
  }

  private String loadOneSymbol(String strongName, String symbol) {
    Set<String> symbolSet = new HashSet<String>();
    symbolSet.add(symbol);
    Map<String, String> symbolMap = loadSymbolMap(strongName, symbolSet);
    return symbolMap.get(symbol);
  }

  /**
   * Returns a symbol map for the given strong name containing symbol data for
   * all of the given required symbols. First checks the symbol cache, then
   * reads from disk if any symbol is missing. If a symbol cannot be loaded for
   * some reason, it will be mapped to empty string.
   */
  private Map<String, String> loadSymbolMap(
      String strongName, Set<String> requiredSymbols) {
    Map<String, String> toReturn = symbolCache.getAll(strongName, requiredSymbols);
    if (toReturn.size() == requiredSymbols.size()) {
      return toReturn;
    }

    Set<String> symbolsLeftToFind = new HashSet<String>(requiredSymbols);
    toReturn = new HashMap<String, String>();
    String line;

    try {
      BufferedReader bin = new BufferedReader(
          new InputStreamReader(getSymbolMapInputStream(strongName)));
      try {
        while ((line = bin.readLine()) != null && (symbolsLeftToFind.size() > 0 || !lazyLoad)) {
          if (line.charAt(0) == '#') {
            continue;
          }
          int idx = line.indexOf(',');
          String symbol = line.substring(0, idx);
          String symbolData = line.substring(idx + 1);
          if (requiredSymbols.contains(symbol) || !lazyLoad) {
            symbolsLeftToFind.remove(symbol);
            toReturn.put(symbol, symbolData);
          }
        }
      } finally {
        bin.close();
      }
    } catch (IOException e) {
      // If the symbol map isn't found or there's an I/O error reading the file, the returned
      // mapping may contain some or all empty data (see below).
    }
    for (String symbol : symbolsLeftToFind) {
      // Store the empty string in the symbolCache to show we actually looked on disk and couldn't
      // find the symbols. This avoids reading disk repeatedly for symbols that can't be translated.
      toReturn.put(symbol, SYMBOL_DATA_UNKNOWN);
    }

    symbolCache.putAll(strongName, toReturn);
    return toReturn;
  }

  /**
   * Extracts the declaring class and method name from a JSNI ref, or null if the information cannot
   * be extracted.
   *
   * @param refString symbol map reference string
   * @return a string array contains the declaring class and method name, or null when the regex
   *         match fails
   * @see com.google.gwt.dev.util.JsniRef
   */
  private String[] parse(String refString) {
    Matcher matcher = JsniRefPattern.matcher(refString);
    if (!matcher.matches()) {
      return null;
    }
    String className = matcher.group(1);
    String memberName = matcher.group(2);
    String[] toReturn = new String[]{className, memberName};
    return toReturn;
  }
}
