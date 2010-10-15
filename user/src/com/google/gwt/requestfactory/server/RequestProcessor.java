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
package com.google.gwt.requestfactory.server;

/**
 * Simple interface to abstract the underlying RequestFactory RPC mechanism. A
 * serialized object of type T is provided, such as a String in the case of JSON
 * requests, and a serialized return value of the same type is returned.
 * @param <T> the type of encoding used to serialize the request (e.g. String)
 */
public interface RequestProcessor<T> {
  /**
   * Decodes request, invokes methods, and re-encoded resulting return values.
   *
   * @param encodedRequest an encoded request of type T
   * @return a decoded instance of type T
   * @throws RequestProcessingException if an error occurs
   */
  T decodeAndInvokeRequest(T encodedRequest) throws RequestProcessingException;

  /**
   * Sets the ExceptionHandler to use to convert exceptions caused by
   * method invocations into failure messages sent back to the client.
   * 
   * @param exceptionHandler an implementation, such as
   *        {@code DefaultExceptionHandler}
   */
  void setExceptionHandler(ExceptionHandler exceptionHandler);

  /**
   * Sets the OperationRegistry to be used for looking up invocation metadata.
   * 
   * @param registry an implementation, such as
   *          {@link ReflectionBasedOperationRegistry}
   */
  void setOperationRegistry(OperationRegistry registry);
}
