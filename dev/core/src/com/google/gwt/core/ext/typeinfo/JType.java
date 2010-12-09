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
package com.google.gwt.core.ext.typeinfo;

/**
 * Super interface for types.
 */
public interface JType {

  JType getErasedType();

  String getJNISignature();

  JType getLeafType();

  String getParameterizedQualifiedSourceName();

  /**
   * A binary type name as specified by the 
   * <a href="http://java.sun.com/docs/books/jls/second_edition/html/binaryComp.doc.html">
   * Java Language Spec, Edition 2</a>.
   */
  String getQualifiedBinaryName();

  /**
   * A type name as it would be specified in Java source.
   */
  String getQualifiedSourceName();

  String getSimpleSourceName();

  /**
   * Returns this instance if it is a annotation or <code>null</code> if it is
   * not.
   */
  JAnnotationType isAnnotation();

  JArrayType isArray();

  JClassType isClass();

  JClassType isClassOrInterface();

  /**
   * Returns this instance if it is an enumeration or <code>null</code> if it is
   * not.
   */
  JEnumType isEnum();

  JGenericType isGenericType();

  JClassType isInterface();

  JParameterizedType isParameterized();

  JPrimitiveType isPrimitive();

  JRawType isRawType();

  JTypeParameter isTypeParameter();

  JWildcardType isWildcard();
}
