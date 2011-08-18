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

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Attribute parsers are classes that parse xml attribute values, turning them
 * into valid Java expressions.
 */
public interface AttributeParser {
  
  /**
   * Parse the given attribute value.
   * @param source the source code the value came from, for error reporting purposes
   * @param value the attribute value to be parsed
   * 
   * @return a valid Java expression
   * @throws UnableToCompleteException on parse error
   */
  String parse(XMLElement source, String value)
      throws UnableToCompleteException;
}
