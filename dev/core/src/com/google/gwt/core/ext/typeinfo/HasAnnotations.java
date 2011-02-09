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
package com.google.gwt.core.ext.typeinfo;

import java.lang.annotation.Annotation;

/**
 * Interface implemented by elements that can have annotations. This interface
 * is a departure for GWT in that it used types declared in the
 * java.lang.annotation package instead of types declared as part of this
 * typeinfo package. This reflects a compromise between a pure
 * {@link TypeOracle} model and one that is more useful to developers.
 */
public interface HasAnnotations {
  /**
   * Returns an instance of the specified annotation type if it is present on
   * this element or <code>null</code> if it is not.
   * 
   * @param annotationClass annotation type to search for
   * @return instance of the specified annotation type if it is present on this
   *         element or <code>null</code> if it is not
   */
  <T extends Annotation> T getAnnotation(Class<T> annotationClass);

  /**
   * Returns all the annotations present on this element.
   */
  Annotation[] getAnnotations();

  /**
   * Returns the annotations declared directly on this element; does not include
   * any inherited annotations.
   */
  Annotation[] getDeclaredAnnotations();

  /**
   * Returns <code>true</code> if this item has an annotation of the specified
   * type.
   * 
   * @param annotationClass
   * 
   * @return <code>true</code> if this item has an annotation of the specified
   *         type
   */
  boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);
}
