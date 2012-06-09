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
package elemental.js.stylesheets;
import elemental.dom.Node;
import elemental.stylesheets.StyleSheet;
import elemental.stylesheets.MediaList;
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

public class JsStyleSheet extends JsElementalMixinBase  implements StyleSheet {
  protected JsStyleSheet() {}

  public final native boolean isDisabled() /*-{
    return this.disabled;
  }-*/;

  public final native void setDisabled(boolean param_disabled) /*-{
    this.disabled = param_disabled;
  }-*/;

  public final native String getHref() /*-{
    return this.href;
  }-*/;

  public final native JsMediaList getMedia() /*-{
    return this.media;
  }-*/;

  public final native JsNode getOwnerNode() /*-{
    return this.ownerNode;
  }-*/;

  public final native JsStyleSheet getParentStyleSheet() /*-{
    return this.parentStyleSheet;
  }-*/;

  public final native String getTitle() /*-{
    return this.title;
  }-*/;

  public final native String getType() /*-{
    return this.type;
  }-*/;
}
