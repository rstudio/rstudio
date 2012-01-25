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

/**
 * Common information across all types of views of {@link ArrayBuffer}s.
 * 
 * {@link "http://www.khronos.org/registry/typedarray/specs/latest/#6"}
 */
public interface ArrayBufferView {

  /**
   * Get the underlying {@link ArrayBuffer}.
   * 
   * @return the {@link ArrayBuffer} instance backing this view
   */
  ArrayBuffer buffer();

  /**
   * Get the length of this view in bytes.
   * 
   * @return number of bytes in this view
   */
  int byteLength();

  /**
   * Get the offset from the beginning of the underlying {@link ArrayBuffer}.
   * 
   * @return 0-based offset from the beginning of {@link #buffer()}
   */
  int byteOffset();
}
