/*
 * Copyright 2014 Google Inc.
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

package com.google.gwt.resources.gss;

import com.google.gwt.thirdparty.common.css.compiler.ast.CssTree;
import com.google.gwt.thirdparty.common.css.compiler.ast.ErrorManager;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateConditionalNodes;

import java.util.Set;

/**
 * Test class for {@link com.google.gwt.resources.gss.BooleanConditionCollector}.
 */
public class BooleanConditionCollectorTest extends BaseGssTest {

  public void testBooleanConditionCollector() {
    // given
    CssTree cssTree = parseAndBuildTree(lines(
        "@if (FOO && BAR) {",
        "  .foo {",
        "    padding: 5px;",
        " }",
        "}",
        "",
        "@elseif (!FOO || !BAR) {",
        "  .foo {",
        "    padding: 15px;",
        " }",
        "}"));

    BooleanConditionCollector visitor = new BooleanConditionCollector(
        cssTree.getMutatingVisitController());

    // when
    visitor.runPass();

    // then

    Set<String> booleanConditions = visitor.getBooleanConditions();
    assertEquals(2, booleanConditions.size());
    assertTrue(booleanConditions.contains("FOO"));
    assertTrue(booleanConditions.contains("BAR"));
  }

  public void testBooleanConditionCollectorWithIsFunction() {
    // given
    CssTree cssTree = parseAndBuildTree(lines(
        "@if (FOO && BAR && is('ie8') || is('locale', 'en')) {",
        "  .foo {",
        "    padding: 5px;",
        " }",
        "}",
        "",
        "@elseif (is(\"custom\", \"foo\")) {",
        "  .foo {",
        "    padding: 15px;",
        " }",
        "}",
        "@elseif (BAZ) {",
        "  .foo {",
        "    padding: 25px;",
        " }",
        "}"));

    BooleanConditionCollector visitor = new BooleanConditionCollector(
        cssTree.getMutatingVisitController());

    // when
    visitor.runPass();

    // then

    Set<String> booleanConditions = visitor.getBooleanConditions();
    assertEquals(3, booleanConditions.size());
    assertTrue(booleanConditions.contains("FOO"));
    assertTrue(booleanConditions.contains("BAR"));
    assertTrue(booleanConditions.contains("BAZ"));
  }

  public void testBooleanConditionCollectorWithEvalFunction() {
    // given
    CssTree cssTree = parseAndBuildTree(lines(
        "@if (eval(\"com.google.gwt.resources.client.gss.BooleanEval.FIRST\")) {",
        "  .foo {",
        "    padding: 5px;",
        " }",
        "}",
        "",
        "@elseif (FOO) {",
        "  .foo {",
        "    padding: 15px;",
        " }",
        "}"));

    BooleanConditionCollector visitor = new BooleanConditionCollector(
        cssTree.getMutatingVisitController());

    // when
    visitor.runPass();

    // then

    Set<String> booleanConditions = visitor.getBooleanConditions();
    assertEquals(1, booleanConditions.size());
    assertTrue(booleanConditions.contains("FOO"));
  }

  @Override
  protected void runPassesOnNewTree(CssTree cssTree, ErrorManager errorManager) {
    new CreateConditionalNodes(cssTree.getMutatingVisitController(), errorManager).runPass();
    new CreateRuntimeConditionalNodes(cssTree.getMutatingVisitController()).runPass();
  }
}
