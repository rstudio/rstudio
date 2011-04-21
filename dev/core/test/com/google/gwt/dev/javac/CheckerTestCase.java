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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.testing.impl.StaticJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.UnitTestTreeLogger;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

/**
 * A base type for testing the code checkers.
 */
public abstract class CheckerTestCase extends TestCase {

  protected void shouldGenerate(CharSequence buggyCode, CharSequence extraCode,
      int line, Type logLevel, String logHeader, String message) {
    UnitTestTreeLogger.Builder b = new UnitTestTreeLogger.Builder();
    b.setLowestLogLevel(logLevel);
    if (message != null) {
      b.expect(logLevel, logHeader + " in '/mock/Buggy.java'", null);
      final String fullMessage = "Line " + line + ": " + message;
      b.expect(logLevel, fullMessage, null);
    }
    UnitTestTreeLogger logger = b.createLogger();
    TypeOracle oracle = buildOracle(buggyCode, extraCode, logger);
    logger.assertCorrectLogEntries();
    if (message != null && logLevel == TreeLogger.ERROR) {
      assertNull("Buggy compilation unit not removed from type oracle",
          oracle.findType("Buggy"));
    } else {
      assertNotNull("Buggy compilation unit removed with only a warning",
          oracle.findType("Buggy"));
    }
  }

  protected void shouldGenerateError(CharSequence buggyCode,
      CharSequence extraCode, int line, String message) {
    shouldGenerate(buggyCode, extraCode, line, TreeLogger.ERROR, "Errors",
        message);
  }

  protected void shouldGenerateError(CharSequence buggyCode, int line,
      String message) {
    shouldGenerateError(buggyCode, null, line, message);
  }

  protected void shouldGenerateNoError(CharSequence code) {
    shouldGenerateNoError(code, null);
  }

  protected void shouldGenerateNoError(CharSequence code, CharSequence extraCode) {
    shouldGenerateError(code, extraCode, -1, null);
  }

  protected void shouldGenerateNoWarning(CharSequence code) {
    shouldGenerateWarning(code, -1, null);
  }

  protected void shouldGenerateWarning(CharSequence buggyCode,
      CharSequence extraCode, int line, String message) {
    shouldGenerate(buggyCode, extraCode, line, TreeLogger.WARN, "Warnings",
        message);
  }

  protected void shouldGenerateWarning(CharSequence buggyCode, int line,
      String message) {
    shouldGenerateWarning(buggyCode, null, line, message);
  }

  private void addLongCheckingCups(Set<Resource> resources) {
    StringBuilder code = new StringBuilder();
    code.append("package com.google.gwt.core.client;\n");
    code.append("public @interface UnsafeNativeLong {\n");
    code.append("}\n");
    resources.add(new StaticJavaResource(
        "com.google.gwt.core.client.UnsafeNativeLong", code.toString()));
  }

  private TypeOracle buildOracle(CharSequence buggyCode,
      CharSequence extraCode, UnitTestTreeLogger logger) {
    Set<Resource> resources = new HashSet<Resource>();
    addLongCheckingCups(resources);
    StaticJavaResource buggyResource = new StaticJavaResource("Buggy",
        buggyCode);
    Set<GeneratedUnit> generatedUnits = CompilationStateTestBase.getGeneratedUnits(buggyResource);
    if (extraCode != null) {
      StaticJavaResource extraResource = new StaticJavaResource("Extra",
          extraCode);
      generatedUnits.addAll(CompilationStateTestBase.getGeneratedUnits(extraResource));
    }
    return TypeOracleTestingUtils.buildStandardTypeOracleWith(logger,
        resources, generatedUnits);
  }
}
