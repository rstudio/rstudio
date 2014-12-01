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
package com.google.gwt.resources.client.gss;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

/**
 * ClientBundle containing defining several CssResource in order to be able to test the
 * auto-conversion from CSS to GSS.
 */
public interface AutoConversionBundle extends ClientBundle {
  /**
   * For testing automatic constant renaming during the conversion.
   */
  interface ConstantRenaming extends CssResource {
    int myConstant();
    String my_constant();
    int ie6();
    int gecko1_8();
  }

  /**
   * For testing conversion of constants inside conditional nodes.
   */
  interface ConstantConditional extends CssResource {
    String foo();
    String color();
    int width();
    int height();
  }

  /**
   * For testing conversion of external at-rule definition inside conditional nodes.
   */
  interface LenientExternal extends CssResource {
    String nonObfuscated();
    String nonObfuscated2();
    String nonObfuscated3();
    String obfuscated();
  }

  /**
   * For testing conversion of conditional.
   */
  interface Conditional extends CssResource {
    String foo();
  }

  ConstantRenaming constantRenaming();

  ConstantConditional constantConditional();

  LenientExternal lenientExternal();

  Conditional conditional();
}
