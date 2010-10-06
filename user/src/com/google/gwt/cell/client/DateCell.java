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

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;

import java.util.Date;

/**
 * A {@link Cell} used to render {@link Date}s.
 */
public class DateCell extends AbstractCell<Date> {

  private final DateTimeFormat format;

  private final SafeHtmlRenderer<String> renderer;

  /**
   * Construct a new {@link DateCell} using the format
   * {@link PredefinedFormat#DATE_FULL} and a {@link SimpleSafeHtmlRenderer}.
   */
  public DateCell() {
    this(DateTimeFormat.getFormat(PredefinedFormat.DATE_FULL),
        SimpleSafeHtmlRenderer.getInstance());
  }

  /**
   * Construct a new {@link DateCell} using the format
   * {@link PredefinedFormat#DATE_FULL} and a {@link SimpleSafeHtmlRenderer}.
   *
   * @param renderer a non-null {@link SafeHtmlRenderer} used to render the
   *          formatted date as HTML
   */
  public DateCell(SafeHtmlRenderer<String> renderer) {
    this(DateTimeFormat.getFormat(PredefinedFormat.DATE_FULL), renderer);
  }

  /**
   * Construct a new {@link DateCell} using the specified format and a
   * {@link SimpleSafeHtmlRenderer}.
   *
   * @param format the {@link DateTimeFormat} used to render the date
   */
  public DateCell(DateTimeFormat format) {
    this(format, SimpleSafeHtmlRenderer.getInstance());
  }

  /**
   * Construct a new {@link DateCell} using the specified format and the given
   * {@link SafeHtmlRenderer}.
   *
   * @param format the {@link DateTimeFormat} used to render the date
   * @param renderer a non-null {@link SafeHtmlRenderer} used to render the
   *          formatted date
   */
  public DateCell(DateTimeFormat format, SafeHtmlRenderer<String> renderer) {
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
  public void render(Date value, Object key, SafeHtmlBuilder sb) {
    if (value != null) {
      sb.append(renderer.render(format.format(value)));
    }
  }
}
