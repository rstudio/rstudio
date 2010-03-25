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
package com.google.gwt.lang;

/**
 * This is a magic class the compiler uses to contain synthetic methods to
 * support collapsed or "soft" properties.
 */
final class CollapsedPropertyHolder {

  /**
   * This variable is initialized by the compiler in gwtOnLoad.
   */
  public static volatile int permutationId = -1;

  public static int getPermutationId() {
    assert permutationId != -1 : "The bootstrap linker did not provide a "
        + "soft permutation id to the gwtOnLoad function";
    return permutationId;
  }
}
