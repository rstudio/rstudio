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
package com.google.gwt.uibinder.attributeparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.XMLElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a CSS length value (e.g., "2em", "50%"), returning a comma-separated
 * (double, Unit) pair.
 */
public class LengthAttributeParser implements AttributeParser {

  static final String UNIT = Unit.class.getCanonicalName();

  // This regular expression matches CSS length patterns of the form
  // (value)(unit), where the two may be separated by whitespace. Either part
  // can be a {class.method} expression.
  private static final Pattern pattern = Pattern.compile("((?:\\{[\\w\\.]+\\})|[\\+\\-]?[\\d\\.]+)\\s*(\\{?[\\w\\.\\%]*\\}?)?");

  private final MortalLogger logger;
  private final DoubleAttributeParser doubleParser;
  private final EnumAttributeParser enumParser;

  LengthAttributeParser(DoubleAttributeParser doubleParser,
      EnumAttributeParser enumParser, MortalLogger logger) {
    this.doubleParser = doubleParser;
    this.enumParser = enumParser;
    this.logger = logger;
  }

  public String parse(XMLElement source, String lengthStr) throws UnableToCompleteException {
    Matcher matcher = pattern.matcher(lengthStr);
    if (!matcher.matches()) {
      logger.die(source, "Unable to parse %s as length", lengthStr);
    }

    String valueStr = matcher.group(1);
    String value = doubleParser.parse(source, valueStr);

    String unit = null;
    String unitStr = matcher.group(2);
    if (unitStr.length() > 0) {
      if (!unitStr.startsWith("{")) {
        // For non-refs, convert % => PCT, px => PX, etc.
        if ("%".equals(unitStr)) {
          unitStr = "PCT";
        }
        unitStr = unitStr.toUpperCase();
      }

      // Now let the default enum parser handle it.
      unit = enumParser.parse(source, unitStr);
    } else {
      // Use PX by default.
      unit = UNIT + ".PX";
    }

    return value + ", " + unit;
  }
}
