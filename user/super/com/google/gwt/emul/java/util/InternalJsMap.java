/*
 * Copyright 2015 Google Inc.
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
package java.util;

import com.google.gwt.core.client.JavaScriptObject;

class InternalJsMap<V> extends JavaScriptObject {
  protected InternalJsMap() { }
  public final native V get(int key) /*-{ return this.get(key); }-*/;
  public final native V get(String key) /*-{ return this.get(key); }-*/;
  public final native void set(int key, V value) /*-{ this.set(key, value); }-*/;
  public final native void set(String key, V value) /*-{ this.set(key, value); }-*/;
  // Calls delete via brackets to be workable with polyfills
  public final native void delete(int key) /*-{ this['delete'](key); }-*/;
  public final native void delete(String key) /*-{ this['delete'](key); }-*/;
  public final native InternalJsIterator<V> entries() /*-{ return this.entries(); }-*/;
}