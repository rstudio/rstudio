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
package elemental.js.events;
import elemental.util.Indexable;
import elemental.js.html.JsWindow;
import elemental.events.MessageEvent;
import elemental.js.util.JsIndexable;
import elemental.html.Window;
import elemental.events.Event;

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

public class JsMessageEvent extends JsEvent  implements MessageEvent {
  protected JsMessageEvent() {}

  public final native Object getData() /*-{
    return this.data;
  }-*/;

  public final native String getLastEventId() /*-{
    return this.lastEventId;
  }-*/;

  public final native String getOrigin() /*-{
    return this.origin;
  }-*/;

  public final native JsIndexable getPorts() /*-{
    return this.ports;
  }-*/;

  public final native JsWindow getSource() /*-{
    return this.source;
  }-*/;

  public final native void initMessageEvent(String typeArg, boolean canBubbleArg, boolean cancelableArg, Object dataArg, String originArg, String lastEventIdArg, Window sourceArg, Indexable messagePorts) /*-{
    this.initMessageEvent(typeArg, canBubbleArg, cancelableArg, dataArg, originArg, lastEventIdArg, sourceArg, messagePorts);
  }-*/;

  public final native void webkitInitMessageEvent(String typeArg, boolean canBubbleArg, boolean cancelableArg, Object dataArg, String originArg, String lastEventIdArg, Window sourceArg, Indexable transferables) /*-{
    this.webkitInitMessageEvent(typeArg, canBubbleArg, cancelableArg, dataArg, originArg, lastEventIdArg, sourceArg, transferables);
  }-*/;
}
