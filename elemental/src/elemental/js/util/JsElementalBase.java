/*
 * Copyright 2010 Google Inc.
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
package elemental.js.util;

import com.google.gwt.core.client.JavaScriptObject;
import elemental.util.*;

/**
 * All Elemental classes must extend this base class, mixes in support for
 * Indexable, Settable, Mappable.
 */
// TODO (cromwellian) add generic when JSO bug in gwt-dev fixed
public class JsElementalBase extends JavaScriptObject implements Mappable, 
    Indexable, IndexableInt, IndexableNumber, Settable, SettableInt, SettableNumber {

  protected JsElementalBase() {}

  public final native Object /* T */ at(int index) /*-{
    return this[index];
  }-*/;

  public final native double numberAt(int index) /*-{
    return this[index];
  }-*/;

  public final native int intAt(int index) /*-{
    return this[index];
  }-*/;

  public final native int length() /*-{
    return this.length;
  }-*/;

  public final native void setAt(int index, Object /* T */ value) /*-{
    this[index] = value;
  }-*/;

  public final native void setAt(int index, double value) /*-{
    this[index] = value;
  }-*/;

  public final native void setAt(int index, int value) /*-{
    this[index] = value;
  }-*/;

  public final native Object /* T */ at(String key) /*-{
    return this[key];
  }-*/;

  public final native int intAt(String key) /*-{
    return this[key];
  }-*/;

  public final native double numberAt(String key) /*-{
    return this[key];
  }-*/;

  public final native void setAt(String key, Object /* T */ value) /*-{
    this[key] = value;
  }-*/;

  public final native void setAt(String key, int value) /*-{
    this[key] = value;
  }-*/;

  public final native void setAt(String key, double value) /*-{
    this[key] = value;
  }-*/;
}
