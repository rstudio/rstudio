/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.GWTCompiler.GWTCompilerOptionsImpl;
import com.google.gwt.dev.jjs.JsOutputOption;

import java.io.File;

/**
 * Test for deprecated {@link GWTShell}.
 */
@SuppressWarnings("deprecation")
public class GWTCompilerTest extends ArgProcessorTestBase {

  private final GWTCompiler.ArgProcessor argProcessor;
  private final GWTCompilerOptionsImpl options = new GWTCompilerOptionsImpl();

  public GWTCompilerTest() {
    argProcessor = new GWTCompiler.ArgProcessor(options);
  }

  public void testAllValidArgs() {
    assertProcessSuccess(argProcessor, "-logLevel", "DEBUG", "-style",
        "PRETTY", "-ea", "-XdisableAggressiveOptimization", "-out", "myWww",
        "-gen", "myGen", "c.g.g.h.H", "my.Module");

    assertEquals(new File("myGen").getAbsoluteFile(),
        options.getGenDir().getAbsoluteFile());
    assertEquals(new File("myWww"), options.getOutDir());

    assertEquals(TreeLogger.DEBUG, options.getLogLevel());
    assertEquals(JsOutputOption.PRETTY, options.getOutput());
    assertTrue(options.isEnableAssertions());
    assertFalse(options.isAggressivelyOptimize());

    assertEquals(2, options.getModuleNames().size());
    assertEquals("c.g.g.h.H", options.getModuleNames().get(0));
    assertEquals("my.Module", options.getModuleNames().get(1));
  }

  public void testDefaultArgs() {
    assertProcessSuccess(argProcessor, "c.g.g.h.H");

    assertEquals(null, options.getGenDir());
    assertEquals(new File("").getAbsoluteFile(),
        options.getOutDir().getAbsoluteFile());

    assertEquals(TreeLogger.INFO, options.getLogLevel());
    assertEquals(JsOutputOption.OBFUSCATED, options.getOutput());
    assertFalse(options.isEnableAssertions());
    assertTrue(options.isAggressivelyOptimize());

    assertEquals(1, options.getModuleNames().size());
    assertEquals("c.g.g.h.H", options.getModuleNames().get(0));
  }

  public void testForbiddenArgs() {
    assertProcessFailure(argProcessor, "-localWorkers", "2");
    assertProcessFailure(argProcessor, "-extra", "extra");
    assertProcessFailure(argProcessor, "-war", "war");
    assertProcessFailure(argProcessor, "-work", "work");
  }
}
