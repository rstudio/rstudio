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

  /**
   * Returns this type with no type parameters or type variables. See the JLS
   * Third Edition section on <a href="http://java.sun.com/docs/books/jls/third_edition/html/typesValues.html#4.6">
   * Type Erasure</a>.
   */
  JType getErasedType();

  /**
   * Returns the turns the
   * <a href="http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html#14152">
   * field descriptor"</a> for a type as specified by the Java Virtual Machine Specification.
   *
   * Examples:
   *
   * <ul>
   * <li><b>boolean</b> = <code>Z</code></li>
   * <li><b>byte[]</b> = <code>[B</code></li>
   * <li><b>java.lang.String</b> = <code>Ljava/lang/String;</li>
   * </ul>
   */
  String getJNISignature();

  /**
   * For array types, recursively looks for the element type that is not an
   * array. Otherwise, returns this type.
   */
  JType getLeafType();

  String getParameterizedQualifiedSourceName();

  /**
   * A binary type name as specified by the
   * <a href="http://java.sun.com/docs/books/jls/third_edition/html/binaryComp.html">
   * Java Language Spec, ThirdEdition</a>.
   */
  String getQualifiedBinaryName();

  /**
   * Returns a type name as it would be specified in Java source, with the
   * package name included.
   */
  String getQualifiedSourceName();

  /**
   * Returns the name of this class without the package name or enclosing class name.
   */
  String getSimpleSourceName();

  /**
   * Returns this instance as a {@link JAnnotationType} if it is an annotation
   * or <code>null</code> if it is not.
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

  /**
   * Returns the raw type if this is a {@link JRawType}, otherwise returns
   * <code>null</code>.
   */
  JRawType isRawType();

  JTypeParameter isTypeParameter();

  JWildcardType isWildcard();
}
