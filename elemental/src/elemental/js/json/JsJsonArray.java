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
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.JsArrayString;

import elemental.js.util.JsArrayOf;
import elemental.json.JsonArray;
import elemental.json.JsonBoolean;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;
import elemental.json.JsonString;
import elemental.json.JsonValue;

/**
 * Client-side implementation of JsonArray.
 */
final public class JsJsonArray extends JsJsonValue
    implements JsonArray {

  public static JsonArray create() {
    return (JsJsonArray) JavaScriptObject.createArray();
  }

  protected JsJsonArray() {
  }

  public final native JsonValue get(int index) /*-{
    return this[index];
  }-*/;

  public JsonArray getArray(int index) {
    return (JsonArray) get(index);
  }

  public boolean getBoolean(int index) {
    return ((JsonBoolean) get(index)).getBoolean();
  }

  public double getNumber(int index) {
    return ((JsonNumber) get(index)).getNumber();
  }

  public JsonObject getObject(int index) {
    return (JsonObject) get(index);
  }

  public String getString(int index) {
    return ((JsonString) get(index)).getString();
  }

  @Override
  public native int length() /*-{
    return this.length;
  }-*/;

  @Override
  public void remove(int index) {
    this.<JsArrayOf>cast().removeByIndex(index);
  }

  public native void set(int index, JsonValue value) /*-{
       this[index] = value;
  }-*/;
  
  public void set(int index, String string) {
    asJsStringArray().set(index, string);
  }

  public native void set(int index, double number) /*-{
    this[index] = Object(number);
  }-*/;

  public native void set(int index, boolean bool) /*-{
    this[index] = Object(bool);
  }-*/;

  private JsArrayString asJsStringArray() {
    return this.cast();
  }
}
