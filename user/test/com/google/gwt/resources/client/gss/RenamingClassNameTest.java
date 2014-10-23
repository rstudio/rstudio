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

package com.google.gwt.resources.client.gss;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.resources.client.gss.TestResources.ExternalClasses;
import com.google.gwt.resources.client.gss.TestResources.TestImportCss;

/**
 * These tests are tested with several configurations.
 *
 * @see com.google.gwt.resources.client.gss.StableNoTypeObfuscationStyleTest
 * @see com.google.gwt.resources.client.gss.StableObfuscationStyleTest
 * @see com.google.gwt.resources.client.gss.StableShortTypeObfuscationStyleTest
 * @see com.google.gwt.resources.client.gss.DebugObfuscationStyleTest
 * @see com.google.gwt.resources.client.gss.GssResourceTest
 */
public abstract class RenamingClassNameTest extends GWTTestCase {
  static final String OBFUSCATION_PATTERN = "[a-zA-Z][a-zA-Z0-9]*-[a-zA-Z][a-zA-Z0-9]*";

  /**
   * Test that style classes mentioned as external are not obfuscated.
   */
  public void testExternalClasses() {
    ExternalClasses externalClasses = res().externalClasses();

    assertNotSame("obfuscatedClass", externalClasses.obfuscatedClass());

    assertEquals("externalClass", externalClasses.externalClass());
    assertEquals("externalClass2", externalClasses.externalClass2());
    assertEquals("unobfuscated", externalClasses.unobfuscated());
    assertEquals("unobfuscated2", externalClasses.unobfuscated2());

    String css = externalClasses.getText();

    // external at-rule shouldn't be printed
    assertFalse(css.contains("@external"));
    String expectedCss = "." +  externalClasses.obfuscatedClass() + "{width:100%}" +
        ".externalClass,.externalClass2,.unobfuscated{height:50px}" +
        ".unobfuscated-with-prefix-without-method{width:15px}" +
        ".unobfuscated2{height:50px}" +
        ".externalWithoutMethod{color:white}";

    assertEquals(expectedCss, css);
  }

  public void testObfuscationScope() {
    ScopeResource res = GWT.create(ScopeResource.class);

    assertEquals(res.scopeA().foo(), res.scopeA2().foo());
    assertNotSame(res.scopeA().foo(), res.scopeB().foo());
    assertNotSame(res.scopeB().foo(), res.scopeC().foo());
    assertNotSame(res.scopeA().foo(), res.scopeC().foo());
  }

  public void testImportAndImportWithPrefix() {
    TestImportCss css = res().testImportCss();
    ImportResource importResource = GWT.create(ImportResource.class);
    ImportResource.ImportCss importCss = importResource.importCss();
    ImportResource.ImportWithPrefixCss importWithPrefixCss = importResource.importWithPrefixCss();

    String expectedCss = "." + css.other() + "{color:black}." + importCss.className() +
        " ." + css.other() + "{color:white}." + importWithPrefixCss.className() + " ." +
        css.other() + "{color:gray}";
    assertEquals(expectedCss, css.getText());
  }

  public void testSharedScope() {
    ScopeResource res = GWT.create(ScopeResource.class);
    TestResources res2 = res();

    // shareClassName1 is shared
    assertEquals(res.sharedParent().sharedClassName1(), res.sharedChild1().sharedClassName1());
    assertEquals(res.sharedParent().sharedClassName1(), res.sharedChild2().sharedClassName1());
    assertEquals(res.sharedParent().sharedClassName1(), res.sharedGreatChild().sharedClassName1());
    assertEquals(res.sharedParent().sharedClassName1(), res2.sharedChild3().sharedClassName1());

    // shareClassName2 is shared
    assertEquals(res.sharedParent().sharedClassName2(), res.sharedChild1().sharedClassName2());
    assertEquals(res.sharedParent().sharedClassName2(), res.sharedChild2().sharedClassName2());
    assertEquals(res.sharedParent().sharedClassName2(), res.sharedGreatChild().sharedClassName2());
    assertEquals(res.sharedParent().sharedClassName2(), res2.sharedChild3().sharedClassName2());

    // nonSharedClassName isn't shared
    assertNotSame(res.sharedChild1().nonSharedClassName(),
        res.sharedChild2().nonSharedClassName());
    assertNotSame(res.sharedChild1().nonSharedClassName(),
        res.sharedGreatChild().nonSharedClassName());
    assertNotSame(res.sharedChild1().nonSharedClassName(),
        res2.sharedChild3().nonSharedClassName());
    assertNotSame(res.sharedChild2().nonSharedClassName(),
        res.sharedGreatChild().nonSharedClassName());
    assertNotSame(res.sharedChild2().nonSharedClassName(),
        res2.sharedChild3().nonSharedClassName());
    assertNotSame(res2.sharedChild3().nonSharedClassName(),
        res.sharedGreatChild().nonSharedClassName());
  }

  public abstract void testClassesRenaming();

  protected TestResources res() {
    return GWT.create(TestResources.class);
  }
}
