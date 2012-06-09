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
import elemental.js.css.JsCSSValue;
import elemental.css.RGBColor;
import elemental.svg.SVGColor;
import elemental.js.css.JsRGBColor;
import elemental.css.CSSValue;

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

public class JsSVGColor extends JsCSSValue  implements SVGColor {
  protected JsSVGColor() {}

  public final native int getColorType() /*-{
    return this.colorType;
  }-*/;

  public final native JsRGBColor getRgbColor() /*-{
    return this.rgbColor;
  }-*/;

  public final native void setColor(int colorType, String rgbColor, String iccColor) /*-{
    this.setColor(colorType, rgbColor, iccColor);
  }-*/;

  public final native void setRGBColor(String rgbColor) /*-{
    this.setRGBColor(rgbColor);
  }-*/;

  public final native void setRGBColorICCColor(String rgbColor, String iccColor) /*-{
    this.setRGBColorICCColor(rgbColor, iccColor);
  }-*/;
}
