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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.gwt.thirdparty.common.css.compiler.ast.CssTree;
import com.google.gwt.thirdparty.common.css.compiler.ast.ErrorManager;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssError;
import com.google.gwt.thirdparty.common.css.compiler.ast.MutatingVisitController;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateConditionalNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateDefinitionNodes;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import org.mockito.verification.VerificationMode;

/**
 * Test class for {@link ValidateRuntimeConditionalNode}.
 */
public class ValidateRuntimeConditionalNodeTest extends BaseGssTest {

  public void testValidRuntimeConditional() {
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
        "}",
        "@if (is('ie6')) {",
        "  .foo {",
        "    padding: 25px;",
        "  }",
        "}",
        "@elseif (eval('com.foo.BAR')) {",
        "  .foo {",
        "    padding: 35px;",
        "  }",
        "}"));

    ErrorManager errorManager = mock(ErrorManager.class);
    boolean lenient = false;
    ValidateRuntimeConditionalNode visitor = new ValidateRuntimeConditionalNode(
        cssTree.getMutatingVisitController(), errorManager, lenient);

    // when
    visitor.runPass();

    // then
    verify(errorManager, never()).report(any(GssError.class));
    verify(errorManager, never()).reportWarning(any(GssError.class));
  }

  public void testInvalidRuntimeConditionalWithExternalAtRuleLenient() {
   testInvalidRuntimeConditionalWithExternal(true);
  }

  public void testInvalidRuntimeConditionalWithExternalAtRuleNotLenient() {
    testInvalidRuntimeConditionalWithExternal(false);
  }

  private void testInvalidRuntimeConditionalWithExternal(boolean lenient) {
    // given
    CssTree cssTree = parseAndBuildTree(lines(
        "@if (eval('com.foo.BAR')) {",
        "  @external foo;",
        "}"));

    ErrorManager errorManager = mock(ErrorManager.class);
    ValidateRuntimeConditionalNode visitor = new ValidateRuntimeConditionalNode(
        cssTree.getMutatingVisitController(), errorManager, lenient);

    // when
    visitor.runPass();

    // then
    VerificationMode error = lenient ? never() : times(1);
    VerificationMode warning = lenient ? times(1) : never();

    verify(errorManager, error).report(any(GssError.class));
    verify(errorManager, warning).reportWarning(any(GssError.class));
  }

  public void testInvalidRuntimeConditionalWithConstantDefLenient() {
    testInvalidRuntimeConditionalWithConstantDef(true);
  }

  public void testInvalidRuntimeConditionalWithConstantDefNotLenient() {
    testInvalidRuntimeConditionalWithConstantDef(false);
  }

  private void testInvalidRuntimeConditionalWithConstantDef(boolean lenient) {
    // given
    CssTree cssTree = parseAndBuildTree(lines(
        "@if (eval('com.foo.BAR')) {",
        "  @def FOO 5px;",
        "}"));

    ErrorManager errorManager = mock(ErrorManager.class);
    ValidateRuntimeConditionalNode visitor = new ValidateRuntimeConditionalNode(
        cssTree.getMutatingVisitController(), errorManager, lenient);

    // when
    visitor.runPass();

    // then
    VerificationMode error = lenient ? never() : times(1);
    VerificationMode warning = lenient ? times(1) : never();

    verify(errorManager, error).report(any(GssError.class));
    verify(errorManager, warning).reportWarning(any(GssError.class));
  }

  public void testValidCompileTimeConditionalWithConstantDef() {
    // given
    CssTree cssTree = parseAndBuildTree(lines(
        "@if (is('foo', 'bar')) {",
        "  @def FOO 5px;",
        "}"));

    ErrorManager errorManager = mock(ErrorManager.class);
    ValidateRuntimeConditionalNode visitor = new ValidateRuntimeConditionalNode(
        cssTree.getMutatingVisitController(), errorManager, false);

    // when
    visitor.runPass();

    // then
    verify(errorManager, never()).report(any(GssError.class));
    verify(errorManager, never()).reportWarning(any(GssError.class));
  }

  public void testValidCompileTimeConditionalWithExternal() {
    // given
    CssTree cssTree = parseAndBuildTree(lines(
        "@if (is('foo', 'bar')) {",
        "  @external bar;",
        "}"));

    ErrorManager errorManager = mock(ErrorManager.class);
    ValidateRuntimeConditionalNode visitor = new ValidateRuntimeConditionalNode(
        cssTree.getMutatingVisitController(), errorManager, false);

    // when
    visitor.runPass();

    // then
    verify(errorManager, never()).report(any(GssError.class));
    verify(errorManager, never()).reportWarning(any(GssError.class));
  }

  @Override
  protected void runPassesOnNewTree(CssTree cssTree, ErrorManager errorManager) {
    MutatingVisitController mutatingVisitController = cssTree.getMutatingVisitController();
    new CreateDefinitionNodes(mutatingVisitController, errorManager).runPass();
    new CreateConditionalNodes(mutatingVisitController, errorManager).runPass();
    new CreateRuntimeConditionalNodes(mutatingVisitController).runPass();

    new PermutationsCollector(mutatingVisitController).runPass();
    RuntimeConditionalBlockCollector runtimeConditionalBlockCollector = new
        RuntimeConditionalBlockCollector(mutatingVisitController);
    runtimeConditionalBlockCollector.runPass();

    new ExtendedEliminateConditionalNodes(mutatingVisitController, Sets.newHashSet("foo:bar"),
        runtimeConditionalBlockCollector.getRuntimeConditionalBlock()).runPass();
  }
}
