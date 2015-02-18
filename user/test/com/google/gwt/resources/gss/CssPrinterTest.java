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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssTree;
import com.google.gwt.thirdparty.common.css.compiler.ast.ErrorManager;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssFunction;
import com.google.gwt.thirdparty.common.css.compiler.ast.MutatingVisitController;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateConditionalNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateStandardAtRuleNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.ResolveCustomFunctionNodes;

import java.util.HashSet;
import java.util.Map;

/**
 * Test for {@link CssPrinter}.
 */
public class CssPrinterTest extends BaseGssTest {
  public void testWrapCssInValidJavaString() {
    assertPrintedResult("(\".foo{width:15px}\")",
        lines(
            ".foo {",
            "    width:15px;",
            "}"
        ));
  }

  public void testDontPrintExternalAtRule() {
    assertPrintedResult("(\".foo{width:15px}\")",
        lines(
            "@external foo;",
            ".foo {",
            "    width:15px;",
            "}"
        ));
  }

  public void testRuntimeConditionalNode() {
    String expectedCss = "((com.foo.BAR) ? (\".foo{color:black}\") + (" +
        "(com.foo.BAR2) ? (\".foo{color:white}\") : (\".foo{color:gray}\"))" +
        " : (com.foo.foo()) ? (\".foo{color:blue}\")" +
        " : (\".foo{color:yellow}\"))";

    assertPrintedResult(expectedCss,
        lines(
            "@if (eval('com.foo.BAR')) {",
            "  .foo {",
            "    color: black;",
            "  }",
            "",
            "  @if (eval('com.foo.BAR2')) {",
            "    .foo {",
            "      color: white;",
            "    }",
            "  }",
            "  @else {",
            "    .foo {",
            "      color: gray;",
            "    }",
            "  }",
            "}",
            "@elseif (eval('com.foo.foo()')) {",
            "  .foo {",
            "    color: blue;",
            "  }",
            "}",
            "@else {",
            "  .foo {",
            "    color:yellow",
            "  }",
            "}"
        ));
  }

  public void testCssDotPathNodePrint() {
    assertPrintedResult("(\".foo{width:\" + (image().getWidth() + \"px\") + \"}\")",
        lines(
            ".foo {",
            "  width: value('image.getWidth', 'px');",
            "}"
        ));
  }

  public void testCssJavaExpressionNodePrint() {
    assertPrintedResult("(\".foo{width:\" " +
            "+ (com.foo.bar.WIDTH) " +
            "+ \";height:\" " +
            "+ (com.foo.bar.height()) " +
            "+ \"}\")",
        lines(
            ".foo {",
            "  width: eval('com.foo.bar.WIDTH');",
            "  height: eval('com.foo.bar.height()');",
            "}"
        ));
  }

  private void assertPrintedResult(String expectedCss, String source) {
    CssTree cssTree = parseAndBuildTree(source);

    CssPrinter pass = new CssPrinter(cssTree);
    pass.runPass();

    assertEquals(expectedCss, pass.getCompactPrintedString());
  }

  @Override
  protected void runPassesOnNewTree(CssTree cssTree, ErrorManager errorManager) {
    MutatingVisitController mutatingVisitController = cssTree.getMutatingVisitController();

    new CreateConditionalNodes(mutatingVisitController, errorManager).runPass();
    new CreateRuntimeConditionalNodes(mutatingVisitController).runPass();
    new CreateStandardAtRuleNodes(mutatingVisitController, errorManager).runPass();

    ResourceContext context = mockResourceContext();
    Map<String, GssFunction> gssFunctionMap = new GwtGssFunctionMapProvider(context).get();
    new ResolveCustomFunctionNodes(mutatingVisitController, errorManager,
        gssFunctionMap, true, new HashSet<String>()).runPass();
    new ExternalClassesCollector(mutatingVisitController, errorManager)
        .runPass();
  }

  private ResourceContext mockResourceContext() {
    ResourceContext context = mock(ResourceContext.class);
    GeneratorContext generatorContext = mock(GeneratorContext.class);
    TypeOracle oracle = mock(TypeOracle.class);
    when(generatorContext.getTypeOracle()).thenReturn(oracle);
    when(context.getGeneratorContext()).thenReturn(generatorContext);
    return context;
  }
}
