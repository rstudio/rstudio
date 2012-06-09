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

import elemental.json.JsonNull;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Server-side implementation of JsonObject.
 */
public class JreJsonNull extends JreJsonValue implements JsonNull {

  public static final JsonNull NULL_INSTANCE = new JreJsonNull();

  @Override
  public double asNumber() {
    return 0;
  }

  @Override
  public boolean asBoolean() {
    return false;
  }

  @Override
  public String asString() {
    return "null";
  }

  public Object getObject() {
    return null;
  }

  public JsonType getType() {
    return JsonType.NULL;
  }

  @Override
  public boolean jsEquals(JsonValue value) {
    return value == null || value.getType() == JsonType.NULL;
  }

  @Override
  public void traverse(JsonVisitor visitor, elemental.json.impl.JsonContext ctx) {
    visitor.visitNull(ctx);
  }

  public String toJson() {
    return null;
  }
}
