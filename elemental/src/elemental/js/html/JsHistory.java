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
import elemental.html.History;

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

public class JsHistory extends JsElementalMixinBase  implements History {
  protected JsHistory() {}

  public final native int getLength() /*-{
    return this.length;
  }-*/;

  public final native Object getState() /*-{
    return this.state;
  }-*/;

  public final native void back() /*-{
    this.back();
  }-*/;

  public final native void forward() /*-{
    this.forward();
  }-*/;

  public final native void go(int distance) /*-{
    this.go(distance);
  }-*/;

  public final native void pushState(Object data, String title) /*-{
    this.pushState(data, title);
  }-*/;

  public final native void pushState(Object data, String title, String url) /*-{
    this.pushState(data, title, url);
  }-*/;

  public final native void replaceState(Object data, String title) /*-{
    this.replaceState(data, title);
  }-*/;

  public final native void replaceState(Object data, String title, String url) /*-{
    this.replaceState(data, title, url);
  }-*/;
}
