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
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.StaticJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.thirdparty.guava.common.base.Joiner;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

/**
 * A base type for testing the code checkers.
 */
public abstract class CheckerTestCase extends TestCase {

  /**
   * A warning or error message.
   */
  protected static class Message {
    private int line;
    private Type logLevel;
    private String message;

    private Message(int line, Type logLevel, String message) {
      this.line = line;
      this.logLevel = logLevel;
      this.message = message;
    }
  }

  protected Message warning(int line, String message) {
    return new Message(line, Type.WARN, message);
  }

  protected Message error(int line, String message) {
    return new Message(line, Type.ERROR, message);
  }

  protected void shouldGenerate(MockJavaResource buggyCode, MockJavaResource extraCode,
      Type logLevel, Message... messages) {
    UnitTestTreeLogger.Builder b = new UnitTestTreeLogger.Builder();
    b.setLowestLogLevel(logLevel);
    if (messages.length != 0) {
      b.expect(logLevel, (logLevel == Type.WARN ? "Warnings" : "Errors") +
          " in '" + buggyCode.getLocation() + "'", null);
    }
    for (Message message : messages) {
      final String fullMessage = "Line " + message.line + ": " + message.message;
      b.expect(message.logLevel, fullMessage, null);
    }
    UnitTestTreeLogger logger = b.createLogger();
    TypeOracle oracle = buildOracle(logger, buggyCode, extraCode);

    logger.assertCorrectLogEntries();
    String className = buggyCode.getTypeName();
    if (messages.length != 0 && logLevel == TreeLogger.ERROR) {
      assertNull("Buggy compilation unit not removed from type oracle",
          oracle.findType(className));
    } else {
      assertNotNull("Buggy compilation unit removed with only a warning",
          oracle.findType(className));
    }
  }

  protected void shouldGenerateError(MockJavaResource buggyCode, MockJavaResource extraCode,
      int line, String message) {
    shouldGenerate(buggyCode, extraCode, TreeLogger.ERROR, error(line, message));
  }

  protected void shouldGenerateError(CharSequence buggyCode, CharSequence extraCode, int line,
      String message) {
    StaticJavaResource codeResource = new StaticJavaResource("Buggy", buggyCode);
    StaticJavaResource extraResource = new StaticJavaResource("Extra", extraCode);
    shouldGenerate(codeResource, extraResource, TreeLogger.ERROR,  error(line, message));
  }

  protected void shouldGenerateError(MockJavaResource buggyCode, int line,  String message) {
    shouldGenerateError(buggyCode, null, line, message);
  }

  protected void shouldGenerateNoError(MockJavaResource code) {
    shouldGenerateNoError(code, null);
  }

  protected void shouldGenerateNoError(MockJavaResource code, MockJavaResource extraCode) {
    shouldGenerate(code, extraCode, TreeLogger.ERROR);
  }

  protected void shouldGenerateNoError(CharSequence code, CharSequence extraCode) {
    StaticJavaResource codeResource = new StaticJavaResource("Buggy", code);
    StaticJavaResource extraResource = new StaticJavaResource("Extra", extraCode);
    shouldGenerate(codeResource, extraResource, TreeLogger.ERROR);
  }

  protected void shouldGenerateNoWarning(MockJavaResource code) {
    shouldGenerateNoWarning(code, null);
  }

  protected void shouldGenerateNoWarning(MockJavaResource code, MockJavaResource extraCode) {
    shouldGenerate(code, extraCode, TreeLogger.WARN);
  }

  protected void shouldGenerateWarning(MockJavaResource buggyCode,
      MockJavaResource extraCode, int line, String message) {
    shouldGenerate(buggyCode, extraCode, TreeLogger.WARN,  warning(line, message));
  }

  protected void shouldGenerateWarning(MockJavaResource buggyCode, int line,
      String message) {
    shouldGenerateWarning(buggyCode, null, line, message);
  }

  protected void shouldGenerateWarnings(MockJavaResource buggyCode, Message... messages) {
    shouldGenerateWarnings(buggyCode, null, messages);
  }

  protected void shouldGenerateWarnings(MockJavaResource buggyCode, MockJavaResource extraCode,
      Message... messages) {
    shouldGenerate(buggyCode, extraCode, TreeLogger.WARN, messages);
  }
  private String buggyPackage = "";

  private void addLongCheckingCups(Set<Resource> resources) {
    String code = Joiner.on('\n').join(
      "package com.google.gwt.core.client;",
      "public @interface UnsafeNativeLong {",
      "}");
    resources.add(new StaticJavaResource(
        "com.google.gwt.core.client.UnsafeNativeLong", code.toString()));
  }

  private TypeOracle buildOracle(UnitTestTreeLogger logger, MockJavaResource buggyResource,
      MockJavaResource extraResource) {
    Set<Resource> resources = new HashSet<Resource>();
    addLongCheckingCups(resources);
    Set<GeneratedUnit> generatedUnits = CompilationStateTestBase.getGeneratedUnits(buggyResource);
    if (extraResource != null) {
      generatedUnits.addAll(CompilationStateTestBase.getGeneratedUnits(extraResource));
    }
    return TypeOracleTestingUtils.buildStandardTypeOracleWith(logger,
        resources, generatedUnits);
  }

}
