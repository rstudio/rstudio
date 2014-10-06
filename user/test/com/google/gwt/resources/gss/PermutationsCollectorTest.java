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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.gwt.thirdparty.common.css.compiler.ast.CssTree;
import com.google.gwt.thirdparty.common.css.compiler.ast.ErrorManager;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssError;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateConditionalNodes;

import java.util.List;

/**
 * Test class for {@link PermutationsCollector}.
 */
public class PermutationsCollectorTest extends BaseGssTest {

  public void testPermutationCollector() {
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

    ErrorManager errorManager = mock(ErrorManager.class);
    PermutationsCollector visitor = new PermutationsCollector(cssTree.getMutatingVisitController(),
        errorManager);

    // when
    visitor.runPass();

    // then
    verify(errorManager, never()).report(any(GssError.class));

    List<String> permutationAxis = visitor.getPermutationAxes();
    assertEquals(4, permutationAxis.size());
    assertTrue(permutationAxis.contains("custom.one"));
    assertTrue(permutationAxis.contains("custom.two"));
    assertTrue(permutationAxis.contains("custom.three"));
    assertTrue(permutationAxis.contains("custom.four"));
  }

  public void testUserAgentShortcut() {
    CssTree cssTree = parseAndBuildTree(lines(
        "@if (is('custom.one', 'foo') && is('ie6')) {",
        "  .foo {",
        "    padding: 5px;",
        " }",
        "}"));

    ErrorManager errorManager = mock(ErrorManager.class);
    PermutationsCollector visitor = new PermutationsCollector(cssTree.getMutatingVisitController(),
        errorManager);

    // when
    visitor.runPass();

    // then
    verify(errorManager, never()).report(any(GssError.class));

    List<String> permutationAxis = visitor.getPermutationAxes();
    assertEquals(2, permutationAxis.size());
    assertTrue(permutationAxis.contains("custom.one"));
    assertTrue(permutationAxis.contains("user.agent"));
  }

  public void testRuntimeConditionAreIgnored() {
    CssTree cssTree = parseAndBuildTree(lines(
        "@if (eval('com.foo.BAR')) {",
        "  .foo {",
        "    padding: 5px;",
        "  }",
        "}",
        "@elseif (eval('com.foo.bar()')) {",
        "  @if (is('custom.one', 'foo')) {",
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

    ErrorManager errorManager = mock(ErrorManager.class);
    PermutationsCollector visitor = new PermutationsCollector(cssTree.getMutatingVisitController(),
        errorManager);

    // when
    visitor.runPass();

    // then
    verify(errorManager, never()).report(any(GssError.class));

    List<String> permutationAxis = visitor.getPermutationAxes();
    assertEquals(1, permutationAxis.size());
    assertTrue(permutationAxis.contains("custom.one"));
  }

  public void testInvalidConditionThrowsAnError() {
    CssTree cssTree = parseAndBuildTree(lines(
        "@if (evaluate('com.foo.BAR')) {",
        "  .foo {",
        "    padding: 5px;",
        "  }",
        "}"
    ));

    ErrorManager errorManager = mock(ErrorManager.class);
    PermutationsCollector visitor = new PermutationsCollector(cssTree.getMutatingVisitController(),
        errorManager);

    // when
    visitor.runPass();

    // then
    verify(errorManager).report(any(GssError.class));
    assertEquals(0, visitor.getPermutationAxes().size());
  }

  @Override
  protected void runPassesOnNewTree(CssTree cssTree, ErrorManager errorManager) {
    new CreateConditionalNodes(cssTree.getMutatingVisitController(), errorManager).runPass();
    new CreateRuntimeConditionalNodes(cssTree.getMutatingVisitController()).runPass();
  }
}
