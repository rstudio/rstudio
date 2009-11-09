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
package com.google.gwt.uibinder.elementparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.uibinder.rebind.FieldManager;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;

import java.util.ArrayList;
import java.util.List;

class MockUiBinderWriter extends UiBinderWriter {
  final List<String> statements = new ArrayList<String>();

  public MockUiBinderWriter(JClassType baseClass, String implClassName,
      String templatePath, TypeOracle oracle, MortalLogger logger,
      FieldManager fieldManager, MessagesWriter messagesWriter)
      throws UnableToCompleteException {
    super(baseClass, implClassName, templatePath, oracle, logger,
        fieldManager, messagesWriter);
  }

  @Override
  public void addStatement(String format, Object... args) {
    statements.add(String.format(format, args));
  }

  @Override
  public String parseElementToField(XMLElement elem)
      throws UnableToCompleteException {
    return elem.consumeOpeningTag();
  }
}