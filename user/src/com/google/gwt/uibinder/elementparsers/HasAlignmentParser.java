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
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;

/**
 * Parses widgets that inherit from 
 * {@link com.google.gwt.user.client.ui.HasAlignment}.
 * This class is needed to resolve the parse order of alignment attributes for 
 * these classes.
 * <p>
 * 
 * See {@link "http://code.google.com/p/google-web-toolkit/issues/detail?id=5518"}
 * for issue details.
 */

public class HasAlignmentParser implements ElementParser {
  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {

    // Get fully qualified class name for horizontal alignment
    JClassType hAlignConstantType = writer.getOracle().findType(
        HorizontalAlignmentConstant.class.getCanonicalName());
    // Get horizontal alignment value
    String horizontalAlignment = elem.consumeAttributeWithDefault(
        "horizontalAlignment", null, hAlignConstantType);
    // Set horizontal alignment if not null
    if (horizontalAlignment != null) {
      writer.addStatement("%s.setHorizontalAlignment(%s);", fieldName, 
          horizontalAlignment);
    }

    // Get fully qualified class name for vertical alignment
    JClassType vAlignConstantType = writer.getOracle().findType(
        VerticalAlignmentConstant.class.getCanonicalName());
    // Get vertical alignment value
    String verticalAlignment = elem.consumeAttributeWithDefault(
        "verticalAlignment", null, vAlignConstantType);
    // Set vertical alignment if not null
    if (verticalAlignment != null) {
      writer.addStatement("%s.setVerticalAlignment(%s);", fieldName, 
          verticalAlignment);
    }  
  }
}
