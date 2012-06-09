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
import elemental.dom.Element;
import elemental.html.MetaElement;

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

public class JsMetaElement extends JsElement  implements MetaElement {
  protected JsMetaElement() {}

  public final native String getContent() /*-{
    return this.content;
  }-*/;

  public final native void setContent(String param_content) /*-{
    this.content = param_content;
  }-*/;

  public final native String getHttpEquiv() /*-{
    return this.httpEquiv;
  }-*/;

  public final native void setHttpEquiv(String param_httpEquiv) /*-{
    this.httpEquiv = param_httpEquiv;
  }-*/;

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native void setName(String param_name) /*-{
    this.name = param_name;
  }-*/;

  public final native String getScheme() /*-{
    return this.scheme;
  }-*/;

  public final native void setScheme(String param_scheme) /*-{
    this.scheme = param_scheme;
  }-*/;
}
