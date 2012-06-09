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
import elemental.events.EventListener;
import elemental.html.WebSocket;
import elemental.js.events.JsEvent;
import elemental.js.events.JsEventListener;
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

public class JsWebSocket extends JsElementalMixinBase  implements WebSocket {
  protected JsWebSocket() {}

  public final native String getURL() /*-{
    return this.URL;
  }-*/;

  public final native String getBinaryType() /*-{
    return this.binaryType;
  }-*/;

  public final native void setBinaryType(String param_binaryType) /*-{
    this.binaryType = param_binaryType;
  }-*/;

  public final native int getBufferedAmount() /*-{
    return this.bufferedAmount;
  }-*/;

  public final native String getExtensions() /*-{
    return this.extensions;
  }-*/;

  public final native EventListener getOnclose() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onclose);
  }-*/;

  public final native void setOnclose(EventListener listener) /*-{
    this.onclose = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnerror() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onerror);
  }-*/;

  public final native void setOnerror(EventListener listener) /*-{
    this.onerror = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnmessage() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onmessage);
  }-*/;

  public final native void setOnmessage(EventListener listener) /*-{
    this.onmessage = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnopen() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onopen);
  }-*/;

  public final native void setOnopen(EventListener listener) /*-{
    this.onopen = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native String getProtocol() /*-{
    return this.protocol;
  }-*/;

  public final native int getReadyState() /*-{
    return this.readyState;
  }-*/;

  public final native String getUrl() /*-{
    return this.url;
  }-*/;

  public final native void close() /*-{
    this.close();
  }-*/;

  public final native void close(int code) /*-{
    this.close(code);
  }-*/;

  public final native void close(int code, String reason) /*-{
    this.close(code, reason);
  }-*/;

  public final native boolean send(String data) /*-{
    return this.send(data);
  }-*/;
}
