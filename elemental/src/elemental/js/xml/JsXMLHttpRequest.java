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
package elemental.js.xml;
import elemental.html.FormData;
import elemental.js.html.JsFormData;
import elemental.xml.XMLHttpRequest;
import elemental.js.dom.JsDocument;
import elemental.js.html.JsArrayBuffer;
import elemental.js.events.JsEvent;
import elemental.html.Blob;
import elemental.js.html.JsBlob;
import elemental.events.EventListener;
import elemental.xml.XMLHttpRequestUpload;
import elemental.html.ArrayBuffer;
import elemental.events.Event;
import elemental.js.events.JsEventListener;
import elemental.dom.Document;

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

public class JsXMLHttpRequest extends JsElementalMixinBase  implements XMLHttpRequest {
  protected JsXMLHttpRequest() {}

  public final native boolean isAsBlob() /*-{
    return this.asBlob;
  }-*/;

  public final native void setAsBlob(boolean param_asBlob) /*-{
    this.asBlob = param_asBlob;
  }-*/;

  public final native EventListener getOnabort() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onabort);
  }-*/;

  public final native void setOnabort(EventListener listener) /*-{
    this.onabort = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnerror() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onerror);
  }-*/;

  public final native void setOnerror(EventListener listener) /*-{
    this.onerror = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnload() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onload);
  }-*/;

  public final native void setOnload(EventListener listener) /*-{
    this.onload = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnloadend() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onloadend);
  }-*/;

  public final native void setOnloadend(EventListener listener) /*-{
    this.onloadend = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnloadstart() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onloadstart);
  }-*/;

  public final native void setOnloadstart(EventListener listener) /*-{
    this.onloadstart = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnprogress() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onprogress);
  }-*/;

  public final native void setOnprogress(EventListener listener) /*-{
    this.onprogress = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnreadystatechange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onreadystatechange);
  }-*/;

  public final native void setOnreadystatechange(EventListener listener) /*-{
    this.onreadystatechange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native int getReadyState() /*-{
    return this.readyState;
  }-*/;

  public final native Object getResponse() /*-{
    return this.response;
  }-*/;

  public final native JsBlob getResponseBlob() /*-{
    return this.responseBlob;
  }-*/;

  public final native String getResponseText() /*-{
    return this.responseText;
  }-*/;

  public final native String getResponseType() /*-{
    return this.responseType;
  }-*/;

  public final native void setResponseType(String param_responseType) /*-{
    this.responseType = param_responseType;
  }-*/;

  public final native JsDocument getResponseXML() /*-{
    return this.responseXML;
  }-*/;

  public final native int getStatus() /*-{
    return this.status;
  }-*/;

  public final native String getStatusText() /*-{
    return this.statusText;
  }-*/;

  public final native JsXMLHttpRequestUpload getUpload() /*-{
    return this.upload;
  }-*/;

  public final native boolean isWithCredentials() /*-{
    return this.withCredentials;
  }-*/;

  public final native void setWithCredentials(boolean param_withCredentials) /*-{
    this.withCredentials = param_withCredentials;
  }-*/;

  public final native void abort() /*-{
    this.abort();
  }-*/;

  public final native String getAllResponseHeaders() /*-{
    return this.getAllResponseHeaders();
  }-*/;

  public final native String getResponseHeader(String header) /*-{
    return this.getResponseHeader(header);
  }-*/;

  public final native void open(String method, String url) /*-{
    this.open(method, url);
  }-*/;

  public final native void open(String method, String url, boolean async) /*-{
    this.open(method, url, async);
  }-*/;

  public final native void open(String method, String url, boolean async, String user) /*-{
    this.open(method, url, async, user);
  }-*/;

  public final native void open(String method, String url, boolean async, String user, String password) /*-{
    this.open(method, url, async, user, password);
  }-*/;

  public final native void overrideMimeType(String override) /*-{
    this.overrideMimeType(override);
  }-*/;

  public final native void send() /*-{
    this.send();
  }-*/;

  public final native void send(ArrayBuffer data) /*-{
    this.send(data);
  }-*/;

  public final native void send(Blob data) /*-{
    this.send(data);
  }-*/;

  public final native void send(Document data) /*-{
    this.send(data);
  }-*/;

  public final native void send(String data) /*-{
    this.send(data);
  }-*/;

  public final native void send(FormData data) /*-{
    this.send(data);
  }-*/;

  public final native void setRequestHeader(String header, String value) /*-{
    this.setRequestHeader(header, value);
  }-*/;
}
