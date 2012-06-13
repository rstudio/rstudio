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

import elemental.json.JsonNull;

/**
 * Client-side implementation of JsonNull as "extension methods" on an actual
 * null.
 */
final public class JsJsonNull extends JsJsonValue
    implements JsonNull {

  public static JsonNull create() {
    return createProd();
  }

  /*
   * MAGIC: If the implementation of JSOs ever changes, this could cause
   * errors.
   */
  private static native JsJsonNull createProd() /*-{
    return null;
  }-*/;

  protected JsJsonNull() {
  }
}
