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

package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

/**
 * Test the -strict option to the GWT compiler.
 */
public class StrictModeTest extends TestCase {
  /**
   * Module name for a module with a bad source file.
   */
  private static final String BAD = "com.google.gwt.dev.strict.bad.Bad";

  /**
   * Module name for a module with no bad source files.
   */
  private static final String GOOD = "com.google.gwt.dev.strict.good.Good";

  private JJSOptions options = new CompilerOptionsImpl();

  protected final CompilerContext.Builder compilerContextBuilder = new CompilerContext.Builder();

  private CompilerContext compilerContext = new CompilerContext();

  public StrictModeTest() {
    compilerContext = compilerContextBuilder.minimalRebuildCache(new MinimalRebuildCache())
        .build();
  }

  /**
   * A normal compile with a bad file should still succeed.
   */
  public void testBadCompile() throws UnableToCompleteException {
    precompile(TreeLogger.NULL, BAD);
  }

  /**
   * A bad compile in strict mode.
   */
  public void testBadCompileStrict() {
    options.setStrict(true);
    compilerContext =
        compilerContextBuilder.options(new PrecompileTaskOptionsImpl(options)).build();
    try {
      precompile(TreeLogger.NULL, BAD);
      fail("Should have failed");
    } catch (UnableToCompleteException expected) {
    }
  }

  /**
   * A normal compile with a bad file should still succeed.
   */
  public void testBadValidate() {
    assertTrue(validate(TreeLogger.NULL, BAD));
  }

  /**
   * A bad compile in strict mode.
   */
  public void testBadValidateStrict() {
    options.setStrict(true);
    compilerContext =
        compilerContextBuilder.options(new PrecompileTaskOptionsImpl(options)).build();
    assertFalse(validate(TreeLogger.NULL, BAD));
  }

  /**
   * Test a plain old successful compile.
   */
  public void testGoodCompile() throws UnableToCompleteException {
    precompile(getErrorLoggingTreeLogger(), GOOD);
  }

  /**
   * A good compile in strict mode.
   */
  public void testGoodCompileStrict() throws UnableToCompleteException {
    options.setStrict(true);
    compilerContext =
        compilerContextBuilder.options(new PrecompileTaskOptionsImpl(options)).build();
    precompile(getErrorLoggingTreeLogger(), GOOD);
  }

  /**
   * Test a plain old successful validate.
   */
  public void testGoodValidate() {
    assertTrue(validate(getErrorLoggingTreeLogger(), GOOD));
  }

  /**
   * A good compile in strict mode.
   */
  public void testGoodValidateStrict() {
    options.setStrict(true);
    compilerContext =
        compilerContextBuilder.options(new PrecompileTaskOptionsImpl(options)).build();
    assertTrue(validate(getErrorLoggingTreeLogger(), GOOD));
  }

  private void precompile(TreeLogger logger, String moduleName) throws UnableToCompleteException {
    ModuleDef module = ModuleDefLoader.loadFromClassPath(logger, moduleName);
    compilerContext = compilerContextBuilder.module(module).build();
    if (Precompile.precompile(logger, compilerContext) == null) {
      throw new UnableToCompleteException();
    }
  }

  private boolean validate(TreeLogger logger, String moduleName) {
    ModuleDef module;
    try {
      module = ModuleDefLoader.loadFromClassPath(logger, moduleName);
      compilerContext = compilerContextBuilder.module(module).build();
    } catch (UnableToCompleteException e) {
      fail("Failed to load the module definition");
      return false;
    }
    return Precompile.validate(logger, compilerContext);
  }

  private TreeLogger getErrorLoggingTreeLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
    logger.setMaxDetail(Type.ERROR);
    return logger;
  }
}
