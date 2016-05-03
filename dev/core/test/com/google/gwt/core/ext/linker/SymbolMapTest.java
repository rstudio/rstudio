/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.core.ext.linker;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.CompilerOptionsImpl;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.OptionOptimize;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.util.tools.Utility;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * Basic tests for Source maps and (new) soyc reports.
 */
public class SymbolMapTest extends TestCase {

  private static File[] filterByName(File dir, final String namePattern) {
    return dir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.getName().matches(namePattern);
      }
    });
  }

  /**
   * This class represents each row in a generated SymbolMap file.  Because not all fields are
   * serialized, such as CastableTypeMap, some methods are not implemented.
   *
   */
  static final class SimpleSymbolData implements SymbolData {

    static Map<String, SimpleSymbolData> readSymbolMap(File filePath) throws IOException {
      Map<String, SimpleSymbolData> sdata = Maps.newLinkedHashMap();

      BufferedReader reader = new BufferedReader(new FileReader(filePath));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("#")) {
          // reading a comment
          continue;
        }

        SimpleSymbolData symbolData = new SimpleSymbolData(line);
        String key = symbolData.getJsniIdent();
        assertFalse("Duplicate signature <" + key + "> in symbol maps", sdata.containsKey(key));
        sdata.put(key,symbolData);
      }

      return sdata;
    }

    private static final String NOT_IMPLEMENTED_MESSAGE =
        "Data not available in current serialized SymbolMap";

    private String jsName;
    private String jsniIdent;
    private String className;
    private String memberName;
    private String sourceUri;
    private int sourceLine;
    private int fragmentNumber;

    public SimpleSymbolData(String line) {
      this.parseFromLine(line);
    }

    @Override
    public String getClassName() {
      return this.className;
    }

    @Override
    public int getFragmentNumber() {
      return this.fragmentNumber;
    }

    @Override
    public String getJsniIdent() {
      return this.jsniIdent;
    }

    @Override
    public String getMemberName() {
      return this.memberName;
    }

    @Override
    public String getRuntimeTypeId() {
      throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    @Override
    public int getSourceLine() {
      return this.sourceLine;
    }

    @Override
    public String getSourceUri() {
      return this.sourceUri;
    }

    @Override
    public String getSymbolName() {
      return this.jsName;
    }

    @Override
    public boolean isClass() {
      return this.memberName == null || this.memberName.isEmpty();
    }

    @Override
    public boolean isField() {
      return !this.isClass() && jsniIdent.indexOf("(") < 0;
    }

    @Override
    public boolean isMethod() {
      return !this.isClass() && jsniIdent.indexOf("(") >= 0;
    }

    @Override
    public String toString() {
      return jsniIdent + " -> " + jsName;
    }

    private void parseFromLine(String line) {
      String[] fields = line.split(",");

      this.jsName = fields[0];
      this.jsniIdent = fields[1].isEmpty() ? fields[2] : fields[1];
      this.className = fields[2];
      this.memberName = fields[3]; // may be empty
      this.sourceUri = fields[4];
      this.sourceLine = Integer.parseInt(fields[5]);
      this.fragmentNumber = Integer.parseInt(fields[6]);
    }
  }

  /**
   * Loads the symbol map from a file.
   */
  private Iterable<Map<String, SimpleSymbolData>> loadSymbolMaps(File root) throws Exception {
    // Testing SourceMaps as SymbolMap replacement
    // make sure the files have been produced
    assertTrue(root.exists());

    File[] symbolMapFiles = filterByName(root, "(.*)\\.symbolMap");

    assertTrue(symbolMapFiles.length >= 1);

    return Iterables.transform(Arrays.asList(symbolMapFiles),
        new Function<File, Map<String, SimpleSymbolData>>() {
          @Override
          public Map<String, SimpleSymbolData> apply(File file) {
            try {
              return SimpleSymbolData.readSymbolMap(file);
            } catch (IOException e) {
              fail("Error reading symbol map " + file.getAbsolutePath());
            }
            return null;
          }
        });
  }

  private static final String JSE_METHOD =
      "com.google.gwt.core.client.JavaScriptException::getThrown()Ljava/lang/Object;";
  private static final String JSE_FIELD = "com.google.gwt.core.client.JavaScriptException::message";
  private static final String JSE_CLASS = "com.google.gwt.core.client.JavaScriptException";
  private static final String UNINSTANTIABLE_CLASS = "com.google.gwt.lang.Array";

  /**
   * Tests for the presence of some elements.
   */
  private void assertSymbolMapSanity(int optimizeLevel) throws IOException,
      UnableToCompleteException, Exception {
    String benchmark = "hello";
    String module = "com.google.gwt.sample.hello.Hello";

    File work = Utility.makeTemporaryDirectory(null, benchmark + "work");
    try {
      CompilerOptionsImpl options = new CompilerOptionsImpl();
      options.addModuleName(module);
      options.setWarDir(new File(work, "war"));
      options.setExtraDir(new File(work, "extra"));
      options.setOptimizationLevel(optimizeLevel);
      PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(TreeLogger.ERROR);
      Compiler.compile(logger, options);
      // Change parentDir for cached/pre-built reports
      String parentDir = options.getExtraDir() + "/" + benchmark;
      for (Map<String, SimpleSymbolData> symbolDataByJsniIdentifier :
          loadSymbolMaps(new File(parentDir + "/symbolMaps/"))) {
        assertTrue(!symbolDataByJsniIdentifier.isEmpty());
        assertNotNull(symbolDataByJsniIdentifier.get(JSE_METHOD));
        assertTrue(symbolDataByJsniIdentifier.get(JSE_METHOD).isMethod());
        assertFalse(symbolDataByJsniIdentifier.get(JSE_METHOD).isField());
        assertFalse(symbolDataByJsniIdentifier.get(JSE_METHOD).isClass());
        assertNotNull(symbolDataByJsniIdentifier.get(JSE_FIELD));
        assertTrue(symbolDataByJsniIdentifier.get(JSE_FIELD).isField());
        assertFalse(symbolDataByJsniIdentifier.get(JSE_FIELD).isMethod());
        assertFalse(symbolDataByJsniIdentifier.get(JSE_FIELD).isClass());
        assertNotNull(symbolDataByJsniIdentifier.get(JSE_CLASS));
        assertTrue(symbolDataByJsniIdentifier.get(JSE_CLASS).isClass());
        assertFalse(symbolDataByJsniIdentifier.get(JSE_CLASS).isField());
        assertFalse(symbolDataByJsniIdentifier.get(JSE_CLASS).isMethod());
        if (optimizeLevel == OptionOptimize.OPTIMIZE_LEVEL_DRAFT) {
          assertNotNull(symbolDataByJsniIdentifier.get(UNINSTANTIABLE_CLASS));
        } else {
          assertNull(symbolDataByJsniIdentifier.get(UNINSTANTIABLE_CLASS));
        }
        assertSymbolUniquenessForMethods(symbolDataByJsniIdentifier);
      }
    } finally {
      Util.recursiveDelete(work, false);
    }
  }

  private void assertSymbolUniquenessForMethods(
      Map<String, SimpleSymbolData> symbolDataByJsniIdentifier) {
    Multimap<String, SymbolData> methodSymbolDataBySymbol = HashMultimap.create();
    for (SymbolData symbolData : symbolDataByJsniIdentifier.values()) {
      if (symbolData.isMethod()) {
        methodSymbolDataBySymbol.put(symbolData.getSymbolName(), symbolData);
      }
    }
    Iterator<String> iterator = methodSymbolDataBySymbol.keySet().iterator();
    while (iterator.hasNext()) {
      String key = iterator.next();
      if (methodSymbolDataBySymbol.get(key).size() <= 1) {
        iterator.remove();
      }
    }
    assertTrue("The following method symbols where not unique " + methodSymbolDataBySymbol,
        methodSymbolDataBySymbol.isEmpty());
  }

  public void testSymbolMapSanityDraft() throws Exception {
    assertSymbolMapSanity(OptionOptimize.OPTIMIZE_LEVEL_DRAFT);
  }

  public void testSymbolMapSanityOptimized() throws Exception {
    assertSymbolMapSanity(OptionOptimize.OPTIMIZE_LEVEL_MAX);
  }
}
