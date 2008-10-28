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
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.typeinfo.JClassType;

import java.lang.annotation.Annotation;

/**
 * Utility class for i18n-related annotation manipulation routines.
 */
public class AnnotationUtil {

  /**
   * Find an instance of the specified annotation, walking up the inheritance
   * tree if necessary.  
   * 
   * <p>Note that i18n annotations may appear on classes as well as interfaces
   * (a concrete implementation can be supplied rather than just an interface
   * and this is the normal way of using generic Localizable interfaces), so
   * we have to search the super chain as well as other interfaces.
   * 
   * <p>The super chain is walked first, so if an ancestor superclass has the
   * requested annotation, it will be preferred over a directly implemented
   * interface.
   * 
   * @param <T> Annotation type to search for
   * @param clazz root class to search, may be null
   * @param annotationClass class object of Annotation subclass to search for
   * @return the requested annotation or null if none
   */
  static <T extends Annotation> T getClassAnnotation(JClassType clazz,
      Class<T> annotationClass) {
    if (clazz == null) {
      return null;
    }
    T annot = clazz.getAnnotation(annotationClass);
    if (annot == null) {
      annot = getClassAnnotation(clazz.getSuperclass(), annotationClass);
      if (annot != null) {
        return annot;
      }
      for (JClassType intf : clazz.getImplementedInterfaces()) {
        annot = getClassAnnotation(intf, annotationClass);
        if (annot != null) {
          return annot;
        }
      }
    }
    return annot;
  }

}
