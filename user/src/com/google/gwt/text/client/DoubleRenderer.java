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
import com.google.gwt.text.shared.Renderer;

/**
 * A localized renderer based on {@link NumberFormat#getDecimalFormat}.
 */
public class DoubleRenderer extends AbstractRenderer<Double> {
  private static DoubleRenderer INSTANCE;

  /**
   * Returns the instance.
   */
  public static Renderer<Double> instance() {
    if (INSTANCE == null) {
      INSTANCE = new DoubleRenderer();
    }
    return INSTANCE;
  }

  protected DoubleRenderer() {
  }

  public String render(Double object) {
    if (object == null) {
      return "";
    }

    return NumberFormat.getDecimalFormat().format(object);
  }
}
