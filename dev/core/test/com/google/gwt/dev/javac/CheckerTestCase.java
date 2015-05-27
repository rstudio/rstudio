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
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import junit.framework.TestCase;

import java.util.Set;

/**
 * A base type for testing the code checkers.
 */
public abstract class CheckerTestCase extends TestCase {

  /**
   * Pass represents passes to be run by the checker test cases.
   */
  public interface Pass {
    /**
     * Run the pass. Returns true if pass completes successfully.
     */
    boolean run(TreeLogger logger, MockJavaResource buggyResource, MockJavaResource extraResource);

    boolean classAvailable(String className);
    String getTopErrorMessage(Type logLevel, MockJavaResource resource);
  }
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

  /**
   * Provide a pass to be checked by default runs only passes required to build TypeOracle.
   */
  protected Pass providePass() {
    return new Pass() {
      private TypeOracle oracle = null;
      @Override
      public boolean run(TreeLogger logger, MockJavaResource buggyResource,
          MockJavaResource extraResource) {
        Set<Resource> resources = Sets.newHashSet();
        addLongCheckingCups(resources);
        Set<GeneratedUnit> generatedUnits =
            CompilationStateTestBase.getGeneratedUnits(buggyResource);
        if (extraResource != null) {
          generatedUnits.addAll(CompilationStateTestBase.getGeneratedUnits(extraResource));
        }
        CompilationState state  = TypeOracleTestingUtils.buildCompilationStateWith(logger,
            resources, generatedUnits);
        oracle = state.getTypeOracle();
        return !state.hasErrors();
      }

      @Override
      public boolean classAvailable(String className) {
        return oracle.findType(className) != null;
      }

      @Override
      public String getTopErrorMessage(Type logLevel, MockJavaResource resource) {
        return (logLevel == Type.WARN ? "Warnings" : "Errors") +
            " in '" + resource.getLocation() + "'";
      }
    };
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
    Pass pass = providePass();
    String topLevelMessage = pass.getTopErrorMessage(logLevel, buggyCode);
    if (messages.length != 0 && topLevelMessage != null) {
      b.expect(logLevel, topLevelMessage, null);
    }
    for (Message message : messages) {
      final String fullMessage = "Line " + message.line + ": " + message.message;
      b.expect(message.logLevel, fullMessage, null);
    }
    UnitTestTreeLogger logger = b.createLogger();

    boolean result = pass.run(logger, buggyCode, extraCode);

    logger.assertCorrectLogEntries();
    String className = buggyCode.getTypeName();
    if (messages.length != 0 && logLevel == TreeLogger.ERROR) {
      assertFalse("Compilation unit " + className + " not removed" +
              " but should have been removed.",
          pass.classAvailable(className));
    } else {
      assertTrue("Compilation unit " + className + " was removed but shouldnt have.",
          pass.classAvailable(className));
    }

    boolean expectingErrors = messages.length != 0 && logLevel == Type.ERROR;
    assertEquals(!expectingErrors, result);
  }

  protected void shouldGenerateError(MockJavaResource buggyCode, MockJavaResource extraCode,
      int line, String message) {
    shouldGenerate(buggyCode, extraCode, TreeLogger.ERROR, error(line, message));
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

  private void addLongCheckingCups(Set<Resource> resources) {
    String code = Joiner.on('\n').join(
      "package com.google.gwt.core.client;",
      "public @interface UnsafeNativeLong {",
      "}");
    resources.add(new StaticJavaResource(
        "com.google.gwt.core.client.UnsafeNativeLong", code.toString()));
  }
}
