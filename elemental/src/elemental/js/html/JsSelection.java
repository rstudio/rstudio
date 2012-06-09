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
package elemental.js.html;
import elemental.dom.Node;
import elemental.ranges.Range;
import elemental.html.Selection;
import elemental.js.ranges.JsRange;
import elemental.js.dom.JsNode;

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

public class JsSelection extends JsElementalMixinBase  implements Selection {
  protected JsSelection() {}

  public final native JsNode getAnchorNode() /*-{
    return this.anchorNode;
  }-*/;

  public final native int getAnchorOffset() /*-{
    return this.anchorOffset;
  }-*/;

  public final native JsNode getBaseNode() /*-{
    return this.baseNode;
  }-*/;

  public final native int getBaseOffset() /*-{
    return this.baseOffset;
  }-*/;

  public final native JsNode getExtentNode() /*-{
    return this.extentNode;
  }-*/;

  public final native int getExtentOffset() /*-{
    return this.extentOffset;
  }-*/;

  public final native JsNode getFocusNode() /*-{
    return this.focusNode;
  }-*/;

  public final native int getFocusOffset() /*-{
    return this.focusOffset;
  }-*/;

  public final native boolean isCollapsed() /*-{
    return this.isCollapsed;
  }-*/;

  public final native int getRangeCount() /*-{
    return this.rangeCount;
  }-*/;

  public final native String getType() /*-{
    return this.type;
  }-*/;

  public final native void addRange(Range range) /*-{
    this.addRange(range);
  }-*/;

  public final native void collapse(Node node, int index) /*-{
    this.collapse(node, index);
  }-*/;

  public final native void collapseToEnd() /*-{
    this.collapseToEnd();
  }-*/;

  public final native void collapseToStart() /*-{
    this.collapseToStart();
  }-*/;

  public final native boolean containsNode(Node node, boolean allowPartial) /*-{
    return this.containsNode(node, allowPartial);
  }-*/;

  public final native void deleteFromDocument() /*-{
    this.deleteFromDocument();
  }-*/;

  public final native void empty() /*-{
    this.empty();
  }-*/;

  public final native void extend(Node node, int offset) /*-{
    this.extend(node, offset);
  }-*/;

  public final native JsRange getRangeAt(int index) /*-{
    return this.getRangeAt(index);
  }-*/;

  public final native void modify(String alter, String direction, String granularity) /*-{
    this.modify(alter, direction, granularity);
  }-*/;

  public final native void removeAllRanges() /*-{
    this.removeAllRanges();
  }-*/;

  public final native void selectAllChildren(Node node) /*-{
    this.selectAllChildren(node);
  }-*/;

  public final native void setBaseAndExtent(Node baseNode, int baseOffset, Node extentNode, int extentOffset) /*-{
    this.setBaseAndExtent(baseNode, baseOffset, extentNode, extentOffset);
  }-*/;

  public final native void setPosition(Node node, int offset) /*-{
    this.setPosition(node, offset);
  }-*/;
}
