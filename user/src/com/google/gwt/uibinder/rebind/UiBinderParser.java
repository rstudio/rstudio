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
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;
import com.google.gwt.uibinder.rebind.model.ImplicitClientBundle;
import com.google.gwt.uibinder.rebind.model.ImplicitCssResource;
import com.google.gwt.uibinder.rebind.model.ImplicitImageResource;
import com.google.gwt.uibinder.rebind.model.OwnerField;

/**
 * Parses the root UiBinder element, and kicks of the parsing of the rest of the
 * document.
 */
public class UiBinderParser {

  private static final String FLIP_RTL_ATTRIBUTE = "flipRtl";
  private static final String FIELD_ATTRIBUTE = "field";
  private static final String SOURCE_ATTRIBUTE = "src";
  private static final String REPEAT_STYLE_ATTRIBUTE = "repeatStyle";

  // TODO(rjrjr) Make all the ElementParsers receive their dependencies via
  // constructor like this one does, and make this an ElementParser. I want
  // guice!!!

  private final UiBinderWriter writer;
  private final TypeOracle oracle;
  private final MessagesWriter messagesWriter;
  private final FieldManager fieldManager;
  private final ImplicitClientBundle bundleClass;
  private final JClassType cssResourceType;
  private final JClassType imageResourceType;

  public UiBinderParser(UiBinderWriter writer, MessagesWriter messagesWriter,
      FieldManager fieldManager, TypeOracle oracle,
      ImplicitClientBundle bundleClass) {
    this.writer = writer;
    this.oracle = oracle;
    this.messagesWriter = messagesWriter;
    this.fieldManager = fieldManager;
    this.bundleClass = bundleClass;
    this.cssResourceType = oracle.findType(CssResource.class.getCanonicalName());
    this.imageResourceType = oracle.findType(ImageResource.class.getCanonicalName());
  }

  /**
   * Parses the root UiBinder element, and kicks off the parsing of the rest of
   * the document.
   */
  public String parse(XMLElement elem) throws UnableToCompleteException {
    // TODO(rjrjr) Clearly need to break these find* methods out into their own
    // parsers, an so need a registration scheme for uibinder-specific parsers
    findStyles(elem);
    findResources(elem);
    findImages(elem);
    messagesWriter.findMessagesConfig(elem);
    XMLElement uiRoot = elem.consumeSingleChildElement();
    return writer.parseElementToField(uiRoot);
  }

  private JClassType consumeCssResourceType(XMLElement elem)
      throws UnableToCompleteException {
    String typeName = elem.consumeAttribute("type", null);
    if (typeName == null) {
      return cssResourceType;
    }

    JClassType publicType = oracle.findType(typeName);
    if (publicType == null) {
      writer.die("In %s, no such type %s", elem, typeName);
    }

    if (!publicType.isAssignableTo(cssResourceType)) {
      writer.die("In %s, type %s does not extend %s", elem,
          publicType.getQualifiedSourceName(),
          cssResourceType.getQualifiedSourceName());
    }
    return publicType;
  }

  private JClassType consumeTypeAttribute(XMLElement elem)
      throws UnableToCompleteException {
    String resourceTypeName = elem.consumeRequiredAttribute("type");

    JClassType resourceType = oracle.findType(resourceTypeName);
    if (resourceType == null) {
      writer.die("In %s, no such type %s", elem, resourceTypeName);
    }

    return resourceType;
  }

  /**
   * Interprets <ui:image> elements
   */
  private void createImage(XMLElement elem) throws UnableToCompleteException {
    String name = elem.consumeRequiredAttribute(FIELD_ATTRIBUTE);
    String source = elem.consumeAttribute(SOURCE_ATTRIBUTE, null); // @source is
                                                                   // optional
                                                                   // on
                                                                   // ImageResource

    Boolean flipRtl = null;
    if (elem.hasAttribute(FLIP_RTL_ATTRIBUTE)) {
      flipRtl = elem.consumeBooleanAttribute(FLIP_RTL_ATTRIBUTE);
    }

    RepeatStyle repeatStyle = null;
    if (elem.hasAttribute(REPEAT_STYLE_ATTRIBUTE)) {
      String value = elem.consumeAttribute(REPEAT_STYLE_ATTRIBUTE);
      try {
        repeatStyle = RepeatStyle.valueOf(value);
      } catch (IllegalArgumentException e) {
        writer.die("In %s, bad repeatStyle value %s", elem, value);
      }
    }

    ImplicitImageResource imageMethod = bundleClass.createImageResource(name,
        source, flipRtl, repeatStyle);

    FieldWriter field = fieldManager.registerField(imageResourceType,
        imageMethod.getName());
    field.setInitializer(String.format("%s.%s()", bundleClass.getFieldName(),
        imageMethod.getName()));
  }

  /**
   * Interprets <ui:with> elements.
   */
  private void createResource(XMLElement elem) throws UnableToCompleteException {
    String resourceName = elem.consumeRequiredAttribute(FIELD_ATTRIBUTE);
    JClassType resourceType = consumeTypeAttribute(elem);
    if (elem.getAttributeCount() > 0) {
      writer.die("In %s, should only find attributes \"field\" and \"type\"",
          elem);
    }

    FieldWriter fieldWriter = fieldManager.registerField(resourceType,
        resourceName);
    OwnerField ownerField = writer.getOwnerClass().getUiField(resourceName);

    /* Perhaps it is provided via @UiField */

    if (ownerField != null) {
      if (!resourceType.equals(ownerField.getType().getRawType())) {
        writer.die("In %s, type must match %s", elem, ownerField);
      }

      if (ownerField.isProvided()) {
        fieldWriter.setInitializer("owner." + ownerField.getName());
        return;
      }
    }

    /* Nope. Maybe a @UiFactory will make it */

    JMethod factoryMethod = writer.getOwnerClass().getUiFactoryMethod(
        resourceType);
    if (factoryMethod != null) {
      fieldWriter.setInitializer(String.format("owner.%s()",
          factoryMethod.getName()));
    }

    /*
     * If neither of the above, the FieldWriter's default GWT.create call will
     * do just fine.
     */
  }

  private void createStyle(XMLElement elem) throws UnableToCompleteException {
    String body = elem.consumeUnescapedInnerText();
    if (body.length() > 0 && elem.hasAttribute(SOURCE_ATTRIBUTE)) {
      writer.die(
          "In %s, cannot use both a source attribute and inline css text.",
          elem);
    }

    String source = elem.consumeAttribute(SOURCE_ATTRIBUTE);
    String name = elem.consumeAttribute(FIELD_ATTRIBUTE, "style");
    JClassType publicType = consumeCssResourceType(elem);

    ImplicitCssResource cssMethod = bundleClass.createCssResource(name, source,
        publicType, body);

    FieldWriter field = fieldManager.registerFieldOfGeneratedType(
        cssMethod.getPackageName(), cssMethod.getClassName(),
        cssMethod.getName());
    field.setInitializer(String.format("%s.%s()", bundleClass.getFieldName(),
        cssMethod.getName()));
  }

  private void findImages(XMLElement binderElement)
      throws UnableToCompleteException {
    binderElement.consumeChildElements(new XMLElement.Interpreter<Boolean>() {
      public Boolean interpretElement(XMLElement elem)
          throws UnableToCompleteException {
        if (!(writer.isBinderElement(elem) && "image".equals(elem.getLocalName()))) {
          return false; // Not of interest, do not consume
        }

        createImage(elem);

        return true; // Yum
      }
    });
  }

  private void findResources(XMLElement binderElement)
      throws UnableToCompleteException {
    binderElement.consumeChildElements(new XMLElement.Interpreter<Boolean>() {
      public Boolean interpretElement(XMLElement elem)
          throws UnableToCompleteException {
        if (!(writer.isBinderElement(elem) && "with".equals(elem.getLocalName()))) {
          return false; // Not of interest, do not consume
        }

        createResource(elem);

        return true; // Yum
      }
    });
  }

  private void findStyles(XMLElement binderElement)
      throws UnableToCompleteException {
    binderElement.consumeChildElements(new XMLElement.Interpreter<Boolean>() {
      public Boolean interpretElement(XMLElement elem)
          throws UnableToCompleteException {
        if (!(writer.isBinderElement(elem) && "style".equals(elem.getLocalName()))) {
          return false; // Not of interest, do not consume
        }

        createStyle(elem);

        return true; // consume
      }
    });
  }
}
