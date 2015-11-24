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
package com.google.gwt.dev.jjs.test.defaultmethods;

/**
 * Classes to test default method defined in different compilation units.
 */
public interface WithDefaultMethodAndStaticInitializer {
  SomeClass someClass = new SomeClass("1");
  SomeClass someClass2 = new SomeClass("2");

  default SomeClass getGetSomeClass() {
    return someClass;
  }
}
