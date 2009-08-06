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
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.UIObject;

/**
 * Static helper methods used by UiBinder. These methods are likely to move,
 * so please don't use them for non-UiBinder code.
 */
public class UiBinderUtil {
  private static Element hiddenDiv;

  public static Element attachToDomAndGetChild(Element element, String id) {
    // TODO(rjrjr) This is copied from HTMLPanel. Reconcile
    ensureHiddenDiv();

    // Hang on to the panel's original parent and sibling elements so that it
    // can be replaced.
    Element origParent = element.getParentElement();
    Element origSibling = element.getNextSiblingElement();

    // Attach the panel's element to the hidden div.
    hiddenDiv.appendChild(element);

    // Now that we're attached to the DOM, we can use getElementById.
    Element child = Document.get().getElementById(id);

    // Put the panel's element back where it was.
    if (origParent != null) {
      origParent.insertBefore(element, origSibling);
    } else {
      orphan(element);
    }

    return child;
  }

  public static Element fromHtml(String html) {
    ensureHiddenDiv();
    hiddenDiv.setInnerHTML(html);
    Element newbie = hiddenDiv.getFirstChildElement();
    orphan(newbie);
    return newbie;
  }

  private static void ensureHiddenDiv() {
    // If the hidden DIV has not been created, create it.
    if (hiddenDiv == null) {
      hiddenDiv = Document.get().createDivElement();
      UIObject.setVisible(hiddenDiv, false);
      RootPanel.getBodyElement().appendChild(hiddenDiv);
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
