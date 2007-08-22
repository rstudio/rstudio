/*
 * Copyright 2007 Google Inc.
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
package java.lang.annotation;

import java.lang.reflect.Method;

/**
 * Indicates an attempt to access an element of an annotation that has changed
 * since it was compiled or serialized.
 *
 * @link http://java.sun.com/j2se/1.5.0/docs/api/java/lang/annotation/AnnotationTypeMismatchException.html
 */
public class AnnotationTypeMismatchException extends RuntimeException {

  private Method element;
  private String foundType;
  
  public AnnotationTypeMismatchException(Method element, String foundType) {
    super("Annotation mismatch: found type " + foundType + " on " + element);
    this.element = element;
    this.foundType = foundType;
  }

  public Method element() {
    return element;
  }
  
  public String foundType() {
    return foundType;
  }

}
