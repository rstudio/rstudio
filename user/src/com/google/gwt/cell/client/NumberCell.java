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
package com.google.gwt.cell.client;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;

/**
 * A {@link Cell} used to render formatted numbers.
 */
public class NumberCell extends AbstractCell<Number> {

  /**
   * The {@link NumberFormat} used to render the number.
   */
  private final NumberFormat format;

  /**
   * The {@link SafeHtmlRenderer} used to render the formatted number as HTML.
   */
  private final SafeHtmlRenderer<String> renderer;

  /**
   * Construct a new {@link NumberCell} using decimal format and a
   * {@link SimpleSafeHtmlRenderer}.
   */
  public NumberCell() {
    this(NumberFormat.getDecimalFormat(), SimpleSafeHtmlRenderer.getInstance());
  }

  /**
   * Construct a new {@link NumberCell} using the given {@link NumberFormat} and
   * a {@link SimpleSafeHtmlRenderer}.
   *
   * @param format the {@link NumberFormat} used to render the number
   */
  public NumberCell(NumberFormat format) {
    this(format, SimpleSafeHtmlRenderer.getInstance());
  }

  /**
   * Construct a new {@link NumberCell} using decimal format and the given
   * {@link SafeHtmlRenderer}.
   *
   * @param renderer the {@link SafeHtmlRenderer} used to render the formatted
   *          number as HTML
   */
  public NumberCell(SafeHtmlRenderer<String> renderer) {
    this(NumberFormat.getDecimalFormat(), renderer);
  }

  /**
   * Construct a new {@link NumberCell} using the given {@link NumberFormat} and
   * a {@link SafeHtmlRenderer}.
   *
   * @param format the {@link NumberFormat} used to render the number
   * @param renderer the {@link SafeHtmlRenderer} used to render the formatted
   *          number as HTML
   */
  public NumberCell(NumberFormat format, SafeHtmlRenderer<String> renderer) {
    if (format == null) {
      throw new IllegalArgumentException("format == null");
    }
    if (renderer == null) {
      throw new IllegalArgumentException("renderer == null");
    }
    this.format = format;
    this.renderer = renderer;
  }

  @Override
  public void render(Context context, Number value, SafeHtmlBuilder sb) {
    if (value != null) {
      sb.append(renderer.render(format.format(value)));
    }
  }
}
