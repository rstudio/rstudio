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

import com.google.gwt.resources.client.gss.TestResources.Charset;
import com.google.gwt.resources.client.gss.TestResources.ClassNameAnnotation;
import com.google.gwt.resources.client.gss.TestResources.EmptyClass;
import com.google.gwt.resources.client.gss.TestResources.Forloop;
import com.google.gwt.resources.client.gss.TestResources.GenKeyFrames;
import com.google.gwt.resources.client.gss.TestResources.NonStandardAtRules;
import com.google.gwt.resources.client.gss.TestResources.NonStandardFunctions;
import com.google.gwt.resources.client.gss.TestResources.RuntimeConditional;
import com.google.gwt.resources.client.gss.TestResources.SomeGssResource;
import com.google.gwt.resources.client.gss.TestResources.WithConstant;

/**
 * Contains various full-stack tests of the CssResource system with GSS.
 */
public class GssResourceTest extends RenamingClassNameTest {
  @Override
  public String getModuleName() {
    return "com.google.gwt.resources.GssResourceTest";
  }

  @Override
  public void testClassesRenaming() {
    ClassNameAnnotation classNameAnnotation = res().classNameAnnotation();
    String renamedClass = classNameAnnotation.renamedClass();
    String nonRenamedClass = classNameAnnotation.nonRenamedClass();

    assertTrue(renamedClass.matches(OBFUSCATION_PATTERN));
    assertTrue(nonRenamedClass.matches(OBFUSCATION_PATTERN));
  }

  public void testMixin() {
    String text = res().mixin().getText();

    assertTrue(text.contains("{width:120px;height:100px}"));
  }

  public void testAdd() {
    String text = res().add().getText();

    assertTrue(text.contains("{width:220px}"));
  }

  public void testEval() {
    String text = res().eval().getText();

    assertTrue(text.contains("{color:#fff;background-color:#f00;width:30px}"));
  }

  public void testSprite() {
    String text = res().sprite().getText();

    String expected = "{height:64px;width:64px;overflow:hidden;background:url(" + res()
        .someImageResource().getSafeUri().asString() + ") -0px -0px  no-repeat}";

    assertTrue(text.contains(expected));
  }

  public void testResourceUrl() {
    String text = res().resourceUrl().getText();

    String expected = "{cursor:url(" + res().someDataResource().getSafeUri().asString() + ");"
        + "background-image:url(" + res().someImageResource().getSafeUri().asString() + ");"
        + "cursor:url(" + res().someDataResource().getSafeUri().asString() + ");"
        + "background-image:url(" + res().someImageResource().getSafeUri().asString() + ")}";
    assertTrue(text.contains(expected));
  }

  /**
   * Test that empty class definitions are removed from the resulting css.
   */
  public void testEmptyClass() {
    EmptyClass emptyClass = res().emptyClass();

    assertEquals("", emptyClass.getText());
  }

  public void testConstant() {
    WithConstant withConstant = res().withConstant();

    assertEquals("15px", withConstant.constantOne());

    String expectedCss = "." + withConstant.classOne() + "{padding:" + withConstant.constantOne()
        + "}";
    assertEquals(expectedCss, withConstant.getText());
  }

  public void testClassNameAnnotation() {
    ClassNameAnnotation css = res().classNameAnnotation();

    String expectedCss = "." + css.renamedClass() + "{color:black}." + css.nonRenamedClass()
        + "{color:white}";
    assertEquals(expectedCss, css.getText());
  }

  public void testConstants() {
    assertEquals("15px", res().cssWithConstant().constantOne());
    assertEquals(5, res().cssWithConstant().constantTwo());
    assertEquals("black", res().cssWithConstant().CONSTANT_THREE());

    assertNotSame("white", res().cssWithConstant().conflictConstantClass());

    assertEquals(15, res().cssWithConstant().overrideConstantInt());
    assertNotSame("15px", res().cssWithConstant().overrideConstantIntClass());
  }

  public void testNotStrict() {
    SomeGssResource notStrict = res().notstrict();

    String expectedCss = "." + notStrict.someClass() + "{color:black}.otherNotStrictClass{" +
        "color:white}";

    assertEquals(expectedCss, notStrict.getText());
  }

  public void testRuntimeConditional() {
    RuntimeConditional runtimeConditional = res().runtimeConditional();
    String foo = runtimeConditional.foo();

    BooleanEval.FIRST = true;
    BooleanEval.SECOND = true;
    BooleanEval.THIRD = true;

    assertEquals(runtimeExpectedCss("purple", "10px", foo), runtimeConditional.getText());

    BooleanEval.FIRST = false;
    BooleanEval.SECOND = true;
    BooleanEval.THIRD = true;

    assertEquals(runtimeExpectedCss("black", "10px", foo), runtimeConditional.getText());

    BooleanEval.FIRST = false;
    BooleanEval.SECOND = true;
    BooleanEval.THIRD = false;

    assertEquals(runtimeExpectedCss("khaki", "10px", foo), runtimeConditional.getText());

    BooleanEval.FIRST = false;
    BooleanEval.SECOND = false;

    assertEquals(runtimeExpectedCss("gray", "10px", foo), runtimeConditional.getText());
  }

  public void testNonStandardAtRules() {
    NonStandardAtRules nonStandardAtRules = res().nonStandardAtRules();

    String css = nonStandardAtRules.getText();
    assertTrue(css.contains("@extenal"));
    assertTrue(css.contains("@-moz-document"));
    assertTrue(css.contains("@supports"));
  }

  public void testNonStandardFunctions() {
    NonStandardFunctions nonStandardFunctions = res().nonStandardFunctions();

    String css = nonStandardFunctions.getText();
    assertTrue(css.contains("expression("));
    assertTrue(css.contains("progid:DXImageTransform.Microsoft.gradient("));
  }

  public void testCharset() {
    Charset charset = res().charset();

    assertEquals("div{content:\"\\008305\\008306\\008307\"}", charset.getText());
  }

  public void testConstantAccess() {
    assertEquals("10px", res().constants().padding2());
    assertEquals("#012345", res().constants().color1());
    assertEquals("#012345", res().constants().mycolor());
    assertEquals("#012345", res().constants().mycolor1());
    assertEquals(10, res().constants().margin());
    assertEquals(120, res().constants().width());
    assertEquals(1, res().constants().bar());

    assertEquals("div{width:120px}", res().constants().getText());
  }

  public void testEmpty() {
    // should not throw an exception if the file is empty
    assertEquals("", res().empty().getText());
  }

  public void testForLoop() {
    Forloop forloop = res().forloop();

    String expectedCss = "." + forloop.foo0() + "{padding:0}." + forloop.foo2() + "{padding:2px}." + forloop.foo4() + "{padding:4px}";

    assertEquals(expectedCss, forloop.getText());
  }

  public void testGenKeyFrames() {
    GenKeyFrames genKeyFrames = res().genKeyFrames();

    String expectedCss = "@keyframes myframe{0%{top:0}to{top:200px}}@-webkit-keyframes myframe{0%{top:0}to{top:200px}}" +
            "div{animation:myframe 5s infinite}";

    assertEquals(expectedCss, genKeyFrames.getText());
  }

  private String runtimeExpectedCss(String color, String padding, String foo) {
    String s = "." + foo + "{width:100%}" + "." + foo + "{color:" + color + "}";

    if (padding != null) {
      s += "." + foo + "{padding:" + padding + "}";
    }

    s += "." + foo + "{margin:100px}";

    if (RuntimeConditional.CONSTANT_DEFINED_ON_INTERFACE) {
      s += "." + foo + "{height:10px}";
    }

    s += "." + foo + "{font-family:kennedy;top:5px}";

    return s;
  }
}
