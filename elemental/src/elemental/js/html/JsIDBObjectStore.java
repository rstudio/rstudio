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
import elemental.util.Mappable;
import elemental.html.IDBKeyRange;
import elemental.html.IDBTransaction;
import elemental.html.IDBRequest;
import elemental.html.IDBIndex;

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

public class JsIDBObjectStore extends JsElementalMixinBase  implements IDBObjectStore {
  protected JsIDBObjectStore() {}

  public final native boolean isAutoIncrement() /*-{
    return this.autoIncrement;
  }-*/;

  public final native JsIndexable getIndexNames() /*-{
    return this.indexNames;
  }-*/;

  public final native Object getKeyPath() /*-{
    return this.keyPath;
  }-*/;

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native JsIDBTransaction getTransaction() /*-{
    return this.transaction;
  }-*/;

  public final native JsIDBRequest add(Object value) /*-{
    return this.add(value);
  }-*/;

  public final native JsIDBRequest add(Object value, Object key) /*-{
    return this.add(value, key);
  }-*/;

  public final native JsIDBRequest clear() /*-{
    return this.clear();
  }-*/;

  public final native JsIDBRequest count() /*-{
    return this.count();
  }-*/;

  public final native JsIDBRequest count(IDBKeyRange range) /*-{
    return this.count(range);
  }-*/;

  public final native JsIDBRequest count(Object key) /*-{
    return this.count(key);
  }-*/;

  public final native JsIDBIndex createIndex(String name, String keyPath) /*-{
    return this.createIndex(name, keyPath);
  }-*/;

  public final native JsIDBIndex createIndex(String name, String keyPath, Mappable options) /*-{
    return this.createIndex(name, keyPath, options);
  }-*/;

  public final native JsIDBRequest _delete(IDBKeyRange keyRange) /*-{
    return this['delete'](keyRange);
  }-*/;

  public final native JsIDBRequest _delete(Object key) /*-{
    return this['delete'](key);
  }-*/;

  public final native void deleteIndex(String name) /*-{
    this.deleteIndex(name);
  }-*/;

  public final native JsIDBRequest getObject(IDBKeyRange key) /*-{
    return this.getObject(key);
  }-*/;

  public final native JsIDBRequest getObject(Object key) /*-{
    return this.getObject(key);
  }-*/;

  public final native JsIDBIndex index(String name) /*-{
    return this.index(name);
  }-*/;

  public final native JsIDBRequest openCursor(IDBKeyRange range) /*-{
    return this.openCursor(range);
  }-*/;

  public final native JsIDBRequest openCursor(IDBKeyRange range, String direction) /*-{
    return this.openCursor(range, direction);
  }-*/;

  public final native JsIDBRequest openCursor(Object key) /*-{
    return this.openCursor(key);
  }-*/;

  public final native JsIDBRequest openCursor(Object key, String direction) /*-{
    return this.openCursor(key, direction);
  }-*/;

  public final native JsIDBRequest openCursor(IDBKeyRange range, int direction) /*-{
    return this.openCursor(range, direction);
  }-*/;

  public final native JsIDBRequest openCursor(Object key, int direction) /*-{
    return this.openCursor(key, direction);
  }-*/;

  public final native JsIDBRequest put(Object value) /*-{
    return this.put(value);
  }-*/;

  public final native JsIDBRequest put(Object value, Object key) /*-{
    return this.put(value, key);
  }-*/;
}
