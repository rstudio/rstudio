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
package com.google.web.bindery.autobean.shared;

import com.google.gwt.core.client.GWT;

/**
 * Provides reflection-based operation for server (JVM) implementation. There is
 * a no-op super-source version for client (dev- and web-mode) code.
 */
class ValueCodexHelper {
  /**
   * Returns {@code true} if {@code clazz} is assignable to any of the value
   * types.
   */
  static boolean canDecode(Class<?> clazz) {
    assert !GWT.isClient();
    for (Class<?> valueType : ValueCodex.getAllValueTypes()) {
      if (valueType.isAssignableFrom(clazz)) {
        return true;
      }
    }
    return false;
  }
}
