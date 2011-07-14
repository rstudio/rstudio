/*
 * Copyright 2008 Google Inc.
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
 * Parser of all UIObject types. Provides parsing of debugId, addStyleNames,
 * addStyleDependentNames. Also handles other setStyle* calls to ensure they
 * happen before the addStyle* calls.
 */
public class UIObjectParser implements ElementParser {

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    String debugId = elem.consumeStringAttribute("debugId", null);
    if (null != debugId) {
      writer.addStatement("%s.ensureDebugId(%s);", fieldName, debugId);
    }

    String styleName = elem.consumeStringAttribute("styleName", null);
    String stylePrimaryName = elem.consumeStringAttribute("stylePrimaryName",
        null);

    if (null != styleName && null != stylePrimaryName) {
      writer.die(elem, "Cannot set both \"styleName\" "
          + "and \"stylePrimaryName\"");
    }

    if (null != styleName) {
      writer.addStatement("%s.setStyleName(%s);", fieldName, styleName);
    }
    if (null != stylePrimaryName) {
      writer.addStatement("%s.setStylePrimaryName(%s);", fieldName,
          stylePrimaryName);
    }

    String[] extraStyleNames = elem.consumeStringArrayAttribute("addStyleNames");
    for (String s : extraStyleNames) {
      writer.addStatement("%s.addStyleName(%s);", fieldName, s);
    }

    extraStyleNames = elem.consumeStringArrayAttribute("addStyleDependentNames");
    for (String s : extraStyleNames) {
      writer.addStatement("%s.addStyleDependentName(%s);", fieldName, s);
    }
  }
}
