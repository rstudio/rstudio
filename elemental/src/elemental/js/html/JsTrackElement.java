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
import elemental.js.dom.JsElement;
import elemental.html.TrackElement;
import elemental.dom.Element;
import elemental.html.TextTrack;

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

public class JsTrackElement extends JsElement  implements TrackElement {
  protected JsTrackElement() {}

  public final native boolean isDefaultValue() /*-{
    return this['default'];
  }-*/;

  public final native void setDefaultValue(boolean param_default) /*-{
    this['default'] = param_default;
  }-*/;

  public final native String getKind() /*-{
    return this.kind;
  }-*/;

  public final native void setKind(String param_kind) /*-{
    this.kind = param_kind;
  }-*/;

  public final native String getLabel() /*-{
    return this.label;
  }-*/;

  public final native void setLabel(String param_label) /*-{
    this.label = param_label;
  }-*/;

  public final native int getReadyState() /*-{
    return this.readyState;
  }-*/;

  public final native String getSrc() /*-{
    return this.src;
  }-*/;

  public final native void setSrc(String param_src) /*-{
    this.src = param_src;
  }-*/;

  public final native String getSrclang() /*-{
    return this.srclang;
  }-*/;

  public final native void setSrclang(String param_srclang) /*-{
    this.srclang = param_srclang;
  }-*/;

  public final native JsTextTrack getTrack() /*-{
    return this.track;
  }-*/;
}
