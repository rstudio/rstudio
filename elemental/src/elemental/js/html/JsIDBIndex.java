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
import elemental.html.IDBKeyRange;
import elemental.html.IDBObjectStore;
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

public class JsIDBIndex extends JsElementalMixinBase  implements IDBIndex {
  protected JsIDBIndex() {}

  public final native Object getKeyPath() /*-{
    return this.keyPath;
  }-*/;

  public final native boolean isMultiEntry() /*-{
    return this.multiEntry;
  }-*/;

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native JsIDBObjectStore getObjectStore() /*-{
    return this.objectStore;
  }-*/;

  public final native boolean isUnique() /*-{
    return this.unique;
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

  public final native JsIDBRequest get(IDBKeyRange key) /*-{
    return this.get(key);
  }-*/;

  public final native JsIDBRequest getObject(Object key) /*-{
    return this.getObject(key);
  }-*/;

  public final native JsIDBRequest getKey(IDBKeyRange key) /*-{
    return this.getKey(key);
  }-*/;

  public final native JsIDBRequest getKey(Object key) /*-{
    return this.getKey(key);
  }-*/;

  public final native JsIDBRequest openCursor() /*-{
    return this.openCursor();
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

  public final native JsIDBRequest openKeyCursor() /*-{
    return this.openKeyCursor();
  }-*/;

  public final native JsIDBRequest openKeyCursor(IDBKeyRange range) /*-{
    return this.openKeyCursor(range);
  }-*/;

  public final native JsIDBRequest openKeyCursor(IDBKeyRange range, String direction) /*-{
    return this.openKeyCursor(range, direction);
  }-*/;

  public final native JsIDBRequest openKeyCursor(Object key) /*-{
    return this.openKeyCursor(key);
  }-*/;

  public final native JsIDBRequest openKeyCursor(Object key, String direction) /*-{
    return this.openKeyCursor(key, direction);
  }-*/;

  public final native JsIDBRequest openKeyCursor(IDBKeyRange range, int direction) /*-{
    return this.openKeyCursor(range, direction);
  }-*/;

  public final native JsIDBRequest openKeyCursor(Object key, int direction) /*-{
    return this.openKeyCursor(key, direction);
  }-*/;
}
