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
package com.google.gwt.core.client.interop;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.core.client.js.impl.PrototypeOfJsType;

@JsType(prototype = "MyJsInterface")
interface MyJsInterfaceWithPrototype {

  @JsProperty
  int getX();

  @JsProperty
  void setX(int a);

  int sum(int bias);

  @PrototypeOfJsType
  static class Prototype implements MyJsInterfaceWithPrototype {

    @Override
    public int getX() {
      return 0;
    }

    @Override
    public void setX(int a) {
    }

    @Override
    public int sum(int bias) {
      return 0;
    }
  }
}
