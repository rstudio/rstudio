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
import elemental.js.events.JsEvent;
import elemental.dom.DOMError;
import elemental.events.EventListener;
import elemental.html.IDBTransaction;
import elemental.html.IDBRequest;
import elemental.js.dom.JsDOMError;
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

public class JsIDBRequest extends JsElementalMixinBase  implements IDBRequest {
  protected JsIDBRequest() {}

  public final native JsDOMError getError() /*-{
    return this.error;
  }-*/;

  public final native int getErrorCode() /*-{
    return this.errorCode;
  }-*/;

  public final native EventListener getOnerror() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onerror);
  }-*/;

  public final native void setOnerror(EventListener listener) /*-{
    this.onerror = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnsuccess() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onsuccess);
  }-*/;

  public final native void setOnsuccess(EventListener listener) /*-{
    this.onsuccess = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native String getReadyState() /*-{
    return this.readyState;
  }-*/;

  public final native Object getResult() /*-{
    return this.result;
  }-*/;

  public final native Object getSource() /*-{
    return this.source;
  }-*/;

  public final native JsIDBTransaction getTransaction() /*-{
    return this.transaction;
  }-*/;

  public final native String getWebkitErrorMessage() /*-{
    return this.webkitErrorMessage;
  }-*/;
}
