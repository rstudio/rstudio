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
package com.google.gwt.typedarrays.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.typedarrays.client.NativeImpl;

/**
 * Factory for creating a {@link TypedArrays} implementation.
 * <p>
 * This is the client version.
 */
class TypedArraysFactory {

  static TypedArrays.Impl createImpl() {
    return GWT.create(NativeImpl.class);
  }
}
