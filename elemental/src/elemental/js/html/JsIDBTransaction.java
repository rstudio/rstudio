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
import elemental.html.IDBObjectStore;
import elemental.js.events.JsEvent;
import elemental.dom.DOMError;
import elemental.events.EventListener;
import elemental.html.IDBTransaction;
import elemental.html.IDBDatabase;
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

public class JsIDBTransaction extends JsElementalMixinBase  implements IDBTransaction {
  protected JsIDBTransaction() {}

  public final native JsIDBDatabase getDb() /*-{
    return this.db;
  }-*/;

  public final native JsDOMError getError() /*-{
    return this.error;
  }-*/;

  public final native String getMode() /*-{
    return this.mode;
  }-*/;

  public final native EventListener getOnabort() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onabort);
  }-*/;

  public final native void setOnabort(EventListener listener) /*-{
    this.onabort = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOncomplete() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.oncomplete);
  }-*/;

  public final native void setOncomplete(EventListener listener) /*-{
    this.oncomplete = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnerror() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onerror);
  }-*/;

  public final native void setOnerror(EventListener listener) /*-{
    this.onerror = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native void abort() /*-{
    this.abort();
  }-*/;

  public final native JsIDBObjectStore objectStore(String name) /*-{
    return this.objectStore(name);
  }-*/;
}
