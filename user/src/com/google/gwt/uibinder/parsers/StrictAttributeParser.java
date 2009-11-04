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
package com.google.gwt.uibinder.parsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.uibinder.parsers.FieldReferenceConverter.Delegate;
import com.google.gwt.uibinder.parsers.FieldReferenceConverter.IllegalFieldReferenceException;
import com.google.gwt.uibinder.rebind.MortalLogger;

/**
 * Fall through attribute parser. Accepts a field reference or nothing.
 */
public class StrictAttributeParser implements AttributeParser {

  /** 
   * Package protected for testing
   */
  static class FieldReferenceDelegate implements Delegate {
    boolean sawReference = false;
    
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

  /**
   * If the value holds a single field reference "{like.this}", converts it to a
   * Java Expression.
   * <p>
   * In any other case (e.g. more than one field reference), an
   * UnableToCompleteException is thrown.
   */
  public String parse(String value, MortalLogger logger)
      throws UnableToCompleteException {

    try {
      return new FieldReferenceConverter(new FieldReferenceDelegate()).convert(value);
    } catch (IllegalFieldReferenceException e) {
      logger.die("Cannot parse value: \"%s\"", value);
      return null; // Unreachable
    }
  }
}
