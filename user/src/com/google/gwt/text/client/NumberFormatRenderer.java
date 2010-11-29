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
package com.google.gwt.text.client;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.text.shared.AbstractRenderer;

/**
 * Renders {@link Number} objects with a {@link NumberFormat}.
 */
public class NumberFormatRenderer extends AbstractRenderer<Number> {
  private final NumberFormat format;

  /**
   * Create an instance using {@link NumberFormat#getDecimalFormat()}.
   */
  public NumberFormatRenderer() {
    this(NumberFormat.getDecimalFormat());
  }

  /**
   * Create an instance with the given format.
   */
  public NumberFormatRenderer(NumberFormat format) {
    this.format = format;
  }

  public String render(Number object) {
    if (object == null) {
      return "";
    }
    return format.format(object);
  }
}
