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
import elemental.html.MediaKeyError;
import elemental.events.Event;
import elemental.js.events.JsEvent;
import elemental.html.MediaKeyEvent;
import elemental.html.Uint8Array;

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

public class JsMediaKeyEvent extends JsEvent  implements MediaKeyEvent {
  protected JsMediaKeyEvent() {}

  public final native String getDefaultURL() /*-{
    return this.defaultURL;
  }-*/;

  public final native JsMediaKeyError getErrorCode() /*-{
    return this.errorCode;
  }-*/;

  public final native JsUint8Array getInitData() /*-{
    return this.initData;
  }-*/;

  public final native String getKeySystem() /*-{
    return this.keySystem;
  }-*/;

  public final native JsUint8Array getMessage() /*-{
    return this.message;
  }-*/;

  public final native String getSessionId() /*-{
    return this.sessionId;
  }-*/;

  public final native int getSystemCode() /*-{
    return this.systemCode;
  }-*/;
}
