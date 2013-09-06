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
package com.google.gwt.junit.client.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;

/**
 * Provides metadata of {@code GWTTestCase} to access its constructor and methods in the runtime.
 */
abstract class GWTTestMetadata {

  static GWTTestMetadata create() {
    return GWT.create(GWTTestMetadata.class);
  }

  /**
   * Returns the metadata as a javascript map. See
   * {@link com.google.gwt.junit.rebind.GWTTestMetadataGenerator#writeCreateMethod} for the format.
   */
  abstract JavaScriptObject get();
}

