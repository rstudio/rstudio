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
import elemental.html.WebGLContextAttributes;

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

public class JsWebGLContextAttributes extends JsElementalMixinBase  implements WebGLContextAttributes {
  protected JsWebGLContextAttributes() {}

  public final native boolean isAlpha() /*-{
    return this.alpha;
  }-*/;

  public final native void setAlpha(boolean param_alpha) /*-{
    this.alpha = param_alpha;
  }-*/;

  public final native boolean isAntialias() /*-{
    return this.antialias;
  }-*/;

  public final native void setAntialias(boolean param_antialias) /*-{
    this.antialias = param_antialias;
  }-*/;

  public final native boolean isDepth() /*-{
    return this.depth;
  }-*/;

  public final native void setDepth(boolean param_depth) /*-{
    this.depth = param_depth;
  }-*/;

  public final native boolean isPremultipliedAlpha() /*-{
    return this.premultipliedAlpha;
  }-*/;

  public final native void setPremultipliedAlpha(boolean param_premultipliedAlpha) /*-{
    this.premultipliedAlpha = param_premultipliedAlpha;
  }-*/;

  public final native boolean isPreserveDrawingBuffer() /*-{
    return this.preserveDrawingBuffer;
  }-*/;

  public final native void setPreserveDrawingBuffer(boolean param_preserveDrawingBuffer) /*-{
    this.preserveDrawingBuffer = param_preserveDrawingBuffer;
  }-*/;

  public final native boolean isStencil() /*-{
    return this.stencil;
  }-*/;

  public final native void setStencil(boolean param_stencil) /*-{
    this.stencil = param_stencil;
  }-*/;
}
