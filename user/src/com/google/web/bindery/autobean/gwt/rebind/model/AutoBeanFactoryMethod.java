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
import com.google.gwt.core.ext.typeinfo.JMethod;

/**
 * Represents a single method in an AutoBeanFactory interface.
 */
public class AutoBeanFactoryMethod {
  /**
   * Builds AutoBeanFactoryMethods.
   */
  public static class Builder {
    private AutoBeanFactoryMethod toReturn = new AutoBeanFactoryMethod();

    public AutoBeanFactoryMethod build() {
      try {
        return toReturn;
      } finally {
        toReturn = null;
      }
    }

    public void setAutoBeanType(AutoBeanType type) {
      toReturn.autoBeanType = type;
    }

    public void setMethod(JMethod method) {
      setName(method.getName());
      setReturnType(method.getReturnType().isClassOrInterface());
      if (method.getParameters().length == 1) {
        setWrappedType(method.getParameters()[0].getType().isClassOrInterface());
      }
    }

    public void setName(String name) {
      toReturn.name = name;
    }

    public void setReturnType(JClassType returnType) {
      toReturn.returnType = returnType;
    }

    public void setWrappedType(JClassType wrapped) {
      toReturn.wrappedType = wrapped;
    }
  }

  private AutoBeanType autoBeanType;
  private JClassType wrappedType;
  private String name;
  private JClassType returnType;

  private AutoBeanFactoryMethod() {
  }

  public AutoBeanType getAutoBeanType() {
    return autoBeanType;
  }

  public String getName() {
    return name;
  }

  public JClassType getReturnType() {
    return returnType;
  }

  public JClassType getWrappedType() {
    return wrappedType;
  }

  public boolean isWrapper() {
    return wrappedType != null;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return name;
  }
}
