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
package com.google.gwt.resources.rg;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.resources.css.GenerateCssAst;
import com.google.gwt.resources.css.ast.CssNode;
import com.google.gwt.resources.css.ast.CssStylesheet;
import com.google.gwt.resources.css.ast.CssVisitor;
import com.google.gwt.resources.css.ast.HasNodes;
import com.google.gwt.resources.ext.ClientBundleRequirements;
import com.google.gwt.resources.ext.ResourceContext;

import junit.framework.TestCase;

import java.net.URL;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains functions for golden-output tests that are concerned with structural
 * modifications to the CSS AST.
 */
public class CssTestCase extends TestCase {
  /*
   * NB: This class is in the resources.rg package so that it can access
   * package-protected methods in CssResourceGenerator.
   */

  /**
   * Triggers an assertion if a CssNode is traversed more than once.
   * 
   * @see CssTestCase#assertNoAliasing(CssNode)
   */
  public static class AliasDetector extends CssVisitor {
    private final Map<CssNode, Void> seen = new IdentityHashMap<CssNode, Void>();

    @Override
    protected void doAccept(List<? extends CssNode> list) {
      for (CssNode node : list) {
        doAccept(node);
      }
    }

    @Override
    protected <T extends CssNode> T doAccept(T node) {
      assertFalse("Found repeated node " + node.toString(),
          seen.containsKey(node));
      seen.put(node, null);
      return super.doAccept(node);
    }

    @Override
    protected void doAcceptWithInsertRemove(List<? extends CssNode> list) {
      for (CssNode node : list) {
        doAccept(node);
      }
    }
  }

  /**
   * Total fake, no implementations.
   */
  private static class FakeContext implements ResourceContext {
    public String deploy(String suggestedFileName, String mimeType,
        byte[] data, boolean forceExternal) throws UnableToCompleteException {
      return null;
    }

    @Deprecated
    public String deploy(URL resource, boolean forceExternal)
        throws UnableToCompleteException {
      return null;
    }

    public String deploy(URL resource, String mimeType, boolean forceExternal)
        throws UnableToCompleteException {
      return null;
    }

    public <T> T getCachedData(String key, Class<T> clazz) {
      return null;
    }

    public JClassType getClientBundleType() {
      return null;
    }

    public GeneratorContext getGeneratorContext() {
      return null;
    }

    public String getImplementationSimpleSourceName()
        throws IllegalStateException {
      return null;
    }
    
    public ClientBundleRequirements getRequirements() {
      return null;
    }

    public <T> boolean putCachedData(String key, T value) {
      return false;
    }

    public boolean supportsDataUrls() {
      return true;
    }
  }

  /**
   * Asserts that two CssNodes are identical.
   */
  protected static <T extends CssNode & HasNodes> void assertEquals(
      TreeLogger logger, T expected, T test) throws UnableToCompleteException {
    String expectedCss = CssResourceGenerator.makeExpression(logger,
        new FakeContext(), expected, false);
    String testCss = CssResourceGenerator.makeExpression(logger,
        new FakeContext(), test, false);
    assertEquals(expectedCss, testCss);
  }

  /**
   * Ensure that no CssNode is traversed more than once due to AST errors.
   */
  protected static void assertNoAliasing(CssNode node) {
    (new AliasDetector()).accept(node);
  }

  /**
   * Compares the generated Java expressions for an input file, transformed in
   * order by the specified visitors, and a golden-output file.
   */
  private static void test(TreeLogger logger, URL test, URL expected,
      CssVisitor... visitors) throws UnableToCompleteException {

    CssStylesheet expectedSheet = null;
    CssStylesheet testSheet = null;

    try {
      expectedSheet = GenerateCssAst.exec(logger, expected);
      testSheet = GenerateCssAst.exec(logger, test);
    } catch (UnableToCompleteException e) {
      fail("Unable to parse stylesheet");
    }

    for (CssVisitor v : visitors) {
      v.accept(testSheet);
    }

    assertEquals(logger, expectedSheet, testSheet);
  }

  /**
   * Runs a test.
   * 
   * @param testName is used to compute the test and expected resource paths.
   * @param reversible if <code>true</code>, the test will attempt to transform
   *          the expected css into the test css
   */
  protected void test(TreeLogger logger, String testName, boolean reversible,
      CssVisitor... visitors) throws UnableToCompleteException {
    String packagePath = getClass().getPackage().getName().replace('.', '/')
        + "/";
    URL testUrl = getClass().getClassLoader().getResource(
        packagePath + testName + "_test.css");
    assertNotNull("Could not find testUrl", testUrl);
    URL expectedUrl = getClass().getClassLoader().getResource(
        packagePath + testName + "_expected.css");
    assertNotNull("Could not find testUrl", expectedUrl);

    test(logger, testUrl, expectedUrl, visitors);

    if (reversible) {
      test(logger, expectedUrl, testUrl, visitors);
    }
  }
}
