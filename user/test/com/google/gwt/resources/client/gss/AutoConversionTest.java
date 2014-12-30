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

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.resources.client.gss.AutoConversionBundle.Conditional;
import com.google.gwt.resources.client.gss.AutoConversionBundle.ConstantConditional;
import com.google.gwt.resources.client.gss.AutoConversionBundle.ConstantRenaming;
import com.google.gwt.resources.client.gss.AutoConversionBundle.LenientExternal;

/**
 * Test for automatic conversion from CSS to GSS.
 */
public class AutoConversionTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.resources.GssAutoConversion";
  }

  public void testConstantRenaming() {
    ConstantRenaming constantRenaming = res().constantRenaming();

    assertEquals(45, constantRenaming.myConstant());
    assertEquals("38px", constantRenaming.my_constant());
    assertEquals(0, constantRenaming.ie6());
    assertEquals(0, constantRenaming.gecko1_8());

    assertEquals("div{box-shadow:inset 0 1px 1px rgba(0,0,0,0.2)}", constantRenaming.getText());
  }

  public void testConstantConditional() {
    ConstantConditional constantConditional = res().constantConditional();

    String expectedCss = "." + constantConditional.foo() + "{width:15px;height:10px;color:black}";
    assertEquals(expectedCss, constantConditional.getText());
    assertEquals("black", constantConditional.color());
    assertEquals(15, constantConditional.width());
    assertEquals(10, constantConditional.height());
  }

  public void testLenientExternal() {
    LenientExternal lenientExternal = res().lenientExternal();

    assertNotSame("obfuscated", lenientExternal.obfuscated());
    assertEquals("nonObfuscated", lenientExternal.nonObfuscated());
    assertEquals("nonObfuscated2", lenientExternal.nonObfuscated2());
    assertEquals("nonObfuscated3", lenientExternal.nonObfuscated3());
  }

  public void testConditional() {
    Conditional conditional = res().conditional();

    String expectedCss = "." + conditional.foo() +
        "{color:white;font-family:kennedy;font-weight:bold;top:5px;left:5px}";
    assertEquals(expectedCss, conditional.getText());
  }

  private AutoConversionBundle res() {
    return GWT.create(AutoConversionBundle.class);
  }

}
