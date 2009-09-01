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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;
import com.google.gwt.uibinder.rebind.model.OwnerField;

/**
 * Parses the root UiBinder element, and kicks of the parsing of the rest of the
 * document.
 */
public class UiBinderParser {
  // TODO(rjrjr) Make all the ElementParsers receive their dependencies via
  // constructor like this one does, and make this an ElementParser

  private final UiBinderWriter writer;
  private final MessagesWriter messagesWriter;
  private final FieldManager fieldManager;

  public UiBinderParser(UiBinderWriter writer, MessagesWriter messagesWriter,
      FieldManager fieldManager) {
    this.writer = writer;
    this.messagesWriter = messagesWriter;
    this.fieldManager = fieldManager;
  }

  /**
   * Parses the root UiBinder element, and kicks off the parsing of the rest of
   * the document.
   */
  public String parse(XMLElement elem)
      throws UnableToCompleteException {
    findResources(elem);
    messagesWriter.findMessagesConfig(elem);
    XMLElement uiRoot = elem.consumeSingleChildElement();
    return writer.parseElementToField(uiRoot);
  }

  /**
   * Interprets <ui:with> elements.
   */
  private void createResource(XMLElement elem)
      throws UnableToCompleteException {
    String resourceName = elem.consumeRequiredAttribute("name");
    String resourceTypeName = elem.consumeRequiredAttribute("type");

    JClassType resourceType = writer.getOracle().findType(resourceTypeName);
    if (resourceType == null) {
      writer.die("In %s, no such type %s", elem, resourceTypeName);
    }

    if (elem.getAttributeCount() > 0) {
      writer.die("In %s, should only find attributes \"name\" and \"type\"");
    }

    FieldWriter fieldWriter = fieldManager.registerField(resourceTypeName,
        resourceName);
    OwnerField ownerField = writer.getOwnerClass().getUiField(resourceName);

    // Perhaps it is provided via @UiField

    if (ownerField != null) {
      if (!resourceType.equals(ownerField.getType().getRawType())) {
        writer.die("In %s, type must match %s", ownerField);
      }

      if (ownerField.isProvided()) {
        fieldWriter.setInitializer("owner." + ownerField.getName());
        return;
      }
    }

    // Nope. Maybe a @UiFactory will make it

    JMethod factoryMethod = writer.getOwnerClass().getUiFactoryMethod(
        resourceType);
    if (factoryMethod != null) {
      fieldWriter.setInitializer(String.format("owner.%s()",
          factoryMethod.getName()));
    }

    // If neither of the above, the FieldWriter's default GWT.create call will
    // do just fine.
  }

  private void findResources(XMLElement binderElement)
      throws UnableToCompleteException {
    binderElement.consumeChildElements(new XMLElement.Interpreter<Boolean>() {
      public Boolean interpretElement(XMLElement elem)
          throws UnableToCompleteException {
        if (!(writer.isBinderElement(elem)
            && "with".equals(elem.getLocalName()))) {
          return false; // Not of interest, do not consume
        }

        createResource(elem);

        return true; // Yum
      }
    });
  }

}
