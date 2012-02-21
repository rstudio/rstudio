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

import com.google.gwt.aria.client.CommonAttributeTypes.IdReferenceList;
import com.google.gwt.aria.client.PropertyTokenTypes.DropeffectToken;
import com.google.gwt.aria.client.PropertyTokenTypes.DropeffectTokenList;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests {@link Role} ARIA classes 
 */
public class RoleTest extends GWTTestCase {
  private Element div;
  private RegionRole regionRole;
  
  public void testSetGetRemoveRole() {
    assertEquals("", regionRole.get(div));
    regionRole.set(div);
    assertEquals(regionRole.getName(), regionRole.get(div));
    regionRole.remove(div);
    assertEquals("", regionRole.get(div));
  }
  
  public void testSetGetRemoveProperty() {
    IdReferenceList idRefs = new IdReferenceList("test1");
    assertEquals("", regionRole.getAriaLabelledbyProperty(div));
    regionRole.setAriaLabelledbyProperty(div, idRefs);
    assertEquals("test1", regionRole.getAriaLabelledbyProperty(div));
    regionRole.removeAriaLabelledbyProperty(div);
    assertEquals("", regionRole.getAriaLabelledbyProperty(div));
  }
  
  public void testSetGetRemoveNmtokensProperty() {
    ButtonRole buttonRole = Roles.getButtonRole();
    assertEquals("", buttonRole.getAriaDropeffectProperty(div));
    regionRole.setAriaDropeffectProperty(div, new DropeffectTokenList(DropeffectToken.COPY, 
        DropeffectToken.MOVE));
    assertEquals("copy move", regionRole.getAriaDropeffectProperty(div));
    regionRole.removeAriaDropeffectProperty(div);
    assertEquals("", regionRole.getAriaDropeffectProperty(div));
  }
  
  public void testSetGetRemoveState() {
    assertEquals("", regionRole.getAriaInvalidState(div));
    regionRole.setAriaInvalidState(div, StateTokenTypes.InvalidToken.GRAMMAR);
    assertEquals(StateTokenTypes.InvalidToken.GRAMMAR.getAriaValue(), 
        regionRole.getAriaInvalidState(div));
    regionRole.removeAriaInvalidState(div);
    assertEquals("", regionRole.getAriaInvalidState(div));
  }
  
  public void testSetGetRemoveExtraAttributes() {
    // Some versions of IE default to "0" instead of ""
    assertTrue("".equals(regionRole.getTabindexExtraAttribute(div)) 
        || "0".equals(regionRole.getTabindexExtraAttribute(div)));
    regionRole.setTabindexExtraAttribute(div, 1);
    assertEquals("1", regionRole.getTabindexExtraAttribute(div));
    regionRole.removeTabindexExtraAttribute(div);
    // Some versions of IE default to "0" instead of ""
    assertTrue("".equals(regionRole.getTabindexExtraAttribute(div)) 
        || "0".equals(regionRole.getTabindexExtraAttribute(div)));
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
    regionRole = Roles.getRegionRole();
  }
  
  @Override
  protected void gwtTearDown() throws Exception {
    super.gwtTearDown();
    div.removeFromParent();
  }
}
