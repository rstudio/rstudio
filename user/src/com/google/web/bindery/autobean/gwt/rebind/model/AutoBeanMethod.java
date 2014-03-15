/*
 * Copyright 2010 Google Inc.
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
package com.google.web.bindery.autobean.gwt.rebind.model;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JEnumConstant;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.editor.rebind.model.ModelUtils;
import com.google.web.bindery.autobean.shared.AutoBean.PropertyName;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes a method implemented by an AutoBean.
 */
public class AutoBeanMethod {
  /**
   * Creates AutoBeanMethods.
   */
  public static class Builder {
    private AutoBeanMethod toReturn = new AutoBeanMethod();

    public AutoBeanMethod build() {
      if (toReturn.action.equals(JBeanMethod.GET)
          || toReturn.action.equals(JBeanMethod.SET)
          || toReturn.action.equals(JBeanMethod.SET_BUILDER)) {
        PropertyName annotation = toReturn.method.getAnnotation(PropertyName.class);
        if (annotation != null) {
          toReturn.propertyName = annotation.value();
        } else {
          toReturn.propertyName = toReturn.action.inferName(toReturn.method);
        }
      }

      try {
        return toReturn;
      } finally {
        toReturn = null;
      }
    }

    public void setAction(JBeanMethod action) {
      toReturn.action = action;
    }

    public void setMethod(JMethod method) {
      toReturn.method = method;
      TypeOracle oracle = method.getEnclosingType().getOracle();

      JType returnType = method.getReturnType();
      toReturn.isValueType = ModelUtils.isValueType(oracle, returnType);

      if (!toReturn.isValueType) {
        // See if it's a collection or a map
        JClassType returnClass = returnType.isClassOrInterface();
        JClassType collectionInterface = oracle.findType(Collection.class.getCanonicalName());
        JClassType mapInterface = oracle.findType(Map.class.getCanonicalName());
        if (collectionInterface.isAssignableFrom(returnClass)) {
          JClassType[] parameterizations = ModelUtils.findParameterizationOf(
              collectionInterface, returnClass);
          toReturn.elementType = parameterizations[0];
          maybeProcessEnumType(toReturn.elementType);
        } else if (mapInterface.isAssignableFrom(returnClass)) {
          JClassType[] parameterizations = ModelUtils.findParameterizationOf(
              mapInterface, returnClass);
          toReturn.keyType = parameterizations[0];
          toReturn.valueType = parameterizations[1];
          maybeProcessEnumType(toReturn.keyType);
          maybeProcessEnumType(toReturn.valueType);
        }
      } else {
        maybeProcessEnumType(returnType);
      }
    }

    public void setNoWrap(boolean noWrap) {
      toReturn.isNoWrap = noWrap;
    }

    public void setStaticImp(JMethod staticImpl) {
      toReturn.staticImpl = staticImpl;
    }

    /**
     * Call {@link #processEnumType(JEnumType)} if {@code type} is a
     * {@link JEnumType}.
     */
    private void maybeProcessEnumType(JType type) {
      assert type != null : "type == null";
      JEnumType enumType = type.isEnum();
      if (enumType != null) {
        processEnumType(enumType);
      }
    }

    /**
     * Adds a JEnumType to the AutoBeanMethod's enumMap so that the
     * AutoBeanFactoryGenerator can embed extra metadata about the enum values.
     */
    private void processEnumType(JEnumType enumType) {
      Map<JEnumConstant, String> map = toReturn.enumMap;
      if (map == null) {
        map = toReturn.enumMap = new LinkedHashMap<JEnumConstant, String>();
      }
      for (JEnumConstant e : enumType.getEnumConstants()) {
        String name = getEnumName(e);
        map.put(e, name);
      }
    }
  }

  static String getEnumName(JEnumConstant e) {
    String name;
    PropertyName annotation = e.getAnnotation(PropertyName.class);
    if (annotation == null) {
      name = e.getName();
    } else {
      name = annotation.value();
    }
    return name;
  }

  private JBeanMethod action;
  private JClassType elementType;
  private Map<JEnumConstant, String> enumMap;
  private JClassType keyType;
  private JMethod method;
  private boolean isNoWrap;
  private boolean isValueType;
  private String propertyName;
  private JMethod staticImpl;
  private JClassType valueType;

  private AutoBeanMethod() {
  }

  public JBeanMethod getAction() {
    return action;
  }

  public JClassType getElementType() {
    return elementType;
  }

  public Map<JEnumConstant, String> getEnumMap() {
    return enumMap;
  }

  public JClassType getKeyType() {
    return keyType;
  }

  public JMethod getMethod() {
    return method;
  }

  public String getPropertyName() {
    return propertyName;
  }

  /**
   * If the AutoBean method was declared in a type containing a
   * {@link com.google.gwt.editor.client.AutoBean.Category Category} annotation,
   * this method will return the static implementation.
   */
  public JMethod getStaticImpl() {
    return staticImpl;
  }

  public JClassType getValueType() {
    return valueType;
  }

  public boolean hasEnumMap() {
    return enumMap != null;
  }

  public boolean isCollection() {
    return elementType != null;
  }

  public boolean isMap() {
    return keyType != null;
  }

  public boolean isNoWrap() {
    return isNoWrap;
  }

  public boolean isValueType() {
    return isValueType;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return method.toString();
  }
}
