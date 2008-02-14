/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.GWTCompiler;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.io.File;

/**
 * Tests various permutations of the GWT module's &amp;public&amp; tag,
 * specifically its ant-like inclusion support.
 */
public class PublicTagTest extends TestCase {

  /**
   * Provides a convenient interface to the {@link GWTCompiler}. This test
   * cannot simply call {@link GWTCompiler#main(String[])} since it always
   * terminates with a call to {@link System#exit(int)}.
   */
  private static class Compiler extends GWTCompiler {
    /**
     * Run the {@link GWTCompiler} with the specified arguments.
     * 
     * @param args arguments passed to the compiler.
     */
    static void compile(String[] args) {
      try {
        final Compiler compiler = new Compiler();
        if (compiler.processArgs(args)) {
          final AbstractTreeLogger logger = new PrintWriterTreeLogger();
          logger.setMaxDetail(compiler.getLogLevel());
          compiler.distill(logger, ModuleDefLoader.loadFromClassPath(logger,
              compiler.getModuleName()));
        }
      } catch (UnableToCompleteException e) {
        throw new RuntimeException("Compilation failed.", e);
      }
    }
  }

  public void testPublicTag() {
    // Find the current directory
    String userDir = System.getProperty("user.dir");
    assertNotNull(userDir);
    File curDir = new File(userDir);
    assertTrue(curDir.isDirectory());

    // Our module name is the same as this class's name
    String moduleName = PublicTagTest.class.getName();

    // Find our module output directory and delete it
    File moduleDir = new File(curDir, "www/" + moduleName + "/std");
    if (moduleDir.exists()) {
      Util.recursiveDelete(moduleDir, false);
    }
    assertFalse(moduleDir.exists());

    // Compile the dummy app; suppress output to stdout
    Compiler.compile(new String[] {
        moduleName, "-logLevel", "ERROR", "-out", "www"});

    // Check the output folder
    assertTrue(new File(moduleDir, "good0.html").exists());
    assertTrue(new File(moduleDir, "good1.html").exists());
    assertTrue(new File(moduleDir, "bar/good.html").exists());
    assertTrue(new File(moduleDir, "good2.html").exists());
    assertTrue(new File(moduleDir, "good3.html").exists());
    assertTrue(new File(moduleDir, "good4.html").exists());
    assertTrue(new File(moduleDir, "good5.html").exists());
    assertTrue(new File(moduleDir, "good6.html").exists());
    assertTrue(new File(moduleDir, "good7.html").exists());
    assertTrue(new File(moduleDir, "good8.html").exists());
    assertTrue(new File(moduleDir, "good10.html").exists());
    assertTrue(new File(moduleDir, "good11.html").exists());
    assertTrue(new File(moduleDir, "good9.html").exists());
    assertTrue(new File(moduleDir, "bar/CVS/good.html").exists());
    assertTrue(new File(moduleDir, "CVS/good.html").exists());
    assertTrue(new File(moduleDir, "GOOD/bar/GOOD/good.html").exists());
    assertTrue(new File(moduleDir, "GOOD/good.html").exists());

    assertFalse(new File(moduleDir, "bad.Html").exists());
    assertFalse(new File(moduleDir, "bar/CVS/bad.html").exists());
    assertFalse(new File(moduleDir, "CVS/bad.html").exists());
    assertFalse(new File(moduleDir, "bad1.html").exists());
    assertFalse(new File(moduleDir, "bad2.html").exists());
    assertFalse(new File(moduleDir, "bad3.html").exists());
    assertFalse(new File(moduleDir, "bad.html").exists());
    assertFalse(new File(moduleDir, "bar/bad.html").exists());
    assertFalse(new File(moduleDir, "GOOD/bar/bad.html").exists());
    assertFalse(new File(moduleDir, "GOOD/bar/GOOD/bar/bad.html").exists());
  }

}
