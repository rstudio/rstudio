/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.collections;

/**
 * Shared assertions and related messages.
 */
class Assertions {
  
  static void assertIndexInRange(int index, int minInclusive, int maxExclusive) {
    assert (index >= minInclusive && index < maxExclusive) : "Index " + index 
        + " was not in the acceptable range [" + minInclusive + ", " + maxExclusive + ")";
  }

  static void assertNotNull(Object ref) {
    assert (ref != null) : "A null reference is not allowed here";
  }

}
