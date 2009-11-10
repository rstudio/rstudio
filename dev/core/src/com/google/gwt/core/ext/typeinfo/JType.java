/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.core.ext.typeinfo;

/**
 * Abstract superclass for types.
 */
public abstract class JType {

  /**
   * All types use identity for comparison.
   */
  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  public abstract JType getErasedType();

  public abstract String getJNISignature();

  public JType getLeafType() {
    return this;
  }

  public String getParameterizedQualifiedSourceName() {
    return getQualifiedSourceName();
  }

  /**
   * TODO(scottb): remove if we can resolve param names differently.
   */
  public abstract String getQualifiedBinaryName();

  public abstract String getQualifiedSourceName();

  public abstract String getSimpleSourceName();

  /**
   * All types use identity for comparison.
   */
  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  /**
   * Returns this instance if it is a annotation or <code>null</code> if it is
   * not.
   * 
   * @return this instance if it is a annotation or <code>null</code> if it is
   *         not
   */
  public JAnnotationType isAnnotation() {
    return null;
  }

  public abstract JArrayType isArray();

  public abstract JClassType isClass();

  public JClassType isClassOrInterface() {
    JClassType type = isClass();
    if (type != null) {
      return type;
    }
    return isInterface();
  }

  /**
   * Returns this instance if it is an enumeration or <code>null</code> if it
   * is not.
   * 
   * @return this instance if it is an enumeration or <code>null</code> if it
   *         is not
   */
  public abstract JEnumType isEnum();

  // TODO: Rename this to isGeneric
  public abstract JGenericType isGenericType();

  public abstract JClassType isInterface();

  public abstract JParameterizedType isParameterized();

  public abstract JPrimitiveType isPrimitive();

  // TODO: Rename this to isRaw
  public abstract JRawType isRawType();

  public JTypeParameter isTypeParameter() {
    return null;
  }

  public abstract JWildcardType isWildcard();

  /**
   * Returns either the substitution of this type based on the parameterized
   * type or this instance.
   * 
   * @param parameterizedType
   * @return either the substitution of this type based on the parameterized
   *         type or this instance
   */
  abstract JType getSubstitutedType(JParameterizedType parameterizedType);
}
