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
package com.google.gwt.uibinder.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

/**
 * Static helper methods used by UiBinder. These methods are likely to move,
 * so please don't use them for non-UiBinder code.
 */
public class UiBinderUtil {
  
  private static Element hiddenDiv;

  public static Element fromHtml(String html) {
    ensureHiddenDiv();
    hiddenDiv.setInnerHTML(html);
    Element newbie = hiddenDiv.getFirstChildElement();
    orphan(newbie);
    return newbie;
  }
  
  public static Node getChild(Node node, int child) {
    return node.getChild(child);
  }
  
  public static Node getNonTextChild(Node node, int child) {
    return node.getNonTextChild(child);
  }
  
  public static Node getTableChild(Node node, int child) {
    // If the table, has a tbody inside...
    Element table = (Element)node;
    Element firstChild = table.getNonTextChild(0).cast();
    if ("tbody".equalsIgnoreCase(firstChild.getTagName())) {
      return firstChild.getNonTextChild(child);
    } else {
      return table.getNonTextChild(child);
    }
  }
  
  public static native Element lookupNodeByTreeIndicies(Element parent, String query,
      String xpath) /*-{
    if (parent.querySelector) {
      return parent.querySelector(query);
    } else {
      return parent.ownerDocument.evaluate(
        xpath, parent, null, XPathResult.ANY_TYPE, null).iterateNext();
    }
  }-*/;
  
  public static native Element lookupNodeByTreeIndiciesIE(Element parent, int[] indicies) /*-{
  var currentNode = parent;
  for(var i = 0; i < indicies.length; i = i + 1) {
    currentNode = currentNode.children[indicies[i]];
  }
  return currentNode;
}-*/;
  
  public static native Element lookupNodeByTreeIndiciesUsingQuery(Element parent, String query) /*-{
    return parent.querySelector(query);
  }-*/; 
  
  public static native Element lookupNodeByTreeIndiciesUsingXpath(Element parent, String xpath) /*-{
    return parent.ownerDocument.evaluate(
        xpath, parent, null, XPathResult.ANY_TYPE, null).iterateNext();
  }-*/; 
  
  private static void ensureHiddenDiv() {
    // If the hidden DIV has not been created, create it.
    if (hiddenDiv == null) {
      hiddenDiv = Document.get().createDivElement();      
    }
  }

  private static void orphan(Node node) {
    node.getParentNode().removeChild(node);
  }

  /**
   * Not to be instantiated.
   */
  private UiBinderUtil() { }
}
