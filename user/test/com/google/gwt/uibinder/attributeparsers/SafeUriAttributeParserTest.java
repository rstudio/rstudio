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
package com.google.gwt.uibinder.attributeparsers;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.uibinder.rebind.MockMortalLogger;
import com.google.gwt.uibinder.test.UiJavaResources;

import junit.framework.TestCase;

/**
 * Test parsing SafeUri attributes.
 */
public class SafeUriAttributeParserTest extends TestCase {
  private SafeUriAttributeParser parserForHtml;
  private SafeUriAttributeParser parserForWidgets;
  private MockMortalLogger logger = new MockMortalLogger();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    CompilationState state =
        CompilationStateBuilder.buildFrom(TreeLogger.NULL, UiJavaResources.getUiResources());
    TypeOracle types = state.getTypeOracle();
    JType stringType = types.parse(String.class.getName());
    JType safeUriType = types.parse(SafeUri.class.getName());
    StringAttributeParser stringParser =
        new StringAttributeParser(new FieldReferenceConverter(null), stringType);
    parserForHtml =
        new SafeUriAttributeParser(stringParser, new FieldReferenceConverter(null), safeUriType,
            stringType, logger);
    parserForWidgets =
        new SafeUriAttributeParser(stringParser, new FieldReferenceConverter(null), safeUriType,
            logger);
  }

  public void testLiteral() throws UnableToCompleteException {
    assertEquals("UriUtils.fromSafeConstant(\"hi mom\")", parserForHtml.parse(null, "hi mom"));
    assertEquals("UriUtils.fromSafeConstant(\"hi mom\")", parserForWidgets.parse(null, "hi mom"));
    // Don't get caught out by escaped braces
    assertEquals("UriUtils.fromSafeConstant(\"hi {foo.bar.baz} friend\")", parserForHtml.parse(
        null, "hi {{foo.bar.baz} friend"));
    assertEquals("UriUtils.fromSafeConstant(\"hi {foo.bar.baz} friend\")", parserForWidgets.parse(
        null, "hi {{foo.bar.baz} friend"));
  }

  public void testFieldRef() throws UnableToCompleteException {
    assertEquals("foo.bar().baz()", parserForHtml.parse(null, "{foo.bar.baz}"));
    assertEquals("foo.bar().baz()", parserForWidgets.parse(null, "{foo.bar.baz}"));
    // Don't get caught out by escaped braces
    assertEquals("UriUtils.fromSafeConstant(\"{foo.bar.baz}\")", parserForHtml.parse(null, "{{foo.bar.baz}"));
    assertEquals("UriUtils.fromSafeConstant(\"{foo.bar.baz}\")", parserForWidgets.parse(null, "{{foo.bar.baz}"));
  }

  public void testConcatenatedFieldRefAllowed() throws UnableToCompleteException {
    assertEquals("UriUtils.fromString(\"hi \" + foo.bar().baz() + \" friend\")",
        parserForHtml.parse(null, "hi {foo.bar.baz} friend"));
    assertNotNull(logger.warned);
    logger.warned = null;
    assertEquals(
        "UriUtils.fromString(\"hi \" + foo.bar().baz() + \" friend \" + boo.bahh() + \" baz\")",
        parserForHtml.parse(null, "hi {foo.bar.baz} friend {boo.bahh} baz"));
    assertNotNull(logger.warned);
  }

  public void testConcatenatedFieldRefNotOkay() {
    try {
      parserForWidgets.parse(null, "hi {foo.bar.baz} friend");
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      // pass
    }
    try {
      parserForWidgets.parse(null, "hi {foo.bar.baz} friend {boo.bahh} baz");
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      // pass
    }
  }
}
