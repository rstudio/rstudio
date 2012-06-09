/*
 * Copyright 2012 Google Inc.
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
package elemental.js.ranges;
import elemental.dom.Node;
import elemental.js.dom.JsDocumentFragment;
import elemental.dom.DocumentFragment;
import elemental.js.html.JsClientRect;
import elemental.ranges.Range;
import elemental.js.html.JsClientRectList;
import elemental.html.ClientRect;
import elemental.js.dom.JsNode;
import elemental.html.ClientRectList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.js.stylesheets.*;
import elemental.js.events.*;
import elemental.js.util.*;
import elemental.js.dom.*;
import elemental.js.html.*;
import elemental.js.css.*;
import elemental.js.stylesheets.*;

import java.util.Date;

public class JsRange extends JsElementalMixinBase  implements Range {
  protected JsRange() {}

  public final native boolean isCollapsed() /*-{
    return this.collapsed;
  }-*/;

  public final native JsNode getCommonAncestorContainer() /*-{
    return this.commonAncestorContainer;
  }-*/;

  public final native JsNode getEndContainer() /*-{
    return this.endContainer;
  }-*/;

  public final native int getEndOffset() /*-{
    return this.endOffset;
  }-*/;

  public final native JsNode getStartContainer() /*-{
    return this.startContainer;
  }-*/;

  public final native int getStartOffset() /*-{
    return this.startOffset;
  }-*/;

  public final native JsDocumentFragment cloneContents() /*-{
    return this.cloneContents();
  }-*/;

  public final native JsRange cloneRange() /*-{
    return this.cloneRange();
  }-*/;

  public final native void collapse(boolean toStart) /*-{
    this.collapse(toStart);
  }-*/;

  public final native short compareNode(Node refNode) /*-{
    return this.compareNode(refNode);
  }-*/;

  public final native short comparePoint(Node refNode, int offset) /*-{
    return this.comparePoint(refNode, offset);
  }-*/;

  public final native JsDocumentFragment createContextualFragment(String html) /*-{
    return this.createContextualFragment(html);
  }-*/;

  public final native void deleteContents() /*-{
    this.deleteContents();
  }-*/;

  public final native void detach() /*-{
    this.detach();
  }-*/;

  public final native void expand(String unit) /*-{
    this.expand(unit);
  }-*/;

  public final native JsDocumentFragment extractContents() /*-{
    return this.extractContents();
  }-*/;

  public final native JsClientRect getBoundingClientRect() /*-{
    return this.getBoundingClientRect();
  }-*/;

  public final native JsClientRectList getClientRects() /*-{
    return this.getClientRects();
  }-*/;

  public final native void insertNode(Node newNode) /*-{
    this.insertNode(newNode);
  }-*/;

  public final native boolean intersectsNode(Node refNode) /*-{
    return this.intersectsNode(refNode);
  }-*/;

  public final native boolean isPointInRange(Node refNode, int offset) /*-{
    return this.isPointInRange(refNode, offset);
  }-*/;

  public final native void selectNode(Node refNode) /*-{
    this.selectNode(refNode);
  }-*/;

  public final native void selectNodeContents(Node refNode) /*-{
    this.selectNodeContents(refNode);
  }-*/;

  public final native void setEnd(Node refNode, int offset) /*-{
    this.setEnd(refNode, offset);
  }-*/;

  public final native void setEndAfter(Node refNode) /*-{
    this.setEndAfter(refNode);
  }-*/;

  public final native void setEndBefore(Node refNode) /*-{
    this.setEndBefore(refNode);
  }-*/;

  public final native void setStart(Node refNode, int offset) /*-{
    this.setStart(refNode, offset);
  }-*/;

  public final native void setStartAfter(Node refNode) /*-{
    this.setStartAfter(refNode);
  }-*/;

  public final native void setStartBefore(Node refNode) /*-{
    this.setStartBefore(refNode);
  }-*/;

  public final native void surroundContents(Node newParent) /*-{
    this.surroundContents(newParent);
  }-*/;
}
