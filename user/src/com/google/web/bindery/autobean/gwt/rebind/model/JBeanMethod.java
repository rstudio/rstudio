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
package com.google.web.bindery.autobean.gwt.rebind.model;

import static com.google.web.bindery.autobean.vm.impl.BeanMethod.GET_PREFIX;
import static com.google.web.bindery.autobean.vm.impl.BeanMethod.HAS_PREFIX;
import static com.google.web.bindery.autobean.vm.impl.BeanMethod.IS_PREFIX;
import static com.google.web.bindery.autobean.vm.impl.BeanMethod.SET_PREFIX;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;

import java.beans.Introspector;

/**
 * Common utility code for matching {@link JMethod} and against bean-style
 * accessor semantics.
 * 
 * @see com.google.web.bindery.autobean.vm.impl.BeanMethod
 */
public enum JBeanMethod {
  GET {
    @Override
    public String inferName(JMethod method) {
      if (isBooleanProperty(method) && method.getName().startsWith(IS_PREFIX)) {
        return Introspector.decapitalize(method.getName().substring(2));
      }
      return super.inferName(method);
    }

    @Override
    public boolean matches(JMethod method) {
      if (method.getParameters().length > 0) {
        return false;
      }

      if (isBooleanProperty(method)) {
        return true;
      }

      String name = method.getName();
      if (name.startsWith(GET_PREFIX) && name.length() > 3) {
        return true;
      }
      return false;
    }

    /**
     * Returns {@code true} if the method matches {@code boolean isFoo()} or
     * {@code boolean hasFoo()} property accessors.
     */
    private boolean isBooleanProperty(JMethod method) {
      JType returnType = method.getReturnType();
      if (JPrimitiveType.BOOLEAN.equals(returnType)
          || method.getEnclosingType().getOracle().findType(
              Boolean.class.getCanonicalName()).equals(returnType)) {
        String name = method.getName();
        if (name.startsWith(IS_PREFIX) && name.length() > 2) {
          return true;
        }
        if (name.startsWith(HAS_PREFIX) && name.length() > 3) {
          return true;
        }
      }
      return false;
    }
  },
  SET {
    @Override
    public boolean matches(JMethod method) {
      if (!JPrimitiveType.VOID.equals(method.getReturnType())) {
        return false;
      }
      if (method.getParameters().length != 1) {
        return false;
      }
      String name = method.getName();
      if (name.startsWith(SET_PREFIX) && name.length() > 3) {
        return true;
      }
      return false;
    }
  },
  SET_BUILDER {
    @Override
    public boolean matches(JMethod method) {
      JClassType returnClass = method.getReturnType().isClassOrInterface();
      if (returnClass == null
          || !returnClass.isAssignableFrom(method.getEnclosingType())) {
        return false;
      }
      if (method.getParameters().length != 1) {
        return false;
      }
      String name = method.getName();
      if (name.startsWith(SET_PREFIX) && name.length() > 3) {
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
    public boolean matches(JMethod method) {
      return true;
    }
  };

  /**
   * Determine which Action a method maps to.
   */
  public static JBeanMethod which(JMethod method) {
    for (JBeanMethod action : JBeanMethod.values()) {
      if (action.matches(method)) {
        return action;
      }
    }
    throw new RuntimeException("CALL should have matched");
  }

  /**
   * Infer the name of a property from the method.
   */
  public String inferName(JMethod method) {
    if (this == CALL) {
      throw new UnsupportedOperationException(
          "Cannot infer a property name for a CALL-type method");
    }
    return Introspector.decapitalize(method.getName().substring(3));
  }

  /**
   * Returns {@code true} if the BeanLikeMethod matches the method.
   */
  public abstract boolean matches(JMethod method);
}
