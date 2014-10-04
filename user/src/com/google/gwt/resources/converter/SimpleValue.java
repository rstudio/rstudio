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
package com.google.gwt.resources.converter;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.resources.css.ast.CssProperty.Value;

/**
 * A simple property value class.
 */
public class SimpleValue extends Value {

  private String value;

  public SimpleValue(String value) {
    this.value = value;
  }

  @Override
  public String toCss() {
    return Generator.escape(value);
  }

  @Override
  public String getExpression() {
    // The escaped CSS content has to be escaped to be a valid Java literal
    return "\"" + toCss() + "\"";
  }
}
