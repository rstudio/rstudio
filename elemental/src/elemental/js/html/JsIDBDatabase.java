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
import elemental.util.Indexable;
import elemental.js.util.JsIndexable;
import elemental.html.IDBObjectStore;
import elemental.js.util.JsMappable;
import elemental.html.IDBVersionChangeRequest;
import elemental.html.IDBTransaction;
import elemental.util.Mappable;
import elemental.events.EventListener;
import elemental.js.events.JsEvent;
import elemental.html.IDBDatabase;
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

public class JsIDBDatabase extends JsElementalMixinBase  implements IDBDatabase {
  protected JsIDBDatabase() {}

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native JsIndexable getObjectStoreNames() /*-{
    return this.objectStoreNames;
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
  public final native EventListener getOnversionchange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onversionchange);
  }-*/;

  public final native void setOnversionchange(EventListener listener) /*-{
    this.onversionchange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native String getVersion() /*-{
    return this.version;
  }-*/;

  public final native void close() /*-{
    this.close();
  }-*/;

  public final native JsIDBObjectStore createObjectStore(String name) /*-{
    return this.createObjectStore(name);
  }-*/;

  public final native JsIDBObjectStore createObjectStore(String name, Mappable options) /*-{
    return this.createObjectStore(name, options);
  }-*/;

  public final native void deleteObjectStore(String name) /*-{
    this.deleteObjectStore(name);
  }-*/;

  public final native JsIDBTransaction transaction(Indexable storeNames) /*-{
    return this.transaction(storeNames);
  }-*/;

  public final native JsIDBTransaction transaction(Indexable storeNames, String mode) /*-{
    return this.transaction(storeNames, mode);
  }-*/;

  public final native JsIDBTransaction transaction(String storeName) /*-{
    return this.transaction(storeName);
  }-*/;

  public final native JsIDBTransaction transaction(String storeName, String mode) /*-{
    return this.transaction(storeName, mode);
  }-*/;

  public final native JsIDBTransaction transaction(Indexable storeNames, int mode) /*-{
    return this.transaction(storeNames, mode);
  }-*/;

  public final native JsIDBTransaction transaction(String storeName, int mode) /*-{
    return this.transaction(storeName, mode);
  }-*/;
}
