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
package com.google.web.bindery.requestfactory.shared;

/**
 * A ServiceLocator provides instances of a type specified by a {@link Service}
 * when {@link Request} methods declared in a {@link RequestContext}are mapped
 * onto instance (non-static) methods.
 * <p>
 * ServiceLocator subtypes must be default instantiable (i.e. public static
 * types with a no-arg constructor). Instances of ServiceLocators may be
 * retained and reused by the RequestFactory service layer.
 * 
 * @see Service#locator()
 */
public interface ServiceLocator {
  /**
   * Returns an instance of the service object.
   * 
   * @param clazz the requested type of service object
   * @return an instance of the service object
   */
  Object getInstance(Class<?> clazz);
}
