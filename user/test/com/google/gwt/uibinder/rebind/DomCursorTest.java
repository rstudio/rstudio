/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Test for DomCuror.
 */
public class DomCursorTest extends TestCase {

  private static final String PARENT = "parent";
  
  private UiBinderWriter writer;
  private DomCursor cursor;
  
  @Override
  public void setUp() {
    writer = org.easymock.classextension.EasyMock.createMock(
        UiBinderWriter.class);
    org.easymock.classextension.EasyMock.expect(writer.getUniqueId()).andStubAnswer(new IAnswer<Integer>() {
      private int nextId = 1;
      public Integer answer() {
        return nextId++;
      }      
    });
    MessagesWriter message = new MessagesWriter("ui", null, "", "", "");
    org.easymock.classextension.EasyMock.expect(writer.getMessages()).andStubReturn(message);
    writer.addInitComment((String) EasyMock.notNull(), EasyMock.notNull());
    org.easymock.classextension.EasyMock.expectLastCall().asStub();
    cursor = new DomCursor(PARENT, writer);
  }
  
  public void testAccessExpressions() throws Exception {    
    verifyInitAssignment(writer, "UiBinderUtil.getNonTextChild(parent, 0).cast()", 1);    
    verifyInitAssignment(writer, "UiBinderUtil.getNonTextChild(parent, 1).cast()", 2);
    verifyInitAssignment(writer, 
        "UiBinderUtil.getNonTextChild(intermediate2, 0).cast()", 3);
    verifyInitAssignment(writer, "UiBinderUtil.getNonTextChild(parent, 2).cast()", 4);    
    org.easymock.classextension.EasyMock.replay(writer);
    
    // parent
    cursor.visitChild(makeElement("div")); 
    // parent's first child
    assertEquals(intermediate(1), cursor.getAccessExpression());  

    cursor.advanceChild(); 
    assertEquals(intermediate(2), cursor.getAccessExpression());
    // intermediate 2, parent's second child
    XMLElement span = makeElement("span");  
    cursor.visitChild(span);
    // intermediate 3, intermediate 2's first child
    assertEquals(intermediate(3), cursor.getAccessExpression());  
    cursor.finishChild(span);
    
    cursor.advanceChild();
    // intermediate 4, parent's third child
    assertEquals(intermediate(4), cursor.getAccessExpression());
    
    org.easymock.classextension.EasyMock.verify(writer);
  }
  
  public void testParagraphsWith() throws Exception {
    writer.die((String) EasyMock.anyObject());
    org.easymock.classextension.EasyMock.expectLastCall().andThrow(new UnableToCompleteException());
    org.easymock.classextension.EasyMock.replay(writer);
    
    cursor.visitChild(makeElement("p"));
    XMLElement span = makeElement("span");
    cursor.visitChild(span);
    cursor.finishChild(span);
    cursor.visitChild(makeElement("div"));    
    
    try {
      cursor.getAccessExpression();
      fail("Expected exception about block elements inside paragraphs");
    } catch (Exception e) {
      // Expected
    }
    org.easymock.classextension.EasyMock.verify(writer);
  }
  
  public void testTables() throws UnableToCompleteException {
    
    verifyInitAssignment(writer, intermediate(1), "UiBinderUtil.getNonTextChild", "parent", 0);
    verifyInitAssignment(writer, "UiBinderUtil.getTableChild(intermediate1, 0).cast()", 2);    
    verifyInitAssignment(writer, "UiBinderUtil.getNonTextChild(intermediate2, 0).cast()", 3);    
    verifyInitAssignment(writer, intermediate(4), "UiBinderUtil.getNonTextChild", "parent", 1);    
    verifyInitAssignment(writer, "UiBinderUtil.getNonTextChild(intermediate4, 0).cast()", 5);    
    verifyInitAssignment(writer, "UiBinderUtil.getNonTextChild(intermediate5, 0).cast()", 6);
    
    org.easymock.classextension.EasyMock.replay(writer);
    XMLElement div = makeElement("div");
    cursor.visitChild(div);
    
    XMLElement table1 = makeElement("table");
    cursor.visitChild(table1);
    assertEquals(intermediate(2), cursor.getAccessExpression());
    
    XMLElement tr = makeElement("tr");
    cursor.visitChild(tr);
    assertEquals(intermediate(3), cursor.getAccessExpression());
    
    cursor.finishChild(tr);
    cursor.finishChild(table1);
    cursor.advanceChild();
    XMLElement table2 = makeElement("table");
    cursor.visitChild(table2);
    
    XMLElement tbody = makeElement("tbody");
    cursor.visitChild(tbody);
    cursor.finishChild(tbody);
    assertEquals(intermediate(5), cursor.getAccessExpression());
    
    XMLElement tr2 = makeElement("tr");
    cursor.visitChild(tr2);
    assertEquals(intermediate(6), cursor.getAccessExpression());
    cursor.finishChild(tr2);
    
    XMLElement td = makeElement("td");
    try {
      cursor.visitChild(td);
      fail("Expected exception about tds inside tables without trs");
    } catch (Exception e) {
      // expected
    }
    org.easymock.classextension.EasyMock.verify(writer);
  }

  private String intermediate(int count) {
    return "intermediate" + count;
  }
  
  private XMLElement makeElement(String tag) {
    NamedNodeMap attributes = EasyMock.createNiceMock(NamedNodeMap.class);
    Element element = EasyMock.createNiceMock(Element.class);
    EasyMock.expect(element.getLocalName()).andStubReturn(tag);
    EasyMock.expect(element.getTagName()).andStubReturn(tag);
    EasyMock.expect(element.getAttributes()).andStubReturn(attributes);
    EasyMock.replay(element, attributes);
    return new XMLElement(element, null, null, null, null, null);
  }
  
  private void verifyInitAssignment(UiBinderWriter writer, String expr, int intermediateCount) {
    writer.addInitStatement("com.google.gwt.dom.client.Node %s = %s;", 
        "intermediate" + intermediateCount, expr);
  }
  
  private void verifyInitAssignment(UiBinderWriter writer, String var, String method, String parent,
      int index) {
    writer.addInitStatement("com.google.gwt.dom.client.Node %s = %s(%s, %d);", var, method, parent,
        index);
  } 
}
