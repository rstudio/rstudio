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
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.uibinder.attributeparsers.FieldReferenceConverter.Delegate;
import com.google.gwt.uibinder.attributeparsers.FieldReferenceConverter.IllegalFieldReferenceException;
import com.google.gwt.uibinder.rebind.FieldReference;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Fall through attribute parser. Accepts a field reference or nothing.
 */
class StrictAttributeParser implements AttributeParser {

  /**
   * Package protected for testing.
   */
  static class FieldReferenceDelegate implements Delegate {
    private boolean sawReference = false;
    private final JType[] types;
    
    FieldReferenceDelegate(JType... types) {
      this.types = types;
    }

    public JType[] getTypes() {
      return types;
    }

    public String handleFragment(String fragment)
        throws IllegalFieldReferenceException {
      if (fragment.length() > 0) {
        throw new IllegalFieldReferenceException();
      }
      return fragment;
    }

    public String handleReference(String reference)
        throws IllegalFieldReferenceException {
      assertOnly();
      sawReference = true;
      return reference;
    }

    private void assertOnly() {
      if (sawReference) {
        throw new IllegalFieldReferenceException();
      }
    }
  }

  private final FieldReferenceConverter converter;
  protected final MortalLogger logger;
  private final JType[] types;

  StrictAttributeParser(FieldReferenceConverter converter, MortalLogger logger,
      JType... types) {
    this.converter = converter;
    this.logger = logger;
    this.types = types;
  }

  /**
   * If the value holds a single field reference "{like.this}", converts it to a
   * Java Expression.
   * <p>
   * In any other case (e.g. more than one field reference), an
   * UnableToCompleteException is thrown.
   */
  public String parse(XMLElement source, String value) throws UnableToCompleteException {
    if ("".equals(value.trim())) {
      logger.die(source, "Cannot use empty value as type %s", FieldReference.renderTypesList(types));
    }
    try {
      return converter.convert(source, value, new FieldReferenceDelegate(types));
    } catch (IllegalFieldReferenceException e) {
      logger.die(source, "Cannot parse value: \"%s\" as type %s", value, FieldReference.renderTypesList(types));
      return null; // Unreachable
    }
  }
}
