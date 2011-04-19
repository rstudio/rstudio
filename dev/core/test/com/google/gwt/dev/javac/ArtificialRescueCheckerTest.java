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

import com.google.gwt.core.client.impl.ArtificialRescue;

/**
 * Tests for the ArtificialRescueChecker.
 */
public class ArtificialRescueCheckerTest extends CheckerTestCase {

  public void testBadClassName() {
    StringBuilder builder = builder();
    builder.append("@ArtificialRescue(@Rescue(className=\"Fail\"))\n");
    builder.append("class Buggy{}");
    shouldGenerateError(builder, targetClass(), 3,
        ArtificialRescueChecker.notFound("Fail"));
  }

  public void testBadFieldName() {
    StringBuilder builder = builder();
    builder.append("@ArtificialRescue(@Rescue(className=\"Target\", fields=\"foo\"))\n");
    builder.append("class Buggy{}");
    shouldGenerateError(builder, targetClass(), 3,
        ArtificialRescueChecker.unknownField("foo"));
  }

  public void testBadMembersOnArray() {
    StringBuilder builder = builder();
    builder.append("@ArtificialRescue(@Rescue(className=\"Target[]\", fields=\"foo\"))\n");
    builder.append("class Buggy{}");
    shouldGenerateError(builder, targetClass(), 3,
        ArtificialRescueChecker.noFieldsAllowed());
  }

  public void testBadMethodName() {
    StringBuilder builder = builder();
    builder.append("@ArtificialRescue(@Rescue(className=\"Target\", methods=\"blah()\"))\n");
    builder.append("class Buggy{}");
    shouldGenerateError(builder, targetClass(), 3,
        ArtificialRescueChecker.noMethod("Target", "blah()"));
  }

  public void testBadMethodSignature() {
    StringBuilder builder = builder();
    builder.append("@ArtificialRescue(@Rescue(className=\"Target\", methods=\"blah()()\"))\n");
    builder.append("class Buggy{}");
    shouldGenerateError(builder, targetClass(), 3,
        ArtificialRescueChecker.badMethodSignature("blah()()"));
  }

  public void testBadMethodSignatureFullyQualified() {
    StringBuilder builder = builder();
    builder.append("@ArtificialRescue(@Rescue(className=\"Target\", methods=\"@Target::blah()\"))\n");
    builder.append("class Buggy{}");
    shouldGenerateError(builder, targetClass(), 3,
        ArtificialRescueChecker.nameAndTypesOnly());
  }

  public void testOkArray() {
    StringBuilder builder = builder();
    builder.append("@ArtificialRescue(@Rescue(className=\"Target[]\"))\n");
    builder.append("class Buggy{}");
    shouldGenerateNoError(builder, targetClass());
  }

  public void testOkConstructor() {
    StringBuilder builder = builder();
    builder.append("@ArtificialRescue(@Rescue(className=\"Target\", methods=\"Target()\"))\n");
    builder.append("class Buggy{}");
    shouldGenerateNoError(builder, targetClass());
  }

  public void testOkInner() {
    StringBuilder builder = builder();
    builder.append("@ArtificialRescue(@Rescue(className=\"Target.Inner\", methods=\"Target$Inner()\"))\n");
    builder.append("class Buggy{}");
    shouldGenerateNoError(builder, targetClass());
  }

  public void testOkOneField() {
    StringBuilder builder = builder();
    builder.append("@ArtificialRescue(@Rescue(className=\"Target\", fields=\"i\"))\n");
    builder.append("class Buggy{}");
    shouldGenerateNoError(builder, targetClass());
  }

  public void testOkOneMethod() {
    StringBuilder builder = builder();
    builder.append("@ArtificialRescue(@Rescue(className=\"Target\", methods=\"getI()\"))\n");
    builder.append("class Buggy{}");
    shouldGenerateNoError(builder, targetClass());
  }

  public void testOkPrimitiveArray() {
    StringBuilder builder = builder();
    builder.append("@ArtificialRescue(@Rescue(className=\"boolean[]\"))\n");
    builder.append("class Buggy{}");
    shouldGenerateNoError(builder, targetClass());
  }

  public void testOkTwoFields() {
    StringBuilder builder = builder();
    builder.append("@ArtificialRescue(@Rescue(className=\"Target\", fields={\"i\", \"str\"}))\n");
    builder.append("class Buggy{}");
    shouldGenerateNoError(builder, targetClass());
  }

  public void testOkTwoMethods() {
    StringBuilder builder = builder();
    builder.append("@ArtificialRescue(@Rescue(className=\"Target\", methods={\"getI()\", \"getI(Z)\"}))\n");
    builder.append("class Buggy{}");
    shouldGenerateNoError(builder, targetClass());
  }

  private StringBuilder builder() {
    StringBuilder code = new StringBuilder();
    code.append("import " + ArtificialRescue.class.getCanonicalName() + ";\n");
    code.append("import " + ArtificialRescue.Rescue.class.getCanonicalName()
        + ";\n");
    return code;
  }

  private StringBuilder targetClass() {
    StringBuilder targetClass = new StringBuilder();
    targetClass = new StringBuilder();
    targetClass.append("class Target {\n");
    targetClass.append("  public class Inner{}\n");
    targetClass.append("  private String str;\n");
    targetClass.append("  private int i;\n");
    targetClass.append("  public int getI() { return i; }\n");
    targetClass.append("  public int getI(boolean override) {return override ? 0 : i; }\n");
    targetClass.append("  private String getStr() {return str;}");
    targetClass.append("}");
    return targetClass;
  }
}
