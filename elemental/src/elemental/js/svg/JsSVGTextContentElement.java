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
package elemental.js.svg;
import elemental.svg.SVGRect;
import elemental.svg.SVGAnimatedEnumeration;
import elemental.svg.SVGPoint;
import elemental.svg.SVGAnimatedLength;
import elemental.svg.SVGElement;
import elemental.svg.SVGTextContentElement;

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

public class JsSVGTextContentElement extends JsSVGElement  implements SVGTextContentElement {
  protected JsSVGTextContentElement() {}

  public final native JsSVGAnimatedEnumeration getLengthAdjust() /*-{
    return this.lengthAdjust;
  }-*/;

  public final native JsSVGAnimatedLength getTextLength() /*-{
    return this.textLength;
  }-*/;

  public final native int getCharNumAtPosition(SVGPoint point) /*-{
    return this.getCharNumAtPosition(point);
  }-*/;

  public final native float getComputedTextLength() /*-{
    return this.getComputedTextLength();
  }-*/;

  public final native JsSVGPoint getEndPositionOfChar(int offset) /*-{
    return this.getEndPositionOfChar(offset);
  }-*/;

  public final native JsSVGRect getExtentOfChar(int offset) /*-{
    return this.getExtentOfChar(offset);
  }-*/;

  public final native int getNumberOfChars() /*-{
    return this.getNumberOfChars();
  }-*/;

  public final native float getRotationOfChar(int offset) /*-{
    return this.getRotationOfChar(offset);
  }-*/;

  public final native JsSVGPoint getStartPositionOfChar(int offset) /*-{
    return this.getStartPositionOfChar(offset);
  }-*/;

  public final native float getSubStringLength(int offset, int length) /*-{
    return this.getSubStringLength(offset, length);
  }-*/;

  public final native void selectSubString(int offset, int length) /*-{
    this.selectSubString(offset, length);
  }-*/;
}
