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

/**
 * Class in the same package as the {@link SomeParentParent} to be able to call the package private
 * method.
 */
public class Caller {
  /**
   * This method should always call the method m() from the
   * {@link SomeParentParent}. It should never trigger a call to the one in the
   * {@link SomeSubClassInAnotherPackage} subclass because it is not actually overriding it.
   */
  public String callPackagePrivatem(SomeParentParent someClass) {
    return someClass.m();
  }
}