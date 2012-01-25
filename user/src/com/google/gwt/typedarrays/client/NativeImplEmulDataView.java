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
 * An implementation class used for browsers which provide native
 * implementations of
 * {@link com.google.gwt.typedarrays.shared.Uint8ClampedArray} but not
 * {@link com.google.gwt.typedarrays.shared.DataView}.
 * <p>
 * Current versions of FireFox are such browsers.
 */
public class NativeImplEmulDataView extends NativeImpl {
  // TODO: can we override runtimeSupportCheck to return true?
  // FF4+ supports typed arrays (except DataView).

  @Override
  protected boolean checkDataViewSupport() {
    return false;
  }

  @Override
  protected boolean checkUint8ClampedArraySupport() {
    return true;
  }
}