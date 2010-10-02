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
package com.google.gwt.editor.rebind.model;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

import java.util.Collection;

/**
 * Describes a method implemented by an AutoBean.
 */
public class AutoBeanMethod {
  /**
   * Describes the type of method that was invoked.
   */
  public enum Action {
    GET, SET, CALL
  }
  /**
   * Creates AutoBeanMethods.
   */
  public static class Builder {
    private AutoBeanMethod toReturn = new AutoBeanMethod();

    public AutoBeanMethod build() {
      if (toReturn.action.equals(Action.GET)
          || toReturn.action.equals(Action.SET)) {
        String name = toReturn.method.getName();
        // setFoo
        toReturn.propertyName = Character.toLowerCase(name.charAt(3))
            + (name.length() >= 5 ? name.substring(4) : "");
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
        // See if it's a collection
        JClassType returnClass = method.getReturnType().isClassOrInterface();
        JClassType collectionInterface = oracle.findType(Collection.class.getCanonicalName());
        if (collectionInterface.isAssignableFrom(returnClass)) {
          toReturn.elementType = ModelUtils.findParameterizationOf(
              collectionInterface, returnClass)[0];
        }
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
  private JMethod method;
  private boolean isNoWrap;
  private boolean isValueType;
  private String propertyName;
  private JMethod staticImpl;

  private AutoBeanMethod() {
  }

  public Action getAction() {
    return action;
  }

  public JClassType getElementType() {
    return elementType;
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

  public boolean isCollection() {
    return elementType != null;
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