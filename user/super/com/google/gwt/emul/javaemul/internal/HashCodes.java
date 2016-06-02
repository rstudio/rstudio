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
package javaemul.internal;

/**
 * Contains logics for calculating hash codes in JavaScript.
 */
public class HashCodes {
  public static int getIdentityHashCode(Object o) {
    switch (JsUtils.typeOf(o)) {
      case "string":
        return getStringHashCode(JsUtils.unsafeCastToString(o));
      case "number":
        return Double.hashCode(JsUtils.unsafeCastToDouble(o));
      case "boolean":
        return Boolean.hashCode(JsUtils.unsafeCastToBoolean(o));
      default:
        return o == null ? 0 : getObjectIdentityHashCode(o);
    }
  }

  public static int getObjectIdentityHashCode(Object o) {
    return ObjectHashing.getHashCode(o);
  }

  public static int getStringHashCode(String s) {
    return StringHashCache.getHashCode(s);
  }
}
