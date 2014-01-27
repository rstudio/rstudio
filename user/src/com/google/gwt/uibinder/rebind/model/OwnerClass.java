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
package com.google.gwt.uibinder.rebind.model;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.UiBinderContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Model class with all attributes of the owner class.
 * This includes factories, fields and handlers.
 */
public class OwnerClass {

  /**
   * Map from field name to model.
   */
  private final Map<String, OwnerField> uiFields =
      new TreeMap<String, OwnerField>();

  /**
   * Map from field type to model.
   *
   * This is used for binding resources - for widgets, there may be multiple
   * widgets with the same type, in which case this becomes meaningless.
   */
  private final Map<JClassType, OwnerField> uiFieldTypes =
      new HashMap<JClassType, OwnerField>();

  /**
   * Map from type to the method that produces it.
   */
  private final Map<JClassType, JMethod> uiFactories =
      new HashMap<JClassType, JMethod>();

  /**
   * List of all @UiHandler methods in the owner class.
   */
  private final List<JMethod> uiHandlers = new ArrayList<JMethod>();

  private final MortalLogger logger;

  private final JClassType ownerType;

  private final UiBinderContext context;

  /**
   * Constructor.
   *
   * @param ownerType the type of the owner class
   * @param logger
   */
  public OwnerClass(JClassType ownerType, MortalLogger logger,
      UiBinderContext context) throws UnableToCompleteException {
    this.logger = logger;
    this.ownerType = ownerType;
    this.context = context;
    findUiFields(ownerType);
    findUiFactories(ownerType);
    findUiHandlers(ownerType);
  }

  public JClassType getOwnerType() {
    return ownerType;
  }

  /**
   * Returns the method annotated with @UiFactory which returns the given type.
   *
   * @param forType the type to look for a factory of
   * @return the factory method, or null if none exists
   */
  public JMethod getUiFactoryMethod(JClassType forType) {
    JGenericType genericType = forType.isGenericType();
    if (genericType != null) {
      forType = genericType.getRawType();
    }

    return uiFactories.get(forType);
  }

  /**
   * Gets a field with the given name.
   * It's important to notice that a field may not exist on the owner class even
   * if it has a name in the XML and even has handlers attached to it -  such a
   * field will only exist in the generated binder class.
   *
   * @param name the name of the field to get
   * @return the field descriptor, or null if the owner doesn't have that field
   */
  public OwnerField getUiField(String name) {
    return uiFields.get(name);
  }

  /**
   * Gets the field with the given type.
   * Note that multiple fields can have the same type, so it only makes sense to
   * call this to retrieve resource fields, such as messages and image bundles,
   * for which only one instance is expected.
   *
   * @param type the type of the field
   * @return the field descriptor
   * @deprecated This will die with {@link com.google.gwt.uibinder.attributeparsers.BundleAttributeParser}
   */
  @Deprecated
  public OwnerField getUiFieldForType(JClassType type) {
    return uiFieldTypes.get(type);
  }

  /**
   * Returns a collection of all fields in the owner class.
   */
  public Collection<OwnerField> getUiFields() {
    return uiFields.values();
  }

  /**
   * Returns all the UiHandler methods defined in the owner class.
   */
  public List<JMethod> getUiHandlers() {
    return uiHandlers;
  }

  /**
   * Scans the owner class to find all methods annotated with @UiFactory, and
   * puts them in {@link #uiFactories}.
   *
   * @param ownerType the type of the owner class
   * @throws UnableToCompleteException
   */
  private void findUiFactories(JClassType ownerType)
      throws UnableToCompleteException {
    JMethod[] methods = ownerType.getMethods();
    for (JMethod method : methods) {
      if (method.isAnnotationPresent(UiFactory.class)) {
        JClassType factoryType = method.getReturnType().isClassOrInterface();

        if (factoryType == null) {
          logger.die("Factory return type is not a class in method "
              + method.getName());
        }
        
        JParameterizedType paramType = factoryType.isParameterized();
        if (paramType != null) {
          factoryType = paramType.getRawType();
        }

        if (uiFactories.containsKey(factoryType)) {
          logger.die("Duplicate factory in class "
              + method.getEnclosingType().getName() + " for type "
              + factoryType.getName());
        }

        uiFactories.put(factoryType, method);
      }
    }

    // Recurse to superclass
    JClassType superclass = ownerType.getSuperclass();
    if (superclass != null) {
      findUiFactories(superclass);
    }
  }

  /**
   * Scans the owner class to find all fields annotated with @UiField, and puts
   * them in {@link #uiFields} and {@link #uiFieldTypes}.
   *
   * @param ownerType the type of the owner class
   */
  private void findUiFields(JClassType ownerType)
      throws UnableToCompleteException {
    JField[] fields = ownerType.getFields();
    for (JField field : fields) {
      if (field.isAnnotationPresent(UiField.class)) {
        JClassType ownerFieldType = field.getType().isClassOrInterface();

        if (ownerFieldType == null) {
          logger.die("Field type is not a class in field "
              + field.getName());
        }

        OwnerField ownerField = new OwnerField(field, logger, context);
        String ownerFieldName = field.getName();

        uiFields.put(ownerFieldName, ownerField);
        uiFieldTypes.put(ownerFieldType, ownerField);
      }
    }

    // Recurse to superclass
    JClassType superclass = ownerType.getSuperclass();
    if (superclass != null) {
      findUiFields(superclass);
    }
  }

  /**
   * Scans the owner class to find all methods annotated with @UiHandler, and
   * adds them to their respective fields.
   *
   * @param ownerType the type of the owner class
   */
  private void findUiHandlers(JClassType ownerType) {
    JMethod[] methods = ownerType.getMethods();
    for (JMethod method : methods) {
      if (method.isAnnotationPresent(UiHandler.class)) {
        uiHandlers.add(method);
      }
    }

    // Recurse to superclass
    JClassType superclass = ownerType.getSuperclass();
    if (superclass != null) {
      findUiHandlers(superclass);
    }
  }
}
