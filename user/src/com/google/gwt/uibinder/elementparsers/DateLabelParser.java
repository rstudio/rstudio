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
package com.google.gwt.uibinder.elementparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.uibinder.rebind.TypeOracleUtils;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Parses {@link com.google.gwt.user.client.ui.DateLabel} widgets.
 */
public class DateLabelParser implements ElementParser {

  static final String AT_MOST_ONE_SPECIFIED_FORMAT = "May have at most one of format, predefinedFormat and customFormat.";
  static final String AT_MOST_ONE_SPECIFIED_TIME_ZONE = "May have at most one of timezone and timezoneOffset.";
  static final String NO_TIMEZONE_WITHOUT_SPECIFIED_FORMAT = "May not specify a time zone if no format is given.";

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    boolean supportsTimeZone = hasDateTimeFormatAndTimeZoneConstructor(
        writer.getOracle(), type);
    if (hasDateTimeFormatConstructor(writer.getOracle(), type)
        || supportsTimeZone) {
      String format = consumeFormat(elem, writer);

      if (format != null) {
        String timeZone = (supportsTimeZone ? consumeTimeZone(elem, writer)
            : null);

        writer.setFieldInitializerAsConstructor(fieldName, makeArgs(
            format, timeZone));
      } else if (supportsTimeZone && hasTimeZone(elem)) {
        writer.die(elem, NO_TIMEZONE_WITHOUT_SPECIFIED_FORMAT);
      }
    }
  }

  private String consumeFormat(XMLElement elem, UiBinderWriter writer)
      throws UnableToCompleteException {
    String format = elem.consumeAttribute("format",
        writer.getOracle().findType(DateTimeFormat.class.getCanonicalName()));
    String predefinedFormat = elem.consumeAttribute("predefinedFormat",
        writer.getOracle().findType(PredefinedFormat.class.getCanonicalName()));
    String customFormat = elem.consumeStringAttribute("customFormat");

    if (format != null) {
      if (predefinedFormat != null || customFormat != null) {
        writer.die(elem, AT_MOST_ONE_SPECIFIED_FORMAT);
      }
      return format;
    }
    if (predefinedFormat != null) {
      if (customFormat != null) {
        writer.die(elem, AT_MOST_ONE_SPECIFIED_FORMAT);
      }
      return makeGetFormat(predefinedFormat);
    }
    if (customFormat != null) {
      return makeGetFormat(customFormat);
    }
    return null;
  }

  private String consumeTimeZone(XMLElement elem, UiBinderWriter writer)
      throws UnableToCompleteException {
    String timeZone = elem.consumeAttribute("timezone",
        writer.getOracle().findType(TimeZone.class.getCanonicalName()));
    String timeZoneOffset = elem.consumeAttribute("timezoneOffset",
        getIntType(writer.getOracle()));
    if (timeZone != null && timeZoneOffset != null) {
      writer.die(elem, AT_MOST_ONE_SPECIFIED_TIME_ZONE);
    }
    if (timeZone != null) {
      return timeZone;
    }
    if (timeZoneOffset != null) {
      return TimeZone.class.getCanonicalName() + ".createTimeZone("
          + timeZoneOffset + ")";
    }
    return null;
  }

  private JType getIntType(TypeOracle oracle) {
    try {
      return oracle.parse("int");
    } catch (TypeOracleException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean hasDateTimeFormatAndTimeZoneConstructor(
      TypeOracle typeOracle, JClassType type) {
    JType dateTimeFormatType = typeOracle.findType(DateTimeFormat.class.getName());
    JType timeZoneType = typeOracle.findType(TimeZone.class.getName());
    return TypeOracleUtils.hasCompatibleConstructor(type, dateTimeFormatType, timeZoneType);
  }

  private boolean hasDateTimeFormatConstructor(TypeOracle typeOracle,
      JClassType type) {
    JType dateTimeFormatType = typeOracle.findType(DateTimeFormat.class.getName());
    return TypeOracleUtils.hasCompatibleConstructor(type, dateTimeFormatType);
  }

  private boolean hasTimeZone(XMLElement elem) {
    return elem.hasAttribute("timezone") || elem.hasAttribute("timezoneOffset");
  }

  private String[] makeArgs(String format, String timeZone) {
    if (timeZone == null) {
      return new String[] {format};
    }
    return new String[] {format, timeZone};
  }

  private String makeGetFormat(String format) {
    return DateTimeFormat.class.getCanonicalName() + ".getFormat(" + format
        + ")";
  }
}
