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
 * Used by {@link RenderablePanelParser} to interpret renderable elements.
 * Declares the appropriate {@link IsRenderable}, and returns the correct HTML
 * to be inlined in the {@link RenderablePanel}.
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

    String stamper = uiWriter.declareRenderableStamper();
    FieldManager fieldManager = uiWriter.getFieldManager();
    FieldWriter fieldWriter = fieldManager.require(fieldName);
    FieldWriter childFieldWriter = uiWriter.parseElementToField(elem);

    fieldWriter.addAttachStatement(
        "%s.claimElement(%s.findStampedElement());",
        fieldManager.convertFieldToGetter(childFieldWriter.getName()),
        fieldManager.convertFieldToGetter(stamper));

    // Some operations are more efficient when the Widget isn't attached to
    // the document. Perform them here.
    fieldWriter.addDetachStatement(
        "%s.initializeClaimedElement();",
        fieldManager.convertFieldToGetter(childFieldWriter.getName()));

    fieldWriter.addDetachStatement(
        "%s.logicalAdd(%s);",
        fieldManager.convertFieldToGetter(fieldName),
        fieldManager.convertFieldToGetter(childFieldWriter.getName()));

    // TODO(rdcastro): use the render() call that receives the SafeHtmlBuilder
    String elementHtml = fieldManager.convertFieldToGetter(childFieldWriter.getName()) + ".render("
        + fieldManager.convertFieldToGetter(stamper) + ")";
    return uiWriter.tokenForSafeHtmlExpression(elem, elementHtml);
  }
}
