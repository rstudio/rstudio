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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gwt.thirdparty.common.css.compiler.ast.CssClassSelectorNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssCompositeValueNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssLiteralNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssStringNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssUnknownAtRuleNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssValueNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.ErrorManager;
import com.google.gwt.thirdparty.common.css.compiler.ast.MutatingVisitController;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test for {@link com.google.gwt.resources.gss.ExternalClassesCollector}.
 */
public class ExternalClassesCollectorTest  extends TestCase {
  private CssUnknownAtRuleNode cssUnknownAtRuleNode;
  private CssLiteralNode atRuleNameNode;
  private MutatingVisitController mutatingVisitController;
  private CssCompositeValueNode atRuleParameters;
  private ErrorManager errorManager;

  @Override
  protected void setUp() {
    cssUnknownAtRuleNode = mock(CssUnknownAtRuleNode.class);
    atRuleNameNode = mock(CssLiteralNode.class);
    mutatingVisitController = mock(MutatingVisitController.class);
    atRuleParameters = mock(CssCompositeValueNode.class);
    errorManager = mock(ErrorManager.class);

    when(cssUnknownAtRuleNode.getName()).thenReturn(atRuleNameNode);
    when(cssUnknownAtRuleNode.getParameters()).thenReturn(
        Lists.<CssValueNode>newArrayList(atRuleParameters));
  }

  public void testLeaveUnknownAtRule_notAnExternalAtRule_doNothing() {
    // Given
    ExternalClassesCollector externalClassesCollector = createAndInitExternalClassesCollector();
    when(atRuleNameNode.getValue()).thenReturn("dummy");

    // When
    externalClassesCollector.leaveUnknownAtRule(cssUnknownAtRuleNode);

    // Then
    verify(cssUnknownAtRuleNode, never()).getParameters();
    verify(mutatingVisitController, never()).removeCurrentNode();
  }

  public void testLeaveUnknownAtRule_simpleExternalAtRule_classesReturnByGetExternalClass() {
    // Given
    HashSet<String> styleClassSet = Sets.newHashSet();
    HashSet<String> orphanClassName = Sets.newHashSet();
    ExternalClassesCollector externalClassesCollector = createAndInitExternalClassesCollector();
    when(atRuleNameNode.getValue()).thenReturn("external");

    List<CssValueNode> parameters = Lists.newArrayList(literalNode("externalClass"),
        literalNode("externalClass2"));
    when(atRuleParameters.getValues()).thenReturn(parameters);

    // When
    externalClassesCollector.leaveUnknownAtRule(cssUnknownAtRuleNode);

    // Then
    verify(cssUnknownAtRuleNode).getParameters();
    verify(atRuleParameters).getValues();
    verify(mutatingVisitController).removeCurrentNode();

    Set<String> externalClasses = externalClassesCollector.getExternalClassNames(styleClassSet,
        orphanClassName);
    assertEquals(2, externalClasses.size());
    assertTrue(externalClasses.contains("externalClass"));
    assertTrue(externalClasses.contains("externalClass2"));
  }

  public void testLeaveUnknownAtRule_externalAtRuleWithMatchAllPrefix_allClassesAreExternals() {
    // Given
    HashSet<String> styleClassSet = Sets.newHashSet("class1", "class2", "class3");
    HashSet<String> orphanClassName = Sets.newHashSet();
    ExternalClassesCollector externalClassesCollector = createAndInitExternalClassesCollector();
    when(atRuleNameNode.getValue()).thenReturn("external");
    List<CssValueNode> parameters = Lists.newArrayList(stringNode("*"));
    when(atRuleParameters.getValues()).thenReturn(parameters);

    // When
    externalClassesCollector.leaveUnknownAtRule(cssUnknownAtRuleNode);

    // Then
    verify(cssUnknownAtRuleNode).getParameters();
    verify(atRuleParameters).getValues();
    verify(mutatingVisitController).removeCurrentNode();

    Set<String> externalClasses = externalClassesCollector.getExternalClassNames(styleClassSet,
        orphanClassName);
    assertEquals(3, externalClasses.size());
    assertTrue(externalClasses.contains("class1"));
    assertTrue(externalClasses.contains("class2"));
    assertTrue(externalClasses.contains("class3"));
  }

  public void
  testLeaveUnknownAtRule_styleClassWithoutMethodAndRemovedFromAST_consideredAsExternal() {
    // Given
    HashSet<String> styleClassSet = Sets.newHashSet("foo", "bar");
    ExternalClassesCollector externalClassesCollector = createAndInitExternalClassesCollector();
    // AST contains only one style class named foo, bar is not in the AST anymore
    CssClassSelectorNode classSelectorNode = mock(CssClassSelectorNode.class);
    when(classSelectorNode.getRefinerName()).thenReturn("foo");
    externalClassesCollector.enterClassSelector(classSelectorNode);
    // The style class bar is not associated to a java method
    HashSet<String> orphanClassName = Sets.newHashSet("bar");

    // When
    Set<String> externalClasses = externalClassesCollector.getExternalClassNames(styleClassSet,
        orphanClassName);

    // Then
    assertEquals(1, externalClasses.size());
    assertTrue(externalClasses.contains("bar"));
  }

  public void testLeaveUnknownAtRule_externalAtRuleWithMatchAllPrefixThenAnotherExternalAtRule_anotherAtRuleNotProcessed() {
    // Given
    ExternalClassesCollector externalClassesCollector = createAndInitExternalClassesCollector();
    when(atRuleNameNode.getValue()).thenReturn("external");
    List<CssValueNode> parameters = Lists.newArrayList(stringNode("*"));
    when(atRuleParameters.getValues()).thenReturn(parameters);
    externalClassesCollector.leaveUnknownAtRule(cssUnknownAtRuleNode);
    reset(mutatingVisitController);
    CssUnknownAtRuleNode secondAtRuleNode = mock(CssUnknownAtRuleNode.class);
    CssLiteralNode secondAtRuleNameNode = mock(CssLiteralNode.class);
    when(secondAtRuleNameNode.getValue()).thenReturn("external");
    when(secondAtRuleNode.getName()).thenReturn(secondAtRuleNameNode);

    // When
    externalClassesCollector.leaveUnknownAtRule(secondAtRuleNode);

    // Then
    verify(secondAtRuleNode, never()).getParameters();
    verify(mutatingVisitController).removeCurrentNode();
  }

  public void testLeaveUnknownAtRule_externalAtRuleWithPrefix_classesMatchingThePrefixAreExternals() {
    // Given
    HashSet<String> styleClassSet = Sets.newHashSet("prefix", "prefix-class1",
        "prefi-notexternal","external");
    HashSet<String> orphanClassName = Sets.newHashSet();
    ExternalClassesCollector externalClassesCollector = createAndInitExternalClassesCollector();
    when(atRuleNameNode.getValue()).thenReturn("external");
    List<CssValueNode> parameters = Lists.newArrayList(literalNode("external"),
        stringNode("prefix*"));
    when(atRuleParameters.getValues()).thenReturn(parameters);

    // When
    externalClassesCollector.leaveUnknownAtRule(cssUnknownAtRuleNode);

    // Then
    verify(cssUnknownAtRuleNode).getParameters();
    verify(atRuleParameters).getValues();
    verify(mutatingVisitController).removeCurrentNode();

    Set<String> externalClasses = externalClassesCollector.getExternalClassNames(styleClassSet,
        orphanClassName);
    assertEquals(3, externalClasses.size());
    assertTrue(externalClasses.contains("prefix"));
    assertTrue(externalClasses.contains("prefix-class1"));
    assertTrue(externalClasses.contains("external"));
  }

  private CssValueNode literalNode(String externalClass) {
    CssValueNode node = mock(CssLiteralNode.class);
    when(node.getValue()).thenReturn(externalClass);
    return node;
  }

  private CssValueNode stringNode(String selector) {
    CssStringNode node = mock(CssStringNode.class);
    when(node.getConcreteValue()).thenReturn(selector);
    return node;
  }

  private ExternalClassesCollector createAndInitExternalClassesCollector() {
    ExternalClassesCollector externalClassesCollector =
        new ExternalClassesCollector(mutatingVisitController, errorManager);

    // initialise the object but do nothing
    externalClassesCollector.runPass();

    return externalClassesCollector;
  }
}
