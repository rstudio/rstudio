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
import elemental.js.stylesheets.JsStyleSheet;
import elemental.js.dom.JsElement;
import elemental.dom.Element;
import elemental.html.StyleElement;
import elemental.stylesheets.StyleSheet;

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

public class JsStyleElement extends JsElement  implements StyleElement {
  protected JsStyleElement() {}

  public final native boolean isDisabled() /*-{
    return this.disabled;
  }-*/;

  public final native void setDisabled(boolean param_disabled) /*-{
    this.disabled = param_disabled;
  }-*/;

  public final native String getMedia() /*-{
    return this.media;
  }-*/;

  public final native void setMedia(String param_media) /*-{
    this.media = param_media;
  }-*/;

  public final native boolean isScoped() /*-{
    return this.scoped;
  }-*/;

  public final native void setScoped(boolean param_scoped) /*-{
    this.scoped = param_scoped;
  }-*/;

  public final native JsStyleSheet getSheet() /*-{
    return this.sheet;
  }-*/;

  public final native String getType() /*-{
    return this.type;
  }-*/;

  public final native void setType(String param_type) /*-{
    this.type = param_type;
  }-*/;
}
