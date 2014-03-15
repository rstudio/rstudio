/*
 * Copyright 2013 Google Inc.
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

import com.google.gwt.dev.javac.testing.impl.Java7MockResources;
import com.google.gwt.dev.util.arg.SourceLevel;


/**
 * Test class for language features introduced in Java 7.
 *
 * <p>Only tests that the JDT accepts and compiles the new syntax..
 */
public class Java7JdtCompilerTest extends JdtCompilerTestBase {

  public void testCompileNewStyleLiterals() throws Exception {
    assertResourcesCompileSuccessfully(Java7MockResources.LIST_T, Java7MockResources.ARRAYLIST_T,
        Java7MockResources.NEW_INTEGER_LITERALS_TEST);
  }

  public void testCompileSwitchWithStrings() throws Exception {
    assertResourcesCompileSuccessfully(Java7MockResources.LIST_T, Java7MockResources.ARRAYLIST_T,
        Java7MockResources.SWITCH_ON_STRINGS_TEST);
  }

  public void testCompileDiamondOperator() throws Exception {
    assertResourcesCompileSuccessfully(Java7MockResources.LIST_T, Java7MockResources.ARRAYLIST_T,
        Java7MockResources.DIAMOND_OPERATOR_TEST);
  }

  public void testCompileTryWithResources() throws Exception {
    assertResourcesCompileSuccessfully(Java7MockResources.TEST_RESOURCE,
        Java7MockResources.TRY_WITH_RESOURCES_TEST);
  }

  public void testCompileMultiExceptions() throws Exception {
    assertResourcesCompileSuccessfully(Java7MockResources.EXCEPTION1, Java7MockResources.EXCEPTION2,
        Java7MockResources.MULTI_EXCEPTION_TEST);
  }

  @Override
  protected SourceLevel getSourceLevel() {
    // Always compile this tests as Java 7.
    return SourceLevel.JAVA7;
  }
}
