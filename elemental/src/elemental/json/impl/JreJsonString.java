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

import elemental.json.JsonString;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Server-side implementation of JsonString.
 */
public class JreJsonString extends JreJsonValue implements JsonString {

  private String string;

  public JreJsonString(String string) {
    this.string = string;
  }

  @Override
  public boolean asBoolean() {
    return !getString().isEmpty();
  }

  @Override
  public double asNumber() {
    try {
      if (asString().isEmpty()) {
        return 0.0;
      } else {
        return Double.parseDouble(asString());
      }
    } catch(NumberFormatException nfe) {
      return Double.NaN;
    }
  }

  @Override
  public String asString() {
    return getString();
  }

  public Object getObject() {
    return getString();
  }

  public String getString() {
    return string;
  }

  public JsonType getType() {
    return JsonType.STRING;
  }

  @Override
  public boolean jsEquals(JsonValue value) {
    return getObject().equals(((JreJsonValue)value).getObject());
  }

  @Override
  public void traverse(JsonVisitor visitor, JsonContext ctx) {
    visitor.visit(getString(), ctx);
  }

  public String toJson() throws IllegalStateException {
    return JsonUtil.quote(getString());
  }
}
