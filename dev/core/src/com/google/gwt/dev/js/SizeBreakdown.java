/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.JsName;

import java.util.Map;

/**
 * A size breakdown of a single JavaScript code fragment.
 */
public class SizeBreakdown {
  private final int size;
  private final Map<JsName, Integer> sizeMap;

  public SizeBreakdown(int size, Map<JsName, Integer> sizeMap) {
    this.size = size;
    this.sizeMap = sizeMap;
  }

  public int getSize() {
    return size;
  }

  public Map<JsName, Integer> getSizeMap() {
    return sizeMap;
  }
}
