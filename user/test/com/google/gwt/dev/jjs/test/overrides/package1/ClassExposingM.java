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
package com.google.gwt.dev.jjs.test.overrides.package1;

import com.google.gwt.dev.jjs.test.overrides.package3.SomeInterface;

/**
 * This class overrides a package private method an exposes it as public.
 */
public class ClassExposingM extends SomeParentParent implements SomeInterface {

  /**
   * This method overrides SomeParentParent.f() and exposes it as public. SomeParentParent.f() is
   * package private but never referred to except after being made public here; that makes
   * f() dead and we should not need a package private name in optimized compiles.
   */
  @Override
  public String f() {
    return "live at ClassExposingM";
  }

  @Override
  public String m() {
    return "ClassExposingM";
   }
}