/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.linker.CastableTypeMap;

/**
 * The standard implementation of {@link CastableTypeMap}.
 */
public class StandardCastableTypeMap implements CastableTypeMap {
  // Save some memory by defining this constant string.
  private static final String EMPTY_JSON_REF = "{}";

  final String jsonData;
  public StandardCastableTypeMap(String jsonData) {
    this.jsonData = jsonData.equals(EMPTY_JSON_REF) ? EMPTY_JSON_REF : jsonData;
  }

  @Override
  public String toJs() {
    return jsonData;
  }
}
