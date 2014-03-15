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

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.ServletContainerLauncher;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.DevMode.HostedModeOptionsImpl;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.shell.BrowserWidgetHostChecker;

import java.io.File;
import java.net.BindException;

/**
 * Test for {@link DevMode}.
 */
public class HostedModeTest extends ArgProcessorTestBase {

  static class MySCL extends ServletContainerLauncher {
    @Override
    public ServletContainer start(TreeLogger logger, int port, File appRootDir)
        throws BindException, Exception {
      throw new UnsupportedOperationException();
    }
  }

  private final DevMode.ArgProcessor argProcessor;
  private final HostedModeOptionsImpl options = new HostedModeOptionsImpl();

  public HostedModeTest() {
    argProcessor = new DevMode.ArgProcessor(options);
  }

  public void testAllValidArgs() {
    assertProcessSuccess(argProcessor, new String[] {"-port", "8080", "-whitelist", "white",
        "-blacklist", "black", "-logLevel", "DEBUG", "-noserver", "-server",
        MySCL.class.getName(), "-gen", "myGen", "-war", "myWar", "-workDir",
        "myWork", "-extra", "myExtra", "-startupUrl", "http://www.google.com/",
        "-startupUrl", "foo", "c.g.g.h.H", "my.Module"});

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
    assertEquals(new File("myWar"), options.getWarDir());
    assertEquals(new File("myWork"), options.getWorkDir());
    assertEquals(new File("myExtra"), options.getExtraDir());

    assertEquals(TreeLogger.DEBUG, options.getLogLevel());

    assertEquals(8080, options.getPort());
    // False because -server overrides -noserver.
    assertFalse(options.isNoServer());
    assertSame(MySCL.class, options.getServletContainerLauncher().getClass());

    assertEquals(2, options.getStartupURLs().size());
    assertEquals("http://www.google.com/", options.getStartupURLs().get(0));
    assertEquals("foo", options.getStartupURLs().get(1));

    assertEquals(2, options.getModuleNames().size());
    assertEquals("c.g.g.h.H", options.getModuleNames().get(0));
    assertEquals("my.Module", options.getModuleNames().get(1));
  }

  public void testNoServer() {
    assertProcessSuccess(argProcessor, new String[] {"-noserver", "c.g.g.h.H"});

    assertTrue(options.isNoServer());
    assertNull(options.getServletContainerLauncher());

    assertEquals(1, options.getModuleNames().size());
    assertEquals("c.g.g.h.H", options.getModuleNames().get(0));
  }

  public void testNoServerOverridesServer() {
    assertProcessSuccess(argProcessor,
        new String[] {"-server", MySCL.class.getName(), "-noserver", "c.g.g.h.H"});

    assertTrue(options.isNoServer());
    assertSame(MySCL.class, options.getServletContainerLauncher().getClass());

    assertEquals(1, options.getModuleNames().size());
    assertEquals("c.g.g.h.H", options.getModuleNames().get(0));
  }

  public void testDefaultArgs() {
    assertProcessSuccess(argProcessor, new String[] {"c.g.g.h.H"});

    assertEquals(null, options.getGenDir());
    assertEquals(new File("war").getAbsoluteFile(),
        options.getWarDir().getAbsoluteFile());
    assertEquals(null, options.getWorkDir());
    assertEquals(null, options.getExtraDir());

    assertEquals(TreeLogger.INFO, options.getLogLevel());
    assertEquals(JsOutputOption.OBFUSCATED, options.getOutput());
    assertFalse(options.isEnableAssertions());
    assertTrue(options.shouldClusterSimilarFunctions());
    assertTrue(options.shouldInlineLiteralParameters());
    assertTrue(options.shouldOptimizeDataflow());
    assertTrue(options.shouldOrdinalizeEnums());
    assertTrue(options.shouldRemoveDuplicateFunctions());

    assertEquals(8888, options.getPort());
    assertFalse(options.isNoServer());
    assertNotNull(options.getServletContainerLauncher());

    assertEquals(0, options.getStartupURLs().size());

    assertEquals(1, options.getModuleNames().size());
    assertEquals("c.g.g.h.H", options.getModuleNames().get(0));
  }

  public void testForbiddenArgs() {
    assertProcessFailure(argProcessor, "Unknown argument", new String[] {"-out", "www"});
  }
}
