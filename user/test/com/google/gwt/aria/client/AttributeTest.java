/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.aria.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests {@link Attribute} ARIA class
 */
public class AttributeTest extends GWTTestCase {
  private Element div;
  private Attribute<OrientationValue> attribute1;
  private Attribute<Boolean> attribute2;
  private Attribute<String> attribute3;
  private Attribute<RelevantValue> attribute4;

  public void testSetGetRemove_booleanValue() {
    attribute2.setDefault(div);
    attribute2.set(div, false);
    attribute2.set(div, true);
    assertEquals("true", attribute2.get(div));
    attribute2.set(div, false);
    assertEquals("false", attribute2.get(div));
    attribute2.remove(div);
    assertEquals("", attribute2.get(div));
  }

  public void testSetGetRemove_tokenValue() {
    attribute1.setDefault(div);
    assertEquals(OrientationValue.VERTICAL.getAriaValue(), attribute1.get(div));
    attribute1.remove(div);
    assertEquals("", attribute1.get(div));
    attribute1.set(div, OrientationValue.HORIZONTAL);
    assertEquals(OrientationValue.HORIZONTAL.getAriaValue(), attribute1.get(div));
  }

  public void testSetGetRemove_tokenListValue() {
    attribute4.setDefault(div);
    assertEquals(RelevantValue.ADDITIONS.getAriaValue() + " " + RelevantValue.TEXT.getAriaValue(),
        attribute4.get(div));
    attribute4.remove(div);
    assertEquals("", attribute1.get(div));
    attribute4.set(div, RelevantValue.REMOVALS);
    assertEquals(RelevantValue.REMOVALS.getAriaValue(), attribute4.get(div));
  }

  public void testSetDefaultValue_noSet() {
    try {
      attribute3.setDefault(div);
      fail();
    } catch (AssertionError e) {
      // Expected -- no default value for attribute2
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.aria.Aria";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    div = Document.get().createDivElement();
    div.setAttribute("id", "test1");
    Document.get().getBody().appendChild(div);
    attribute1 = new AriaValueAttribute<OrientationValue>("attr1", "vertical");
    attribute2 = new PrimitiveValueAttribute<Boolean>("attr2", "true");
    attribute3 = new PrimitiveValueAttribute<String>("attr3");
    attribute4 = new AriaValueAttribute<RelevantValue>("attr4", "additions text");
  }

  @Override
  protected void gwtTearDown() throws Exception {
    super.gwtTearDown();
    div.removeFromParent();
  }
}
