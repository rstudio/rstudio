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
import com.google.gwt.dev.CompilerOptionsImpl;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.util.tools.Utility;

import junit.framework.TestCase;

import org.eclipse.jdt.internal.compiler.problem.ShouldNotImplement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

/**
 * Basic tests for Source maps and (new) soyc reports.
 *
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
    public CastableTypeMap getCastableTypeMap() {
      return null;
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
      throw new ShouldNotImplement(NOT_IMPLEMENTED_MESSAGE);
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

  private final CompilerOptionsImpl options = new CompilerOptionsImpl();
  // maps permutationId to symbolMap content
  private Map<Integer, Map<String, SimpleSymbolData>> mapping =
      Maps.newHashMap();

  /**
   * Loads the symbol map from a file.
   */
  private Map<String, SimpleSymbolData> loadSymbolMap(File root) throws Exception {
    // Testing SourceMaps as SymbolMap replacement
    // make sure the files have been produced
    assertTrue(root.exists());

    File[] symbolMapFiles = filterByName(root, "(.*)\\.symbolMap");

    assertTrue(symbolMapFiles.length >= 1);

    return SimpleSymbolData.readSymbolMap(symbolMapFiles[0]);
  }

  private static final String JSE_METHOD =
      "com.google.gwt.core.client.JavaScriptException::getThrown()Ljava/lang/Object;";
  private static final String JSE_FIELD = "com.google.gwt.core.client.JavaScriptException::message";
  private static final String JSE_CLASS = "com.google.gwt.core.client.JavaScriptException";
  private static final String UNINSTANTIABLE_CLASS = "com.google.gwt.lang.Array";

  /**
   * Tests for the presence of some elements.
   */
  public void testSymbolMapSanity() throws Exception {
    String benchmark = "hello";
    String module = "com.google.gwt.sample.hello.Hello";

    File work = Utility.makeTemporaryDirectory(null, benchmark + "work");
    try {
      options.addModuleName(module);
      options.setWarDir(new File(work, "war"));
      options.setExtraDir(new File(work, "extra"));
      PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(TreeLogger.ERROR);
      new com.google.gwt.dev.Compiler(options).run(logger);
      // Change parentDir for cached/pre-built reports
      String parentDir = options.getExtraDir() + "/" + benchmark;
      Map<String, SimpleSymbolData> symbolDataBySymbolName =
          loadSymbolMap(new File(parentDir + "/symbolMaps/"));
      assertTrue(!symbolDataBySymbolName.isEmpty());
      assertNotNull(symbolDataBySymbolName.get(JSE_METHOD));
      assertTrue(symbolDataBySymbolName.get(JSE_METHOD).isMethod());
      assertFalse(symbolDataBySymbolName.get(JSE_METHOD).isField());
      assertFalse(symbolDataBySymbolName.get(JSE_METHOD).isClass());
      assertNotNull(symbolDataBySymbolName.get(JSE_FIELD));
      assertTrue(symbolDataBySymbolName.get(JSE_FIELD).isField());
      assertFalse(symbolDataBySymbolName.get(JSE_FIELD).isMethod());
      assertFalse(symbolDataBySymbolName.get(JSE_FIELD).isClass());
      assertNotNull(symbolDataBySymbolName.get(JSE_CLASS));
      assertTrue(symbolDataBySymbolName.get(JSE_CLASS).isClass());
      assertFalse(symbolDataBySymbolName.get(JSE_CLASS).isField());
      assertFalse(symbolDataBySymbolName.get(JSE_CLASS).isMethod());
      // There should not be a mapping for uninstantiable classes.
      // TODO(rluble): Uncomment the following line. It is commented because it
      // makes the test flaky, indicating a deeper problem.
      // assertNull(symbolDataBySymbolName.get(UNINSTANTIABLE_CLASS));
    } finally {
      Util.recursiveDelete(work, false);
    }
  }
}