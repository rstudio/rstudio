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
package elemental.js.json;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import elemental.json.JsonArray;
import elemental.json.JsonBoolean;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;
import elemental.json.JsonString;
import elemental.json.JsonValue;

/**
 * Client-side implementation of JsonObject interface.
 */
final public class JsJsonObject extends JsJsonValue
    implements JsonObject {

  public static JsJsonObject create() {
    return JavaScriptObject.createObject().cast();
  }

  protected JsJsonObject() {
  }

  public final native JsonValue get(String key) /*-{
    return @com.google.gwt.core.client.GWT::isScript()() ?
            @elemental.js.json.JsJsonValue::box(Lelemental/json/JsonValue;)(this[key]) :
            this[key] != null ? Object(this[key]) : null;
  }-*/;

  public JsonArray getArray(String key) {
    return (JsonArray) get(key);
  }

  public boolean getBoolean(String key) {
    return ((JsonBoolean) get(key)).getBoolean();
  }


  public double getNumber(String key) {
    return ((JsonNumber) get(key)).getNumber();
  }

  public JsonObject getObject(String key) {
    return (JsonObject) get(key);
  }

  public String getString(String key) {
    return ((JsonString) get(key)).getString();
  }

  public native boolean hasKey(String key) /*-{
    return key in this;
  }-*/;

  public String[] keys() {
    JsArrayString str = keys0();
    return reinterpretCast(str);
  }

  public native JsArrayString keys0() /*-{
    var keys = [];
    for(var key in this) {
      if (this.hasOwnProperty(key) && key != '$H') {
        keys.push(key);
      }
    }
    return keys;
  }-*/;

  public void put(String key, JsonValue value) {
    put0(key, value);
  }

  public native void put(String key, String value)  /*-{
    this[key] = value;
  }-*/;

  public native void put(String key, double value) /*-{
    this[key] = Object(value);
  }-*/;

  public native void put(String key, boolean value) /*-{
    this[key] = Object(value);
  }-*/;

  public native void put0(String key, JsonValue value)  /*-{
    this[key] = value;
  }-*/;

  public native void remove(String key) /*-{
    delete this[key];
  }-*/;

  private native String[] reinterpretCast(JsArrayString arrayString) /*-{
    return arrayString;
  }-*/;
}
