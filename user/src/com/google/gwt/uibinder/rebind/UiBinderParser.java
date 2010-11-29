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
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;
import com.google.gwt.uibinder.rebind.model.ImplicitClientBundle;
import com.google.gwt.uibinder.rebind.model.ImplicitCssResource;
import com.google.gwt.uibinder.rebind.model.ImplicitDataResource;
import com.google.gwt.uibinder.rebind.model.ImplicitImageResource;
import com.google.gwt.uibinder.rebind.model.OwnerField;

import java.util.LinkedHashSet;

/**
 * Parses the root UiBinder element, and kicks of the parsing of the rest of the
 * document.
 */
public class UiBinderParser {

  private enum Resource {
    DATA {
      @Override
      void create(UiBinderParser parser, XMLElement elem)
          throws UnableToCompleteException {
        parser.createData(elem);
      }
    },
    IMAGE {
      @Override
      void create(UiBinderParser parser, XMLElement elem)
          throws UnableToCompleteException {
        parser.createImage(elem);
      }
    },
    IMPORT {
      @Override
      void create(UiBinderParser parser, XMLElement elem)
          throws UnableToCompleteException {
        parser.createImport(elem);
      }
    },
    STYLE {
      @Override
      void create(UiBinderParser parser, XMLElement elem)
          throws UnableToCompleteException {
        parser.createStyle(elem);
      }
    },
    WITH {
      @Override
      void create(UiBinderParser parser, XMLElement elem)
          throws UnableToCompleteException {
        parser.createResource(elem);
      }
    };

    abstract void create(UiBinderParser parser, XMLElement elem)
        throws UnableToCompleteException;
  }

  private static final String FLIP_RTL_ATTRIBUTE = "flipRtl";
  private static final String FIELD_ATTRIBUTE = "field";
  private static final String REPEAT_STYLE_ATTRIBUTE = "repeatStyle";
  private static final String SOURCE_ATTRIBUTE = "src";
  private static final String TYPE_ATTRIBUTE = "type";

  // TODO(rjrjr) Make all the ElementParsers receive their dependencies via
  // constructor like this one does, and make this an ElementParser. I want
  // guice!!!

  private static final String IMPORT_ATTRIBUTE = "import";
  private static final String TAG = "UiBinder";
  private final UiBinderWriter writer;
  private final TypeOracle oracle;
  private final MessagesWriter messagesWriter;
  private final FieldManager fieldManager;
  private final ImplicitClientBundle bundleClass;
  private final JClassType cssResourceType;
  private final JClassType imageResourceType;

  private final JClassType dataResourceType;

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
    this.dataResourceType = oracle.findType(DataResource.class.getCanonicalName());
  }

  /**
   * Parses the root UiBinder element, and kicks off the parsing of the rest of
   * the document.
   */
  public String parse(XMLElement elem) throws UnableToCompleteException {
    if (!writer.isBinderElement(elem)) {
      writer.die(elem, "Bad prefix on <%s:%s>? The root element must be in "
          + "xml namespace \"%s\" (usually with prefix \"ui:\"), "
          + "but this has prefix \"%s\"", elem.getPrefix(),
          elem.getLocalName(), UiBinderGenerator.BINDER_URI, elem.getPrefix());
    }

    if (!TAG.equals(elem.getLocalName())) {
      writer.die(elem, "Root element must be %s:%", elem.getPrefix(), TAG);
    }

    findResources(elem);
    messagesWriter.findMessagesConfig(elem);
    XMLElement uiRoot = elem.consumeSingleChildElement();
    return writer.parseElementToField(uiRoot);
  }

  private JClassType consumeCssResourceType(XMLElement elem)
      throws UnableToCompleteException {
    String typeName = elem.consumeRawAttribute(TYPE_ATTRIBUTE, null);
    if (typeName == null) {
      return cssResourceType;
    }

    return findCssResourceType(elem, typeName);
  }

  private JClassType consumeTypeAttribute(XMLElement elem)
      throws UnableToCompleteException {
    String resourceTypeName = elem.consumeRequiredRawAttribute(TYPE_ATTRIBUTE);

    JClassType resourceType = oracle.findType(resourceTypeName);
    if (resourceType == null) {
      writer.die(elem, "No such type %s", resourceTypeName);
    }

    return resourceType;
  }

  /**
   * Interprets <ui:data> elements.
   */
  private void createData(XMLElement elem) throws UnableToCompleteException {
    String name = elem.consumeRequiredRawAttribute(FIELD_ATTRIBUTE);
    String source = elem.consumeRequiredRawAttribute(SOURCE_ATTRIBUTE);
    ImplicitDataResource dataMethod = bundleClass.createDataResource(name,
        source);
    FieldWriter field = fieldManager.registerField(dataResourceType,
        dataMethod.getName());
    field.setInitializer(String.format("%s.%s()", bundleClass.getFieldName(),
        dataMethod.getName()));
  }

  /**
   * Interprets <ui:image> elements.
   */
  private void createImage(XMLElement elem) throws UnableToCompleteException {
    String name = elem.consumeRequiredRawAttribute(FIELD_ATTRIBUTE);
    // @source is optional on ImageResource
    String source = elem.consumeRawAttribute(SOURCE_ATTRIBUTE, null);

    Boolean flipRtl = elem.consumeBooleanConstantAttribute(FLIP_RTL_ATTRIBUTE);

    RepeatStyle repeatStyle = null;
    if (elem.hasAttribute(REPEAT_STYLE_ATTRIBUTE)) {
      String value = elem.consumeRawAttribute(REPEAT_STYLE_ATTRIBUTE);
      try {
        repeatStyle = RepeatStyle.valueOf(value);
      } catch (IllegalArgumentException e) {
        writer.die(elem, "Bad repeatStyle value %s", value);
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
   * Process <code>&lt;ui:import field="com.example.Blah.CONSTANT"></code>.
   */
  private void createImport(XMLElement elem) throws UnableToCompleteException {
    String rawFieldName = elem.consumeRequiredRawAttribute(FIELD_ATTRIBUTE);
    if (elem.getAttributeCount() > 0) {
      writer.die(elem, "Should only find attribute \"%s\"", FIELD_ATTRIBUTE);
    }

    int idx = rawFieldName.lastIndexOf('.');
    if (idx < 1) {
      writer.die(elem, "Attribute %s does not look like a static import "
          + "reference", FIELD_ATTRIBUTE);
    }
    String enclosingName = rawFieldName.substring(0, idx);
    String constantName = rawFieldName.substring(idx + 1);

    JClassType enclosingType = oracle.findType(enclosingName);
    if (enclosingType == null) {
      writer.die(elem, "Unable to locate type %s", enclosingName);
    }

    if ("*".equals(constantName)) {
      for (JField field : enclosingType.getFields()) {
        if (!field.isStatic()) {
          continue;
        } else if (field.isPublic()) {
          // OK
        } else if (field.isProtected() || field.isPrivate()) {
          continue;
        } else if (!enclosingType.getPackage().equals(
            writer.getOwnerClass().getOwnerType().getPackage())) {
          // package-protected in another package
          continue;
        }
        createSingleImport(elem, enclosingType, enclosingName + "."
            + field.getName(), field.getName());
      }
    } else {
      createSingleImport(elem, enclosingType, rawFieldName, constantName);
    }
  }

  /**
   * Interprets <ui:with> elements.
   */
  private void createResource(XMLElement elem) throws UnableToCompleteException {
    String resourceName = elem.consumeRequiredRawAttribute(FIELD_ATTRIBUTE);
    JClassType resourceType = consumeTypeAttribute(elem);
    if (elem.getAttributeCount() > 0) {
      writer.die(elem, "Should only find attributes \"field\" and \"type\"");
    }

    FieldWriter fieldWriter = fieldManager.registerField(resourceType,
        resourceName);
    OwnerField ownerField = writer.getOwnerClass().getUiField(resourceName);

    /* Perhaps it is provided via @UiField */

    if (ownerField != null) {
      if (!resourceType.getErasedType().equals(ownerField.getType().getRawType()
          .getErasedType())) {
        writer.die(elem, "Type must match %s", ownerField);
      }
      if (ownerField.isProvided()) {
        String initializer;
        if (writer.getDesignTime().isDesignTime()) {
          String typeName = ownerField.getType().getRawType().getQualifiedSourceName();
          initializer = writer.getDesignTime().getProvidedField(typeName,
              ownerField.getName());
        } else {
          initializer = "owner." + ownerField.getName();
        }
        fieldWriter.setInitializer(initializer);
        return;
      }
    }

    /* Nope. Maybe a @UiFactory will make it */

    JMethod factoryMethod = writer.getOwnerClass().getUiFactoryMethod(
        resourceType);
    if (factoryMethod != null) {
      String initializer;
      if (writer.getDesignTime().isDesignTime()) {
        String typeName = factoryMethod.getReturnType().getQualifiedSourceName();
        initializer = writer.getDesignTime().getProvidedFactory(typeName,
            factoryMethod.getName(), "");
      } else {
        initializer = String.format("owner.%s()", factoryMethod.getName());
      }
      fieldWriter.setInitializer(initializer);
    }

    /*
     * If neither of the above, the FieldWriter's default GWT.create call will
     * do just fine.
     */
  }

  private void createSingleImport(XMLElement elem, JClassType enclosingType,
      String rawFieldName, String constantName)
      throws UnableToCompleteException {
    JField field = enclosingType.findField(constantName);
    if (field == null) {
      writer.die(elem, "Unable to locate a field named %s in %s", constantName,
          enclosingType.getQualifiedSourceName());
    } else if (!field.isStatic()) {
      writer.die(elem, "Field %s in type %s is not static", constantName,
          enclosingType.getQualifiedSourceName());
    }

    JType importType = field.getType();
    JClassType fieldType;
    if (importType instanceof JPrimitiveType) {
      fieldType = oracle.findType(((JPrimitiveType) importType).getQualifiedBoxedSourceName());
    } else {
      fieldType = (JClassType) importType;
    }

    FieldWriter fieldWriter = fieldManager.registerField(fieldType,
        constantName);
    fieldWriter.setInitializer(rawFieldName);
  }

  private void createStyle(XMLElement elem) throws UnableToCompleteException {
    String body = elem.consumeUnescapedInnerText();
    String[] source = elem.consumeRawArrayAttribute(SOURCE_ATTRIBUTE);

    if (0 == body.length() && 0 == source.length) {
      writer.die(elem, "Must have either a src attribute or body text");
    }

    String name = elem.consumeRawAttribute(FIELD_ATTRIBUTE, "style");
    JClassType publicType = consumeCssResourceType(elem);

    String[] importTypeNames = elem.consumeRawArrayAttribute(IMPORT_ATTRIBUTE);
    LinkedHashSet<JClassType> importTypes = new LinkedHashSet<JClassType>();
    for (String type : importTypeNames) {
      importTypes.add(findCssResourceType(elem, type));
    }

    ImplicitCssResource cssMethod = bundleClass.createCssResource(name, source,
        publicType, body, importTypes);

    FieldWriter field = fieldManager.registerFieldForGeneratedCssResource(cssMethod);
    field.setInitializer(String.format("%s.%s()", bundleClass.getFieldName(),
        cssMethod.getName()));
  }

  private JClassType findCssResourceType(XMLElement elem, String typeName)
      throws UnableToCompleteException {
    JClassType publicType = oracle.findType(typeName);
    if (publicType == null) {
      writer.die(elem, "No such type %s", typeName);
    }

    if (!publicType.isAssignableTo(cssResourceType)) {
      writer.die(elem, "Type %s does not extend %s",
          publicType.getQualifiedSourceName(),
          cssResourceType.getQualifiedSourceName());
    }
    return publicType;
  }

  private void findResources(XMLElement binderElement)
      throws UnableToCompleteException {
    binderElement.consumeChildElements(new XMLElement.Interpreter<Boolean>() {
      public Boolean interpretElement(XMLElement elem)
          throws UnableToCompleteException {

        if (writer.isBinderElement(elem)) {
          try {
            Resource.valueOf(elem.getLocalName().toUpperCase()).create(
                UiBinderParser.this, elem);
          } catch (IllegalArgumentException e) {
            writer.die(elem,
                "Unknown tag %s, or is not appropriate as a top level element",
                elem.getLocalName());
          }
          return true;
        }
        return false; // leave it be
      }
    });
  }
}
