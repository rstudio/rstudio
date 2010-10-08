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
public class IntegerRenderer extends AbstractRenderer<Integer> {
  private static IntegerRenderer INSTANCE;

  /**
   * Returns the instance.
   */
  public static Renderer<Integer> instance() {
    if (INSTANCE == null) {
      INSTANCE = new IntegerRenderer();
    }
    return INSTANCE;
  }

  protected IntegerRenderer() {
  }

  public String render(Integer object) {
    if (null == object) {
      return "";
    }

    return NumberFormat.getDecimalFormat().format(object);
  }
}
