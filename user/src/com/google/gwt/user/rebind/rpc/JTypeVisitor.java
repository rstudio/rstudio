/*
 * Copyright 2008 Google Inc.
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

package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.typeinfo.JAnnotationType;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JRawType;
import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.JWildcardType;

/**
 * A visitor that traverses the structure of a {@link JType}.
 */
class JTypeVisitor {
  /**
   * Accept <code>type</code>, traverse its children with
   * {@link #acceptChildren(JType)}, and then call {@link #endVisit(JType)} on
   * <code>type</code> itself.
   */
  public void accept(JType type) {
    acceptChildren(type);
    endVisit(type);
  }

  /**
   * Call {@link #accept(JType)} on all children types of <code>type</code>. The
   * children type of a type are its structural components. For example, an
   * array type has one child, which is the component type of the array.
   */
  protected void acceptChildren(JType type) {
    JArrayType typeArray = type.isArray();
    if (typeArray != null) {
      accept(typeArray.getComponentType());
      endVisit(typeArray);
    }

    JParameterizedType typeParameterized = type.isParameterized();
    if (typeParameterized != null) {
      accept(typeParameterized.getBaseType());
      for (JClassType typeArg : typeParameterized.getTypeArgs()) {
        accept(typeArg);
      }
      endVisit(typeParameterized);
    }

    JRawType typeRaw = type.isRawType();
    if (typeRaw != null) {
      accept(typeRaw.getBaseType());
      endVisit(typeRaw);
    }

    JWildcardType typeWild = type.isWildcard();
    if (typeWild != null) {
      accept(typeWild.getFirstBound());
      endVisit(typeWild);
    }
  }

  /**
   * @param typeAnnotation
   */
  protected void endVisit(JAnnotationType typeAnnotation) {
  }

  /**
   * @param typeArray
   */
  protected void endVisit(JArrayType typeArray) {
  }

  /**
   * @param typeEnum
   */
  protected void endVisit(JEnumType typeEnum) {
  }

  /**
   * @param typeGeneric
   */
  protected void endVisit(JGenericType typeGeneric) {
  }

  /**
   * @param typeParameterized
   */
  protected void endVisit(JParameterizedType typeParameterized) {
  }

  /**
   * @param typePrimitive
   */
  protected void endVisit(JPrimitiveType typePrimitive) {
  }

  /**
   * @param typeRaw
   */
  protected void endVisit(JRawType typeRaw) {
  }

  /**
   * @param typeReal
   */
  protected void endVisit(JRealClassType typeReal) {
  }

  /**
   * This endVisit is called on all visited types. By default it dispatches to
   * one of the more specific endVisit methods, e.g.
   * {@link #endVisit(JArrayType)}.
   */
  protected void endVisit(JType type) {
    JAnnotationType typeAnnotation = type.isAnnotation();
    if (typeAnnotation != null) {
      endVisit(typeAnnotation);
      return;
    }

    JArrayType typeArray = type.isArray();
    if (typeArray != null) {
      endVisit(typeArray);
      return;
    }

    JEnumType typeEnum = type.isEnum();
    if (typeEnum != null) {
      endVisit(typeEnum);
      return;
    }

    JGenericType typeGeneric = type.isGenericType();
    if (typeGeneric != null) {
      endVisit(typeGeneric);
      return;
    }

    JParameterizedType typeParameterized = type.isParameterized();
    if (typeParameterized != null) {
      endVisit(typeParameterized);
      return;
    }

    JPrimitiveType typePrimitive = type.isPrimitive();
    if (typePrimitive != null) {
      endVisit(typePrimitive);
      return;
    }

    JRawType typeRaw = type.isRawType();
    if (typeRaw != null) {
      endVisit(typeRaw);
      return;
    }

    JTypeParameter typeParam = type.isTypeParameter();
    if (typeParam != null) {
      endVisit(typeParam);
      return;
    }

    JWildcardType typeWild = type.isWildcard();
    if (typeWild != null) {
      endVisit(typeWild);
      return;
    }

    JRealClassType typeReal = (JRealClassType) type;
    endVisit(typeReal);
  }

  /**
   * @param typeParam
   */
  protected void endVisit(JTypeParameter typeParam) {
  }

  /**
   * @param typeWild
   */
  protected void endVisit(JWildcardType typeWild) {
  }

}
