/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.jjs.test;

import java.util.ArrayList;

/**
 * A superclass that invokes a method in its cstr, so that subclasses can see their state before
 * their own cstr has run.
 * 
 * See {@link CompilerTest#testFieldInitializationOrder()}.
 */
class FieldInitOrderBase {
  FieldInitOrderBase(ArrayList<String> seenValues, int x) {
    method(seenValues, x);
  }

  void method(ArrayList<String> seenValues, int x) {
  }
}