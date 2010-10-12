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
package com.google.gwt.requestfactory.shared;

/**
 * Implemented by the request objects created by this factory.
 * 
 * @param <T> The return type of objects in the corresponding response.
 */
public interface Request<T> {

  /**
   * Submit this request. Failures will be reported through the global uncaught
   * exception handler, if any.
   */
  void fire();

  /**
   * Convenience method equivalent to calling <code>to(...).fire()</code>.
   */
  void fire(Receiver<? super T> receiver);

  /**
   * Specify the object that will receive the result of the method invocation.
   */
  RequestContext to(Receiver<? super T> receiver);

  /**
   * Request additional reference properties to fetch with the return value.
   */
  Request<T> with(String... propertyRefs);
}
