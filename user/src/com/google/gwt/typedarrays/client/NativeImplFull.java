/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.typedarrays.client;

/**
 * An implementation class used for browsers which provide full native
 * implementations of all typed array classes.
 * <p>
 * Current versions of Opera are such browsers.
 */
public class NativeImplFull extends NativeImpl {
  // Opera 11.6+ (but not mini/mobile) supports typed arrays.

  @Override
  protected boolean checkDataViewSupport() {
    return true;
  }

  @Override
  protected boolean checkUint8ClampedArraySupport() {
    return true;
  }
}