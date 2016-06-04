/*
 * Copyright 2007 Google Inc.
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
package java.lang;

import javaemul.internal.JsUtils;

import jsinterop.annotations.JsMethod;

/**
 * An interface used a basis for implementing custom ordering. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Comparable.html">[Sun
 * docs]</a>
 * 
 * @param <T> the type to compare to.
 */
public interface Comparable<T> {
  int compareTo(T other);

  // CHECKSTYLE_OFF: Utility methods.
  @JsMethod
  static boolean $isInstance(Object instance) {
    String type = JsUtils.typeOf(instance);
    if (type.equals("boolean") || type.equals("number") || type.equals("string")) {
      return true;
    }

    return instance != null && JsUtils.hasComparableTypeMarker(instance);
  }
  // CHECKSTYLE_ON: end utility methods
}
