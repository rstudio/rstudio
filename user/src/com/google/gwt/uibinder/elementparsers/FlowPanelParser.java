/*
 * Copyright 2014 Google Inc.
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
import com.google.gwt.uibinder.rebind.model.OwnerField;

/**
 * Parses {@link com.google.gwt.user.client.ui.FlowPanel} widgets.
 */
public class FlowPanelParser implements ElementParser {

  public void parse(XMLElement elem, String fieldName, JClassType type,
      final UiBinderWriter writer) throws UnableToCompleteException {
    String customTag = elem.consumeStringAttribute("tag", null);
    if (null != customTag) {
      OwnerField uiField = writer.getOwnerClass().getUiField(fieldName);
      if (uiField != null && uiField.isProvided()) {
        writer.die("UiField %s for FlowPanel cannot set tag when it is also provided.", fieldName);
      }
      writer.setFieldInitializerAsConstructor(fieldName, customTag);
    }
  }

}
