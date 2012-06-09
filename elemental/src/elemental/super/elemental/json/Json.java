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
package elemental.json;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GwtScriptOnly;
import elemental.js.json.JsJsonFactory;
import elemental.json.impl.JreJsonFactory;

/**
 * Vends out implementation of JsonFactory.
 */
@GwtScriptOnly
public class Json {

  public static JsonString create(String string) {
    return instance().create(string);
  }

  public static JsonBoolean create(boolean bool) {
    return instance().create(bool);
  }

  public static JsonArray createArray() {
    return instance().createArray();
  }

  public static JsonNull createNull() {
    return instance().createNull();
  }

  public static JsonNumber create(double number) {
    return instance().create(number);
  }

  public static JsonObject createObject() {
    return instance().createObject();
  }

  public static JsonFactory instance() {
      assert GWT.isScript() : "Super-sourced Json class ran in DevMode";
      return new JsJsonFactory();
  }

  public static JsonObject parse(String jsonString) {
    return instance().parse(jsonString);
  }
}
