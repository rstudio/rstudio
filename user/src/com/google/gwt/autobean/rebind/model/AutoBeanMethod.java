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
package com.google.gwt.autobean.rebind.model;

import com.google.gwt.autobean.shared.AutoBean.PropertyName;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JEnumConstant;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.editor.rebind.model.ModelUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes a method implemented by an AutoBean.
 */
public class AutoBeanMethod {
  /**
   * Describes the type of method that was invoked.
   */
  public enum Action {
    GET {
      @Override
      String inferName(JMethod method) {
        if (JPrimitiveType.BOOLEAN.equals(method.getReturnType())) {
          String name = method.getName();
          if (name.startsWith("is") && name.length() > 2) {
            name = Character.toLowerCase(name.charAt(2))
                + (name.length() >= 4 ? name.substring(3) : "");
            return name;
          }
        }
        return super.inferName(method);
      }

      @Override
      boolean matches(JMethod method) {
        if (method.getParameters().length > 0) {
          return false;
        }
        String name = method.getName();

        // Allow boolean isFoo() or boolean hasFoo();
        if (JPrimitiveType.BOOLEAN.equals(method.getReturnType())) {
          if (name.startsWith("is") && name.length() > 2) {
            return true;
          }
          if (name.startsWith("has") && name.length() > 3) {
            return true;
          }
        }
        if (name.startsWith("get") && name.length() > 3) {
          return true;
        }
        return false;
      }
    },
    SET {
      @Override
      boolean matches(JMethod method) {
        if (!JPrimitiveType.VOID.equals(method.getReturnType())) {
          return false;
        }
        if (method.getParameters().length != 1) {
          return false;
        }
        String name = method.getName();
        if (name.startsWith("set") && name.length() > 3) {
          return true;
        }
        return false;
      }
    },
    CALL {
      /**
       * Matches all leftover methods.
       */
      @Override
      boolean matches(JMethod method) {
        return true;
      }
    };

    /**
     * Determine which Action a method maps to.
     */
    public static Action which(JMethod method) {
      for (Action action : Action.values()) {
        if (action.matches(method)) {
          return action;
        }
      }
      throw new RuntimeException("CALL should have matched");
    }

    /**
     * Infer the name of a property from the method.
     */
    String inferName(JMethod method) {
      String name = method.getName();
      name = Character.toLowerCase(name.charAt(3))
          + (name.length() >= 5 ? name.substring(4) : "");
      return name;
    }

    /**
     * Returns {@code true} if the Action matches the method.
     */
    abstract boolean matches(JMethod method);
  }

  /**
   * Creates AutoBeanMethods.
   */
  public static class Builder {
    private AutoBeanMethod toReturn = new AutoBeanMethod();

    public AutoBeanMethod build() {
      if (toReturn.action.equals(Action.GET)
          || toReturn.action.equals(Action.SET)) {
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

    public void setAction(Action action) {
      toReturn.action = action;
    }

    public void setMethod(JMethod method) {
      toReturn.method = method;
      TypeOracle oracle = method.getEnclosingType().getOracle();

      toReturn.isValueType = ModelUtils.isValueType(oracle,
          method.getReturnType());

      if (!toReturn.isValueType) {
        // See if it's a collection or a map
        JClassType returnClass = method.getReturnType().isClassOrInterface();
        JClassType collectionInterface = oracle.findType(Collection.class.getCanonicalName());
        JClassType mapInterface = oracle.findType(Map.class.getCanonicalName());
        if (collectionInterface.isAssignableFrom(returnClass)) {
          JClassType[] parameterizations = ModelUtils.findParameterizationOf(
              collectionInterface, returnClass);
          toReturn.elementType = parameterizations[0];
        } else if (mapInterface.isAssignableFrom(returnClass)) {
          JClassType[] parameterizations = ModelUtils.findParameterizationOf(
              mapInterface, returnClass);
          toReturn.keyType = parameterizations[0];
          toReturn.valueType = parameterizations[1];
        }
      }

      JEnumType enumType = method.getReturnType().isEnum();
      if (enumType != null) {
        Map<JEnumConstant, String> map = new LinkedHashMap<JEnumConstant, String>();
        for (JEnumConstant e : enumType.getEnumConstants()) {
          String name;
          PropertyName annotation = e.getAnnotation(PropertyName.class);
          if (annotation == null) {
            name = e.getName();
          } else {
            name = annotation.value();
          }
          map.put(e, name);
        }
        toReturn.enumMap = map;
      }
    }

    public void setNoWrap(boolean noWrap) {
      toReturn.isNoWrap = noWrap;
    }

    public void setStaticImp(JMethod staticImpl) {
      toReturn.staticImpl = staticImpl;
    }
  }

  private Action action;
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

  public Action getAction() {
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

  public boolean isCollection() {
    return elementType != null;
  }

  public boolean isEnum() {
    return enumMap != null;
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