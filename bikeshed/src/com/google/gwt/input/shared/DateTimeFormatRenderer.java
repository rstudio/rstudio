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
package com.google.gwt.input.shared;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.TimeZone;

import java.util.Date;

/**
 * Renders {@link Date} objects with a {@link DateTimeFormat}.
 */
public class DateTimeFormatRenderer implements Renderer<Date> {
  private final DateTimeFormat format;
  private final TimeZone timeZone;

  public DateTimeFormatRenderer(DateTimeFormat format) {
    this(format, null);
  }

  public DateTimeFormatRenderer(DateTimeFormat format, TimeZone timeZone) {
    this.format = format;
    this.timeZone = timeZone;
  }

  public String render(Date object) {
    return timeZone == null ? format.format(object) : format.format(object,
        timeZone);
  }
}
