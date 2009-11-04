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
import com.google.gwt.dev.GWTShell.ShellOptionsImpl;
import com.google.gwt.dev.HostedModeTest.MySCL;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.shell.BrowserWidgetHostChecker;

import java.io.File;

/**
 * Test for deprecated {@link GWTShell}.
 */
@SuppressWarnings("deprecation")
public class GWTShellTest extends ArgProcessorTestBase {

  private final GWTShell.ArgProcessor argProcessor;
  private final ShellOptionsImpl options = new ShellOptionsImpl();

  public GWTShellTest() {
    argProcessor = new GWTShell.ArgProcessor(options, false, false);
  }

  public void testAllValidArgs() {
    assertProcessSuccess(argProcessor, "-port", "8080", "-whitelist", "white",
        "-blacklist", "black", "-logLevel", "DEBUG", "-noserver", "-out",
        "myWww", "-gen", "myGen", "http://www.google.com/", "foo");

    assertNotNull(BrowserWidgetHostChecker.matchWhitelisted("white"));
    assertNotNull(BrowserWidgetHostChecker.matchBlacklisted("black"));
    assertFalse(BrowserWidgetHostChecker.isAlwaysWhitelisted("http://127.0.0.1.40"));
    assertFalse(BrowserWidgetHostChecker.isAlwaysWhitelisted("http://127.0.0.1.40:88"));
    assertFalse(BrowserWidgetHostChecker.isAlwaysWhitelisted("http://127.0.0.1.40:88/"));
    assertFalse(BrowserWidgetHostChecker.isAlwaysWhitelisted("http://127.0.0.1.40:88/foo"));
    assertFalse(BrowserWidgetHostChecker.isAlwaysWhitelisted("http://localhost.evildomain.org"));
    assertFalse(BrowserWidgetHostChecker.isAlwaysWhitelisted("http://localhost.evildomain.org:88"));
    assertFalse(BrowserWidgetHostChecker.isAlwaysWhitelisted("http://localhost.evildomain.org:88/"));
    assertFalse(BrowserWidgetHostChecker.isAlwaysWhitelisted("http://localhost.evildomain.org:88/foo"));
    assertFalse(BrowserWidgetHostChecker.isAlwaysWhitelisted("http://localhost.evildomain.org/"));
    assertFalse(BrowserWidgetHostChecker.isAlwaysWhitelisted("http://localhost.evildomain.org/foo"));
    assertFalse(BrowserWidgetHostChecker.isAlwaysWhitelisted("http://www.evildomain.org/foo?http://localhost"));
    assertTrue(BrowserWidgetHostChecker.isAlwaysWhitelisted("http://127.0.0.1"));
    assertTrue(BrowserWidgetHostChecker.isAlwaysWhitelisted("http://127.0.0.1:88"));
    assertTrue(BrowserWidgetHostChecker.isAlwaysWhitelisted("http://127.0.0.1:88/"));
    assertTrue(BrowserWidgetHostChecker.isAlwaysWhitelisted("http://127.0.0.1:88/foo"));

    assertEquals(new File("myGen").getAbsoluteFile(),
        options.getGenDir().getAbsoluteFile());
    assertEquals(new File("myWww"), options.getOutDir());

    assertEquals(TreeLogger.DEBUG, options.getLogLevel());

    assertEquals(8080, options.getPort());
    assertTrue(options.isNoServer());
    assertEquals(2, options.getStartupURLs().size());
    assertEquals("http://www.google.com/", options.getStartupURLs().get(0));
    assertEquals("foo", options.getStartupURLs().get(1));
  }

  public void testDefaultArgs() {
    assertProcessSuccess(argProcessor);

    assertEquals(null, options.getGenDir());
    assertEquals(new File("").getAbsoluteFile(),
        options.getOutDir().getAbsoluteFile());

    assertEquals(TreeLogger.INFO, options.getLogLevel());
    assertEquals(JsOutputOption.OBFUSCATED, options.getOutput());
    assertFalse(options.isEnableAssertions());
    assertTrue(options.isAggressivelyOptimize());

    assertEquals(8888, options.getPort());
    assertFalse(options.isNoServer());
    assertEquals(0, options.getStartupURLs().size());
  }

  public void testForbiddenArgs() {
    assertProcessFailure(argProcessor, "-localWorkers", "2");
    assertProcessFailure(argProcessor, "-extra", "extra");
    assertProcessFailure(argProcessor, "-war", "war");
    assertProcessFailure(argProcessor, "-work", "work");
    assertProcessFailure(argProcessor, "-server", MySCL.class.getName());
  }
}
