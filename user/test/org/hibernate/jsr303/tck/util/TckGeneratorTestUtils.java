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
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.shell.FailErrorLogger;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.validation.rebind.ValidatorGenerator;

import junit.framework.Assert;

import java.io.File;

import javax.validation.Validator;

/**
 * Static utilities useful for testing TCK Generators.
 */
public class TckGeneratorTestUtils {

  /**
   * Asserts that calling
   * {@link ValidatorGenerator#generate(TreeLogger, com.google.gwt.core.ext.GeneratorContext, String)}
   * causes a {@link UnableToCompleteException} with exactly the log messages
   * specified in {@code testLogger}.
   *
   * @param testLogger test logger with expected log messages set.
   * @param fullyQaulifiedModuleName the gwt Module to load.
   * @param validatorClass the Validator to generate.
   * @throws UnableToCompleteException if The module or derived CompilationState
   *           can not be loaded.
   */
  public static void assertModuleFails(UnitTestTreeLogger testLogger,
      String fullyQaulifiedModuleName,
      Class<? extends Validator> validatorClass)
      throws UnableToCompleteException {
    TreeLogger logger = new FailErrorLogger();
    ModuleDef module = ModuleDefLoader.loadFromClassPath(logger,
        fullyQaulifiedModuleName);
    File genDir = new File(System.getProperty("java.io.tmpdir"));
    ArtifactSet allGenreatedArtifacts = new ArtifactSet();
    boolean isProd = false;
    StandardGeneratorContext context = new StandardGeneratorContext(
        module.getCompilationState(logger), module, genDir,
        allGenreatedArtifacts, isProd);

    ValidatorGenerator generator = new ValidatorGenerator();
    try {
      generator.generate(testLogger, context,
          validatorClass.getCanonicalName());
      Assert.fail("Expected a " + UnableToCompleteException.class);
    } catch (UnableToCompleteException expected) {
      // expected
    }
    testLogger.assertCorrectLogEntries();
  }

  /**
   * Creates the fully qualified GWT module name using the package of
   * {@code clazz} and the {@code moduleNam}
   *
   * @param clazz the class whose package the module is in.
   * @param moduleName the module name.
   * @return the fully qualified module name.
   */
  public static String getFullyQaulifiedModuleName(Class<?> clazz,
      String moduleName) {
    return clazz.getPackage().getName() + "." + moduleName;
  }
}
