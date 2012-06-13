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

import com.google.gwt.core.client.GWT;

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
public class Collections {

  /**
   * Create an ArrayOf collection using the most efficient implementation strategy for the given
   * client.
   *
   * @param <T>   the element type contained in the collection
   * @return a JreArrayOf or JsArrayOf instance
   */
  public static <T> ArrayOf<T> arrayOf() {
    return new JreArrayOf<T>();
  }

  /**
   * Create an ArrayOfBoolean collection using the most efficient implementation strategy for the
   * given client.
   *
   * @return a JreArrayOfBoolean or JsArrayOfBoolean instance
   */
  public static <T> ArrayOfBoolean arrayOfBoolean() {
    return new JreArrayOfBoolean();
  }

  /**
   * Create an ArrayOfInt collection using the most efficient implementation strategy for the given
   * client.
   *
   * @return a JreArrayOfInt or JsArrayOfInt instance
   */
  public static <T> ArrayOfInt arrayOfInt() {
    return new JreArrayOfInt();
  }

  /**
   * Create an ArrayOfNumber collection using the most efficient implementation strategy for the
   * given client.
   *
   * @return a JreArrayOfNumber or JsArrayOfNumber instance
   */
  public static <T> ArrayOfNumber arrayOfNumber() {
    return new JreArrayOfNumber();
  }

  /**
   * Create an ArrayOfString collection using the most efficient implementation strategy for the
   * given client.
   *
   * @return a JreArrayOfString or JsArrayOfString instance
   */
  public static <T> ArrayOfString arrayOfString() {
    return new JreArrayOfString();
  }

  /**
   * Create a MapFromIntTo collection for a given type using the most efficient implementation
   * strategy for the given client.
   *
   * @param <T>   the element type contained in the collection
   * @return a JreMapFromIntTo or JsMapFromIntTo instance
   */
  public static <T> MapFromIntTo<T> mapFromIntTo() {
    return new JreMapFromIntTo<T>();
  }

  /**
   * Create a MapFromIntToString collection for a given type using the most efficient implementation
   * strategy for the given client.
   *
   * @return a JreMapFromIntToString or JsMapFromIntToString instance
   */
  public static MapFromIntToString mapFromIntToString() {
    return new JreMapFromIntToString();
  }

  /**
   * Create a MapFromStringTo collection for a given type using the most efficient implementation
   * strategy for the given client.
   *
   * @param <T>   the element type contained in the collection
   * @return a JreMapFromStringTo or JsMapFromStringTo instance
   */
  public static <T> MapFromStringTo<T> mapFromStringTo() {
    return new JreMapFromStringTo<T>();
  }

  /**
   * Create a MapFromStringToBoolean collection for a given type using the most efficient
   * implementation strategy for the given client.
   *
   * @return a JreMapFromStringToBoolean or JsMapFromStringToBoolean instance
   */
  public static MapFromStringToBoolean mapFromStringToBoolean() {
    return new JreMapFromStringToBoolean();
  }

  /**
   * Create a MapFromStringToInt collection for a given type using the most efficient implementation
   * strategy for the given client.
   *
   * @return a JreMapFromStringToInt or JsMapFromStringToInt instance
   */
  public static MapFromStringToInt mapFromStringToInt() {
    return new JreMapFromStringToInt();
  }

  /**
   * Create a MapFromStringToNumber collection for a given type using the most efficient
   * implementation strategy for the given client.
   *
   * @return a JreMapFromStringToNumber or JsMapFromStringToNumber instance
   */
  public static MapFromStringToNumber mapFromStringToNumber() {
    return new JreMapFromStringToNumber();
  }

  /**
   * Create a MapFromStringToString collection for a given type using the most efficient
   * implementation strategy for the given client.
   *
   * @return a JreMapFromStringToString or JsMapFromStringToString instance
   */
  public static MapFromStringToString mapFromStringToString() {
    return new JreMapFromStringToString();
  }
}
