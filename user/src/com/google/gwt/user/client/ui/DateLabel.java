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
package com.google.gwt.user.client.ui;

import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.i18n.shared.TimeZone;
import com.google.gwt.text.client.DateTimeFormatRenderer;

import java.util.Date;

/**
 * Extends {@link ValueLabel} for convenience when dealing with dates and
 * {@link DateTimeFormat}, especially in
 * {@link com.google.gwt.uibinder.client.UiBinder UiBinder} templates. (Note
 * that this class does not accept renderers. To do so use {@link ValueLabel}
 * directly.)
 * 
 * <h3>Use in UiBinder Templates</h3>
 * In {@link com.google.gwt.uibinder.client.UiBinder UiBinder} templates, both the format and time zone can be configured.
 * <p>
 * The format can be given with one of these attributes:
 * <dl>
 * <dt>format</dt><dd>a reference to a {@link DateTimeFormat} instance.</dd>
 * <dt>predefinedFormat</dt><dd>a {@link com.google.gwt.i18n.shared.DateTimeFormat.PredefinedFormat DateTimeFormat.PredefinedFormat}.</dd>
 * <dt>customFormat</dt><dd>a date time pattern that can be passed to {@link DateTimeFormat#getFormat(String)}.</dd>
 * </dl>
 * <p>
 * The time zone can be specified with either of these attributes:
 * <dl>
 * <dt>timezone</dt><dd>a reference to a {@link TimeZone} instance.</dd>
 * <dt>timezoneOffset</dt><dd>the time zone offset in minutes.</dd>
 * </dl>
 */
public class DateLabel extends ValueLabel<Date> {

  public DateLabel() {
    super(new DateTimeFormatRenderer());
  }

  public DateLabel(DateTimeFormat format) {
    super(new DateTimeFormatRenderer(format));
  }

  public DateLabel(DateTimeFormat format, TimeZone timeZone) {
    super(new DateTimeFormatRenderer(format, timeZone));
  }
}
