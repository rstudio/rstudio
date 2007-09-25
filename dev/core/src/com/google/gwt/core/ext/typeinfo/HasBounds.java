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
package com.google.gwt.core.ext.typeinfo;

/**
 * Interface implemented by elements that can have can have have bounds, namely
 * type parameters and wildcards.
 */
public interface HasBounds {

  /**
   * Returns the bounds on this element.
   * 
   * @return the bounds of this element; cannot be null
   */
  JBound getBounds();

  /**
   * Returns the first bound of this element.
   * 
   * @return the first bound of this element; cannot be null
   */
  JClassType getFirstBound();
}
