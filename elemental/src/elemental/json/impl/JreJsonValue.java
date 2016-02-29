/*
 * Copyright 2011 Google Inc.
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

import java.io.IOException;
import java.io.ObjectInputStream;

import elemental.json.Json;
import elemental.json.JsonValue;

/**
 * JRE (non-Client) implementation of JreJsonValue.
 */
public abstract class JreJsonValue implements JsonValue {
  public abstract Object getObject();
  public abstract void traverse(JsonVisitor visitor, JsonContext ctx);

  @Override
  public Object toNative() {
    return this;
  }

  protected static <T extends JsonValue> T parseJson(ObjectInputStream stream)
    throws ClassNotFoundException, IOException {
    String jsonString = (String) stream.readObject();
    return Json.instance().parse(jsonString);
  }
}
