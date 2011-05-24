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
import com.google.gwt.uibinder.rebind.FieldManager;
import com.google.gwt.uibinder.rebind.FieldWriter;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Used by {@link AttachableHTMLPanelParser} to interpret renderable elements.
 * Declares the appropriate {@link IsRenderable}, and returns the correct HTML
 * to be inlined in the AttachableHTMLPanel.
 */
class IsRenderableInterpreter implements XMLElement.Interpreter<String> {

  private final String fieldName;

  private final UiBinderWriter uiWriter;

  public IsRenderableInterpreter(String fieldName, UiBinderWriter writer) {
    this.fieldName = fieldName;
    this.uiWriter = writer;
    assert writer.useLazyWidgetBuilders();
  }

  public String interpretElement(XMLElement elem)
      throws UnableToCompleteException {
    if (!uiWriter.isRenderableElement(elem)) {
      return null;
    }

    String idHolder = uiWriter.declareDomIdHolder();
    FieldManager fieldManager = uiWriter.getFieldManager();
    FieldWriter fieldWriter = fieldManager.require(fieldName);

    FieldWriter childFieldWriter = uiWriter.parseElementToFieldWriter(elem);

    String elementPointer = idHolder + "Element";
    fieldWriter.addAttachStatement(
        "com.google.gwt.user.client.Element %s = " +
        "com.google.gwt.dom.client.Document.get().getElementById(%s).cast();",
        elementPointer, fieldManager.convertFieldToGetter(idHolder));
    fieldWriter.addAttachStatement(
        "%s.wrapElement(%s);",
        fieldManager.convertFieldToGetter(childFieldWriter.getName()),
        elementPointer);

    // Some operations are more efficient when the Widget isn't attached to
    // the document. Perform them here.
    fieldWriter.addDetachStatement(
        "%s.performDetachedInitialization();",
        fieldManager.convertFieldToGetter(childFieldWriter.getName()));

    fieldWriter.addDetachStatement(
        "%s.logicalAdd(%s);",
        fieldManager.convertFieldToGetter(fieldName),
        fieldManager.convertFieldToGetter(childFieldWriter.getName()));

    // TODO(rdcastro): use the render() call that receives the SafeHtmlBuilder
    String elementHtml = fieldManager.convertFieldToGetter(childFieldWriter.getName()) + ".render("
        + fieldManager.convertFieldToGetter(idHolder) + ")";
    return uiWriter.tokenForSafeHtmlExpression(elementHtml);
  }
}
