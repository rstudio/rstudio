package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.HostedMode.HostedModeOptionsImpl;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.shell.BrowserWidgetHostChecker;
import com.google.gwt.dev.shell.ServletContainer;
import com.google.gwt.dev.shell.ServletContainerLauncher;

import java.io.File;
import java.net.BindException;

/**
 * Test for {@link HostedMode}.
 */
public class HostedModeTest extends ArgProcessorTestBase {

  public static class MySCL implements ServletContainerLauncher {
    public ServletContainer start(TreeLogger logger, int port, File appRootDir)
        throws BindException, Exception {
      throw new UnsupportedOperationException();
    }
  }

  private final HostedMode.ArgProcessor argProcessor;
  private final HostedMode hostedMode;
  private final HostedModeOptionsImpl options;

  public HostedModeTest() {
    hostedMode = new HostedMode();
    options = hostedMode.options;
    argProcessor = hostedMode.new ArgProcessor();
  }

  public void testAllValidArgs() {
    assertProcessSuccess(argProcessor, "-port", "8080", "-whitelist", "white",
        "-blacklist", "black", "-logLevel", "DEBUG", "-style", "PRETTY", "-ea",
        "-XdisableAggressiveOptimization", "-noserver", "-server",
        MySCL.class.getName(), "-gen", "myGen", "-war", "myWar", "-workDir",
        "myWork", "-extra", "myExtra", "-localWorkers", "2", "-startupUrl",
        "http://www.google.com/", "-startupUrl", "foo", "c.g.g.h.H",
        "my.Module");

    assertNotNull(BrowserWidgetHostChecker.matchWhitelisted("white"));
    assertNotNull(BrowserWidgetHostChecker.matchBlacklisted("black"));

    assertEquals(new File("myGen").getAbsoluteFile(),
        options.getGenDir().getAbsoluteFile());
    assertEquals(new File("myWar"), options.getWarDir());
    assertEquals(new File("myWork"), options.getWorkDir());
    assertEquals(new File("myExtra"), options.getExtraDir());

    assertEquals(TreeLogger.DEBUG, options.getLogLevel());
    assertEquals(JsOutputOption.PRETTY, options.getOutput());
    assertTrue(options.isEnableAssertions());
    assertFalse(options.isAggressivelyOptimize());

    assertEquals(2, options.getLocalWorkers());

    assertEquals(8080, hostedMode.getPort());
    assertTrue(hostedMode.isRunTomcat());
    assertSame(MySCL.class, hostedMode.getServerContainerLauncher().getClass());

    assertEquals(2, hostedMode.getStartupURLs().size());
    assertEquals("http://www.google.com/", hostedMode.getStartupURLs().get(0));
    assertEquals("foo", hostedMode.getStartupURLs().get(1));

    assertEquals(2, options.getModuleNames().size());
    assertEquals("c.g.g.h.H", options.getModuleNames().get(0));
    assertEquals("my.Module", options.getModuleNames().get(1));
  }

  public void testDefaultArgs() {
    assertProcessSuccess(argProcessor, "c.g.g.h.H");

    assertEquals(null, options.getGenDir());
    assertEquals(new File("war").getAbsoluteFile(),
        options.getWarDir().getAbsoluteFile());
    assertEquals(null, options.getWorkDir());
    assertEquals(null, options.getExtraDir());

    assertEquals(TreeLogger.INFO, options.getLogLevel());
    assertEquals(JsOutputOption.OBFUSCATED, options.getOutput());
    assertFalse(options.isEnableAssertions());
    assertTrue(options.isAggressivelyOptimize());

    assertEquals(1, options.getLocalWorkers());

    assertEquals(8888, hostedMode.getPort());
    assertTrue(hostedMode.isRunTomcat());
    assertNotNull(hostedMode.getServerContainerLauncher());

    assertEquals(0, hostedMode.getStartupURLs().size());

    assertEquals(1, options.getModuleNames().size());
    assertEquals("c.g.g.h.H", options.getModuleNames().get(0));
  }

  public void testForbiddenArgs() {
    assertProcessFailure(argProcessor, "-out", "www");
  }
}
