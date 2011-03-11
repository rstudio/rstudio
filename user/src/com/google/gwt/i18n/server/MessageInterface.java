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
package com.google.gwt.i18n.server;

import com.google.gwt.i18n.shared.GwtLocale;

import java.lang.annotation.Annotation;

/**
 * Describes a single interface with {@link Message}s.
 */
public interface MessageInterface {

  /**
   * Visit this class and all messages within it, using the default locale.
   * 
   * @param cv
   * @throws MessageProcessingException 
   */
  void accept(MessageInterfaceVisitor cv) throws MessageProcessingException;

  /**
   * Visit this class and all messages within it.
   * 
   * @param cv
   * @param locale
   * @throws MessageProcessingException 
   */  
  void accept(MessageInterfaceVisitor cv, GwtLocale locale)
    throws MessageProcessingException;

  /**
   * Return the requested annotation present on this message, including parents
   * if the annotation is inherited.
   * 
   * @param annotClass
   * @return an annotation instance or null if not found
   */
  <A extends Annotation> A getAnnotation(Class<A> annotClass);

  /**
   * Get the unqualified class name (including parent classes for nested
   * classes - ie, "Foo.Bar") of this {@link MessageInterface}.
   * 
   * @return unqualified class name
   */
  String getClassName();

  /**
   * Get the package name (ie, "org.example") of this {@link MessageInterface}.
   * 
   * @return package name
   */
  String getPackageName();

  /**
   * Get the fully qualified source name (ie, "org.example.Foo.Bar": if Bar is
   * an inner class of Foo) of this message interface - generally used for
   * error messages.
   *  
   * @return fully qualified source name
   */
  String getQualifiedName();

  /**
   * Check if a specified annotation is present on this message (including
   * via inheritance if the annotation is inherited).
   * 
   * @param annotClass
   * @return true if the annotation is present
   */
  boolean isAnnotationPresent(Class<? extends Annotation> annotClass);
}