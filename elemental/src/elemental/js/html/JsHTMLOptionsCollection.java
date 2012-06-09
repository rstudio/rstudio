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
import elemental.html.HTMLOptionsCollection;
import elemental.html.HTMLCollection;

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

public class JsHTMLOptionsCollection extends JsHTMLCollection  implements HTMLOptionsCollection {
  protected JsHTMLOptionsCollection() {}

  public final native void setLength(int param_length) /*-{
    this.length = param_length;
  }-*/;

  public final native int getSelectedIndex() /*-{
    return this.selectedIndex;
  }-*/;

  public final native void setSelectedIndex(int param_selectedIndex) /*-{
    this.selectedIndex = param_selectedIndex;
  }-*/;

  public final native void remove(int index) /*-{
    this.remove(index);
  }-*/;
}
