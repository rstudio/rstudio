/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.uibinder.rebind.DesignTimeUtilsStub;
import com.google.gwt.uibinder.rebind.FieldManager;
import com.google.gwt.uibinder.rebind.FieldWriter;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.UiBinderContext;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;

import java.util.ArrayList;
import java.util.List;

class MockUiBinderWriter extends UiBinderWriter {
  final List<String> statements = new ArrayList<String>();

  public MockUiBinderWriter(JClassType baseClass, String implClassName, String templatePath,
      TypeOracle oracle, MortalLogger logger, FieldManager fieldManager,
      MessagesWriter messagesWriter, String binderUri) throws UnableToCompleteException {
    super(baseClass, implClassName, templatePath, oracle, logger, fieldManager, messagesWriter,
        DesignTimeUtilsStub.EMPTY, new UiBinderContext(), true, false, binderUri);
  }

  @Override
  public void addStatement(String format, Object... args) {
    statements.add(String.format(format, args));
  }

  /**
   * Mocked out version of the template declaration. Returns the fieldName and
   * template separated with a dash, all prefixed with "@mockToken-"
   */
  @Override
  public String declareTemplateCall(String html, String fieldName) {
    return "\"@mockToken-" + fieldName + "-" + html + "\"";
  }

  @Override
  public FieldWriter parseElementToField(XMLElement elem) {
    final String tag = elem.consumeOpeningTag();
    return new MockFieldWriter(tag);
  }
}
