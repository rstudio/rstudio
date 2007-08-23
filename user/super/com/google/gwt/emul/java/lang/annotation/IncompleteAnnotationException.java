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

/**
 * Indicates an attempt to access an element of an annotation that was added
 * since it was compiled or serialized <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/annotation/IncompleteAnnotationException.html">[Sun
 * docs]</a>.
 */
public class IncompleteAnnotationException extends RuntimeException {

  public final Class<? extends Annotation> annotationType;
  public final String elementName;

  public IncompleteAnnotationException(
      Class<? extends Annotation> annotationType, String elementName) {
    super("Incomplete annotation: trying to access " + elementName + " on "
        + annotationType);
    this.annotationType = annotationType;
    this.elementName = elementName;
  }

  public Class<? extends Annotation> annotationType() {
    return annotationType;
  }

  public String elementName() {
    return elementName;
  }

}
