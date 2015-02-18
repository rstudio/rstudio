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

import com.google.gwt.thirdparty.common.css.compiler.ast.CssConditionalBlockNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssTree;
import com.google.gwt.thirdparty.common.css.compiler.ast.ErrorManager;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateConditionalNodes;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

/**
 * Test class for {@link ExtendedEliminateConditionalNodes}.
 */
public class ExtendedEliminateConditionalNodesTest extends BaseGssTest {
  private Set<CssConditionalBlockNode> cssRuntimeConditionalBlockNodes;

  public void testCompileTimeConditional() {
    // given
    CssTree cssTree = parseAndBuildTree(lines(
        "@if (is('custom.one', 'foo') && is('custom.one', 'bar')) {",
        "  .foo {",
        "    padding: 5px;",
        " }",
        "}",
        "",
        "@elseif (!is('custom.two', 'foo') && is('custom.three', 'foo') " +
            "|| is('custom.four', 'foo')) {",
        "  .foo {",
        "    padding: 15px;",
        " }",
        "}"));

    Set<String> trueConditions = Sets.newHashSet("custom.one:foo", "custom.three:foo");

    ExtendedEliminateConditionalNodes visitor =
        new ExtendedEliminateConditionalNodes(cssTree.getMutatingVisitController(),
            trueConditions, cssRuntimeConditionalBlockNodes);

    // when
    visitor.runPass();

    // then
    String expectedTreeToString = "[[.foo]{[padding:[15px]]}]";

    assertEquals(expectedTreeToString, cssTree.getRoot().getBody().toString());
  }

  public void testIgnoreRuntimeConditional() {
    // given
    CssTree cssTree = parseAndBuildTree(lines(
        "@if (eval('com.foo.BAR')) {",
        "  .foo {",
        "    padding: 5px;",
        "  }",
        "  @if (is('ie9')) {",
        "    .foo {",
        "      padding: 55px;",
        "    }",
        "  }",
        "}",
        "@elseif (eval('com.foo.bar()')) {",
        "  .foo {",
        "    padding: 15px;",
        "  }",
        "}"));

    Set<String> trueConditions = Sets.newHashSet("user.agent:ie6");
    ExtendedEliminateConditionalNodes visitor =
        new ExtendedEliminateConditionalNodes(cssTree.getMutatingVisitController(),
            trueConditions, cssRuntimeConditionalBlockNodes);

    // when
    visitor.runPass();

    // then
    // the  "  @if (is('ie9')) {", node is removed because it is evaluated to false
    String expectedTreeToString =
        "[[@if[Java expression : com.foo.BAR]{" +
            "[[.foo]{" +
            "[padding:[5px]]" +
            "}]}, " +
            "@elseif[Java expression : com.foo.bar()]{" +
            "[[.foo]{" +
            "[padding:[15px]]" +
            "}]" +
            "}]]";

    assertEquals(expectedTreeToString, cssTree.getRoot().getBody().toString());
  }

  public void testRemoveUnreachableRuntimeConditional() {
    // given
    CssTree cssTree = parseAndBuildTree(lines(
        "@if (is('foo', 'BAR')) {",
        "  .foo {",
        "    padding: 5px;",
        "  }",
        "}",
        "@elseif (eval('com.foo.bar()')) {",
        "  @if (eval('com.foo.FOO')) {",
        "    .foo {",
        "      padding: 15px;",
        "    }",
        "  }",
        "  @else{",
        "    .foo {",
        "      padding: 15px;",
        "    }",
        "  }",
        "}"));

    Set<String> trueConditions = Sets.newHashSet("foo:BAR");
    ExtendedEliminateConditionalNodes visitor =
        new ExtendedEliminateConditionalNodes(cssTree.getMutatingVisitController(),
            trueConditions, cssRuntimeConditionalBlockNodes);

    // when
    visitor.runPass();

    // then
    String expectedTreeToString = "[[.foo]{[padding:[5px]]}]";

    assertEquals(expectedTreeToString, cssTree.getRoot().getBody().toString());
  }

  @Override
  protected void runPassesOnNewTree(CssTree cssTree, ErrorManager errorManager) {
    new CreateConditionalNodes(cssTree.getMutatingVisitController(), errorManager).runPass();
    new CreateRuntimeConditionalNodes(cssTree.getMutatingVisitController()).runPass();
    new PermutationsCollector(cssTree.getMutatingVisitController()).runPass();
    RuntimeConditionalBlockCollector runtimeConditionalBlockCollector = new
        RuntimeConditionalBlockCollector(cssTree.getVisitController());
    runtimeConditionalBlockCollector.runPass();
    cssRuntimeConditionalBlockNodes = runtimeConditionalBlockCollector.getRuntimeConditionalBlock();
  }
}
