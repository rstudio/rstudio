/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.core.interop;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

/**
 * Test access to static field from JS, ensuring clinit run.
 */
@JsType
public class StaticInitializerStaticField {
  public static final Object EXPORTED_1 = new Object();

  // Not final
  public static Object EXPORTED_2 = new Object();

  @JsIgnore
  public static final Object NOT_EXPORTED_1 = new Object();

  // Not static
  public final Object NOT_EXPORTED_2 = new Object();

  // Not public
  static final Object NOT_EXPORTED_3 = new Object();

  // Not public
  private static final Object NOT_EXPORTED_4 = new Object();

  // Not public
  protected static final Object NOT_EXPORTED_5 = new Object();

  /**
   * Test interface that export a static field.
   */
  @JsType
  public interface InterfaceWithField {
    Object STATIC = new Object();
  }
}
