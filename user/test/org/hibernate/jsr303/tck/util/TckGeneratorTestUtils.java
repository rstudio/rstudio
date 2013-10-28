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
package org.hibernate.jsr303.tck.util;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.shell.FailErrorLogger;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.dev.util.log.CompositeTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.io.File;

import javax.validation.ValidationException;

/**
 * Static utilities useful for testing TCK Generators.
 */
public class TckGeneratorTestUtils {

  private final static boolean LOG_TO_CONSOLE = false;

  static TreeLogger createFailOnErrorLogger() {
    TreeLogger logger = LOG_TO_CONSOLE ? new CompositeTreeLogger(
        new PrintWriterTreeLogger(), new FailErrorLogger())
        : new FailErrorLogger();
    return logger;
  }

  static StandardGeneratorContext createGeneratorContext(String moduleName,
      TreeLogger logger) throws UnableToCompleteException {
    CompilerContext.Builder compilerContextBuilder = new CompilerContext.Builder();
    CompilerContext compilerContext = compilerContextBuilder.build();
    ModuleDef module = ModuleDefLoader.loadFromClassPath(logger, compilerContext, moduleName);
    compilerContext = compilerContextBuilder.module(module).build();
    compilerContext.getOptions().setGenDir(new File(System.getProperty("java.io.tmpdir")));

    ArtifactSet allGenreatedArtifacts = new ArtifactSet();
    boolean isProd = false;
    StandardGeneratorContext context = new StandardGeneratorContext(
        compilerContext, module.getCompilationState(logger, compilerContext), allGenreatedArtifacts,
        isProd);
    return context;
  }

  static UnitTestTreeLogger createTestLogger(
      final Class<? extends ValidationException> expectedException,
      final String expectedMessage) {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.expect(TreeLogger.ERROR, expectedMessage, expectedException);
    builder.setLowestLogLevel(TreeLogger.INFO);
    UnitTestTreeLogger testLogger = builder.createLogger();
    return testLogger;
  }

  /**
   * Creates the fully qualified GWT module name using the package of
   * {@code clazz} and the {@code simpleName}
   *
   * @param clazz the class whose package the module is in.
   * @param simpleName the module name.
   * @return the fully qualified module name.
   */
  static String getFullyQualifiedModuleName(Class<?> clazz, String simpleName) {
    return clazz.getPackage().getName() + "." + simpleName;
  }
}
