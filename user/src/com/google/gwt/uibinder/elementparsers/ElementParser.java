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
package com.google.gwt.uibinder.elementparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Element parsers are classes that parse xml elements, using the context to
 * generate widget initialization code.
 */
public interface ElementParser {

  /**
   * Parse the given element, generating the code to initialize it from the
   * element's attributes and children.
   *
   * @param elem the element to be parsed
   * @param fieldName the name of the widget field to be initialized
   * @param type TODO
   * @param writer the writer
   * @throws UnableToCompleteException on error
   */
  void parse(XMLElement elem, String fieldName,
      JClassType type, UiBinderWriter writer) throws UnableToCompleteException;
}
