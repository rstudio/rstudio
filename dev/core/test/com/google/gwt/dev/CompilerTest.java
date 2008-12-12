package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.Compiler.CompilerOptionsImpl;
import com.google.gwt.dev.jjs.JsOutputOption;

import java.io.File;

/**
 * Test for {@link Compiler}.
 */
public class CompilerTest extends ArgProcessorTestBase {

  private final Compiler.ArgProcessor argProcessor;
  private final CompilerOptionsImpl options = new CompilerOptionsImpl();

  public CompilerTest() {
    argProcessor = new Compiler.ArgProcessor(options);
  }

  public void testAllValidArgs() {
    assertProcessSuccess(argProcessor, "-logLevel", "DEBUG", "-style",
        "PRETTY", "-ea", "-XdisableAggressiveOptimization", "-gen", "myGen",
        "-war", "myWar", "-workDir", "myWork", "-extra", "myExtra",
        "-localWorkers", "2", "c.g.g.h.H", "my.Module");

    assertEquals(new File("myGen").getAbsoluteFile(),
        options.getGenDir().getAbsoluteFile());
    assertEquals(new File("myWar"), options.getWarDir());
    assertEquals(new File("myWork"), options.getWorkDir());
    assertEquals(new File("myExtra"), options.getExtraDir());

    assertEquals(2, options.getLocalWorkers());

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
    assertEquals(new File("war").getAbsoluteFile(),
        options.getWarDir().getAbsoluteFile());
    assertEquals(null, options.getWorkDir());
    assertEquals(null, options.getExtraDir());

    assertEquals(TreeLogger.INFO, options.getLogLevel());
    assertEquals(JsOutputOption.OBFUSCATED, options.getOutput());
    assertFalse(options.isEnableAssertions());
    assertTrue(options.isAggressivelyOptimize());

    assertEquals(1, options.getLocalWorkers());

    assertEquals(1, options.getModuleNames().size());
    assertEquals("c.g.g.h.H", options.getModuleNames().get(0));
  }

  public void testForbiddenArgs() {
    assertProcessFailure(argProcessor, "-out", "www");
  }
}
