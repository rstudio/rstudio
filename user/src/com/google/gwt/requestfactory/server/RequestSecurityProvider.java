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
 * Enforces security policy for operations and classes, as well as permitting
 * request obfuscation.
 */
interface RequestSecurityProvider {

  /**
   * Throws exception if argument is not accessible via remote requests.
   * 
   * @param clazz a Class instance
   * @throws SecurityException if the argument is not accessible via remote
   *           requests
   */
  void checkClass(Class<?> clazz) throws SecurityException;

  /**
   * Encode a Class type into a String token.
   *
   * @param type a Class instance
   * @return an encoded String token
   */
  String encodeClassType(Class<?> type);

  /**
   * Optionally decodes a previously encoded operation. Throws exception if
   * argument is not a legal operation.
   *
   * @param operationName the operation name as a String
   * @return a decoded operation name
   * @throws SecurityException if the argument is not a legal operation
   */
  String mapOperation(String operationName) throws SecurityException;
}
