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
import elemental.svg.SVGTextPositioningElement;
import elemental.svg.SVGAltGlyphElement;

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

public class JsSVGAltGlyphElement extends JsSVGTextPositioningElement  implements SVGAltGlyphElement {
  protected JsSVGAltGlyphElement() {}

  public final native String getFormat() /*-{
    return this.format;
  }-*/;

  public final native void setFormat(String param_format) /*-{
    this.format = param_format;
  }-*/;

  public final native String getGlyphRef() /*-{
    return this.glyphRef;
  }-*/;

  public final native void setGlyphRef(String param_glyphRef) /*-{
    this.glyphRef = param_glyphRef;
  }-*/;
}
