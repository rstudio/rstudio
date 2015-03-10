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
package com.google.gwt.core.client.interop;

/**
 * An interface that extends JsFunction interface {@code MyJsFunctionInterface} with default method.
 */
public interface MyJsFunctionSubInterfaceWithDefaultMethod extends MyJsFunctionInterface {

  // uncomment when Java8 is supported.
//  @Override
//  default int foo(int a) {
//    return a;
//  }
}
