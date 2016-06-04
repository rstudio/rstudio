/*
 * Copyright 2008 Google Inc.
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
 * Abstracts the notion of a sequence of characters.
 */
public interface CharSequence {
  char charAt(int index);

  int length();

  CharSequence subSequence(int start, int end);

  @Override
  String toString();

  // CHECKSTYLE_OFF: Utility methods.
  @JsMethod
  static boolean $isInstance(Object instance) {
    if (JsUtils.typeOf(instance).equals("string")) {
      return true;
    }

    return instance != null && JsUtils.hasCharSequenceTypeMarker(instance);
  }
  // CHECKSTYLE_ON: end utility methods
}
