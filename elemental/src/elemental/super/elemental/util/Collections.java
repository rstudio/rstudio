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
package elemental.util;

import com.google.gwt.core.client.GwtScriptOnly;

import elemental.js.util.JsArrayOf;
import elemental.js.util.JsArrayOfBoolean;
import elemental.js.util.JsArrayOfInt;
import elemental.js.util.JsArrayOfNumber;
import elemental.js.util.JsArrayOfString;
import elemental.js.util.JsMapFromIntTo;
import elemental.js.util.JsMapFromIntToString;
import elemental.js.util.JsMapFromStringTo;
import elemental.js.util.JsMapFromStringToBoolean;
import elemental.js.util.JsMapFromStringToInt;
import elemental.js.util.JsMapFromStringToNumber;
import elemental.js.util.JsMapFromStringToString;
import elemental.util.impl.JreArrayOf;
import elemental.util.impl.JreArrayOfBoolean;
import elemental.util.impl.JreArrayOfInt;
import elemental.util.impl.JreArrayOfNumber;
import elemental.util.impl.JreArrayOfString;
import elemental.util.impl.JreMapFromIntTo;
import elemental.util.impl.JreMapFromIntToString;
import elemental.util.impl.JreMapFromStringTo;
import elemental.util.impl.JreMapFromStringToBoolean;
import elemental.util.impl.JreMapFromStringToInt;
import elemental.util.impl.JreMapFromStringToNumber;
import elemental.util.impl.JreMapFromStringToString;

/**
 * Factory and utility methods for elemental collections.
 */
@GwtScriptOnly
public class Collections {

  /**
   * Create an ArrayOf collection using the most efficient implementation strategy for the given
   * client.
   *
   * @param <T>   the element type contained in the collection
   * @return a JreArrayOf or JsArrayOf instance
   */
  public static <T> ArrayOf<T> arrayOf() {
    return JsArrayOf.<T>create();
  }

  /**
   * Create an ArrayOfBoolean collection using the most efficient implementation strategy for the
   * given client.
   *
   * @return a JreArrayOfBoolean or JsArrayOfBoolean instance
   */
  public static <T> ArrayOfBoolean arrayOfBoolean() {
    return JsArrayOfBoolean.create();
  }

  /**
   * Create an ArrayOfInt collection using the most efficient implementation strategy for the given
   * client.
   *
   * @return a JreArrayOfInt or JsArrayOfInt instance
   */
  public static <T> ArrayOfInt arrayOfInt() {
    return JsArrayOfInt.create();
  }

  /**
   * Create an ArrayOfNumber collection using the most efficient implementation strategy for the
   * given client.
   *
   * @return a JreArrayOfNumber or JsArrayOfNumber instance
   */
  public static <T> ArrayOfNumber arrayOfNumber() {
    return JsArrayOfNumber.create();
  }

  /**
   * Create an ArrayOfString collection using the most efficient implementation strategy for the
   * given client.
   *
   * @return a JreArrayOfString or JsArrayOfString instance
   */
  public static <T> ArrayOfString arrayOfString() {
    return JsArrayOfString.create();
  }

  /**
   * Create a MapFromIntTo collection for a given type using the most efficient implementation
   * strategy for the given client.
   *
   * @param <T>   the element type contained in the collection
   * @return a JreMapFromIntTo or JsMapFromIntTo instance
   */
  public static <T> MapFromIntTo<T> mapFromIntTo() {
    return JsMapFromIntTo.<T>create();
  }

  /**
   * Create a MapFromIntToString collection for a given type using the most efficient implementation
   * strategy for the given client.
   *
   * @return a JreMapFromIntToString or JsMapFromIntToString instance
   */
  public static MapFromIntToString mapFromIntToString() {
    return JsMapFromIntToString.create();
  }

  /**
   * Create a MapFromStringTo collection for a given type using the most efficient implementation
   * strategy for the given client.
   *
   * @param <T>   the element type contained in the collection
   * @return a JreMapFromStringTo or JsMapFromStringTo instance
   */
  public static <T> MapFromStringTo<T> mapFromStringTo() {
    return JsMapFromStringTo.<T>create();
  }

  /**
   * Create a MapFromStringToBoolean collection for a given type using the most efficient
   * implementation strategy for the given client.
   *
   * @return a JreMapFromStringToBoolean or JsMapFromStringToBoolean instance
   */
  public static MapFromStringToBoolean mapFromStringToBoolean() {
    return JsMapFromStringToBoolean.create();
  }

  /**
   * Create a MapFromStringToInt collection for a given type using the most efficient implementation
   * strategy for the given client.
   *
   * @return a JreMapFromStringToInt or JsMapFromStringToInt instance
   */
  public static MapFromStringToInt mapFromStringToInt() {
    return JsMapFromStringToInt.create();
  }

  /**
   * Create a MapFromStringToNumber collection for a given type using the most efficient
   * implementation strategy for the given client.
   *
   * @return a JreMapFromStringToNumber or JsMapFromStringToNumber instance
   */
  public static MapFromStringToNumber mapFromStringToNumber() {
    return JsMapFromStringToNumber.create();
  }

  /**
   * Create a MapFromStringToString collection for a given type using the most efficient
   * implementation strategy for the given client.
   *
   * @return a JreMapFromStringToString or JsMapFromStringToString instance
   */
  public static MapFromStringToString mapFromStringToString() {
    return JsMapFromStringToString.create();
  }
}
