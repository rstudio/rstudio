/*
 * Copyright 2007 Google Inc.
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
import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLAttribute;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.uibinder.rebind.messages.AttributeMessage;
import com.google.gwt.uibinder.rebind.model.OwnerFieldClass;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Utility methods for discovering bean-like properties and generating code to
 * initialize them.
 */
public class BeanParser implements ElementParser {

  /**
   * Generates code to initialize all bean attributes on the given element.
   * Includes support for &lt;ui:attribute /&gt; children that will apply
   * to setters
   * @throws UnableToCompleteException
   */
  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    final Map<String, String> setterValues = new HashMap<String, String>();
    final Map<String, String> localizedValues = fetchLocalizedAttributeValues(
        elem, writer);

    final Map<String, String> requiredValues = new HashMap<String, String>();
    final Map<String, JType> unfilledRequiredParams = new HashMap<String, JType>();

    final OwnerFieldClass ownerFieldClass = OwnerFieldClass.getFieldClass(type,
        writer.getLogger());

    // See if there's a factory method
    JAbstractMethod creator = writer.getOwnerClass().getUiFactoryMethod(type);
    if (creator == null) {
      // If not, see if there's a @UiConstructor
      creator = ownerFieldClass.getUiConstructor();
    }

    if (creator != null) {
      for (JParameter param : creator.getParameters()) {
        unfilledRequiredParams.put(param.getName(), param.getType());
      }
    }

    // Work through the localized attribute values and assign them
    // to appropriate constructor params or setters (which had better be
    // ready to accept strings)

    for (Entry<String, String> property : localizedValues.entrySet()) {
      String key = property.getKey();
      String value = property.getValue();

      JType paramType = unfilledRequiredParams.get(key);
      if (paramType != null) {
        if (!isString(writer, paramType)) {
          writer.die("In %s, cannot apply message attribute to non-string "
              + "constructor argument %s %s.", elem,
              paramType.getSimpleSourceName(), key);
        }

        requiredValues.put(key, value);
        unfilledRequiredParams.remove(key);
      } else {
        JMethod setter = ownerFieldClass.getSetter(key);
        JParameter[] params = setter == null ? null : setter.getParameters();

        if (setter == null || !(params.length == 1)
            || !isString(writer, params[0].getType())) {
          writer.die("In %s, no method found to apply message attribute %s",
              elem, key);
        } else {
          setterValues.put(key, value);
        }
      }
    }

    // Now go through the element and dispatch its attributes, remembering
    // that constructor arguments get first dibs
    for (int i = elem.getAttributeCount() - 1; i >= 0; i--) {
      // Backward traversal b/c we're deleting attributes from the xml element

      XMLAttribute attribute = elem.getAttribute(i);

      // Ignore xmlns attributes
      if (attribute.getName().startsWith("xmlns:")) {
        continue;
      }

      String propertyName = attribute.getLocalName();
      if (setterValues.keySet().contains(propertyName)
          || requiredValues.containsKey(propertyName)) {
        writer.die("Duplicate attribute name: %s", propertyName);
      }

      if (unfilledRequiredParams.keySet().contains(propertyName)) {
        JType paramType = unfilledRequiredParams.get(propertyName);
        String value = elem.consumeAttributeWithDefault(attribute.getName(),
            null, paramType);
        if (value == null) {
          writer.die("In %s, unable to parse %s as constructor argument "
              + "of type %s", elem, attribute, paramType.getSimpleSourceName());
        }
        requiredValues.put(propertyName, value);
        unfilledRequiredParams.remove(propertyName);
      } else {
        JMethod setter = ownerFieldClass.getSetter(propertyName);
        if (setter == null) {
          writer.die("In %s, class %s has no appropriate set%s() method", elem,
              elem.getLocalName(), initialCap(propertyName));
        }

        String value = elem.consumeAttributeWithDefault(attribute.getName(),
            null,getParamTypes(setter));

        if (value == null) {
          writer.die("In %s, unable to parse %s.", elem, attribute);
        }
        setterValues.put(propertyName, value);
      }
    }

    if (!unfilledRequiredParams.isEmpty()) {
      StringBuilder b = new StringBuilder(String.format(
          "%s missing required arguments:", elem));
      for (String name : unfilledRequiredParams.keySet()) {
        b.append(" ").append(name);
      }
      writer.die(b.toString());
    }

    if (creator != null) {
      String[] args = makeArgsList(requiredValues, creator);
      if (creator instanceof JMethod) { // Factory method
        String factoryMethod = String.format("owner.%s(%s)", creator.getName(),
            UiBinderWriter.asCommaSeparatedList(args));
        writer.setFieldInitializer(fieldName, factoryMethod);
      } else { // Annotated Constructor
        writer.setFieldInitializerAsConstructor(fieldName, type, args);
      }
    }

    for (String propertyName : setterValues.keySet()) {
      writer.addStatement("%s.set%s(%s);", fieldName, initialCap(propertyName),
          setterValues.get(propertyName));
    }
  }

  /**
   * Fetch the localized attributes that were stored by the
   * AttributeMessageParser.
   */
  private Map<String, String> fetchLocalizedAttributeValues(XMLElement elem,
      UiBinderWriter writer) {
    final Map<String, String> localizedValues = new HashMap<String, String>();

    Collection<AttributeMessage> attributeMessages =
      writer.getMessages().retrieveMessageAttributesFor(elem);

    if (attributeMessages != null) {
      for (AttributeMessage att : attributeMessages) {
        String propertyName = att.getAttribute();
        localizedValues.put(propertyName, att.getMessageUnescaped());
      }
    }
    return localizedValues;
  }

  private JType[] getParamTypes(JMethod setter) {
    JParameter[] params = setter.getParameters();
    JType[] types = new JType[params.length];
    for (int i = 0; i < params.length; i++) {
      types[i] = params[i].getType();
    }
    return types;
  }

  private String initialCap(String propertyName) {
    return propertyName.substring(0, 1).toUpperCase() +
    propertyName.substring(1);
  }

  private boolean isString(UiBinderWriter writer, JType paramType) {
    JType stringType = writer.getOracle().findType(String.class.getName());
    return stringType.equals(paramType);
  }

  private String[] makeArgsList(final Map<String, String> valueMap,
      JAbstractMethod method) {
    JParameter[] params = method.getParameters();
    String[] args = new String[params.length];
    int i = 0;
    for (JParameter param : params) {
      args[i++] = valueMap.get(param.getName());
    }
    return args;
  }
}
