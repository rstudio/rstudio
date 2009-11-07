/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.UiBinderWriter;

/**
 * Parses a string attribute.
 */
class StringAttributeParser implements AttributeParser {
  /* package private for testing */
  static class FieldReferenceDelegate implements FieldReferenceConverter.Delegate {
    public String handleFragment(String literal) {
      return "\"" + UiBinderWriter.escapeTextForJavaStringLiteral(literal)
          + "\"";
    }

    public String handleReference(String reference) {
      return String.format(" + %s + ", reference);
    }
  }

  final FieldReferenceConverter braceReplacor = new FieldReferenceConverter(
      new FieldReferenceDelegate());

  public String parse(String value, MortalLogger ignored) {
    return braceReplacor.convert(value);
  }
}
