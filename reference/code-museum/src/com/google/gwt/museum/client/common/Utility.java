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

package com.google.gwt.museum.client.common;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;

/**
 * Utility helper methods.
 */
public class Utility {
  /**
   * Convenience method for getting the document's head element.
   * 
   * @return the document's head element
   */
  public static native HeadElement getHeadElement() /*-{
    return $doc.getElementsByTagName("head")[0];
  }-*/;

  /**
   * Remove the GWT style sheet from the head element so it does not conflict
   * with this styles used by the issue. The CSS file used by the issue can
   * still import the GWT style sheet if needed.
   */
  public static void removeGwtStyleSheet() {
    // Remove the GWT style sheet
    HeadElement headElem = getHeadElement();
    NodeList<Node> children = headElem.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.getItem(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element elem = Element.as(node);
        if (elem.getTagName().equalsIgnoreCase("link")
            && elem.getPropertyString("rel").equalsIgnoreCase("stylesheet")
            && elem.getPropertyString("href").contains("standard.css")) {
          headElem.removeChild(elem);
          return;
        }
      }
    }
  }
}
