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
package elemental.json.impl;

import elemental.json.*;

/**
 * A {@link JsonContext} with String based location index.
 */
class JsonObjectContext extends JsonContext {

  String currentKey;

  public JsonObjectContext(JsonObject value) {
    super(value);
  }

  private JsonObject object() {
    return (JsonObject) getValue();
  }
  public String getCurrentKey() {
    return currentKey;
  }

  @Override
  public void removeMe() {
    object().remove(getCurrentKey());
  }

  @Override
  public void replaceMe(double d) {
    object().put(getCurrentKey(), d);
  }

  @Override
  public void replaceMe(String d) {
    object().put(getCurrentKey(), d);
  }

  @Override
  public void replaceMe(boolean d) {
    object().put(getCurrentKey(), d);
  }

  @Override
  public void replaceMe(JsonValue value) {
    object().put(getCurrentKey(), value);
  }

  public void setCurrentKey(String currentKey) {
    this.currentKey = currentKey;
  }
}
