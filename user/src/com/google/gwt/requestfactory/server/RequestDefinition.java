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

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Implemented by enums that define the mapping between request objects and
 * service methods.
 */
public interface RequestDefinition {
  /**
   * Returns the name of the (domain) class that contains the method to be
   * invoked on the server.
   *
   * @return the domain class name as a String
   */
  String getDomainClassName();

  /**
   * Returns the method to be invoked on the server.
   *
   * @return the domain Method
   */
  Method getDomainMethod();

  /**
   * Returns the parameter types of the method to be invoked on the server.
   *
   * @return an array of Class objects for each parameter type
   */
  Class<?>[] getParameterTypes();

  /**
   * Returns the request parameter types.
   *
   * @return an array of Type objects for each request parameter
   */
  Type[] getRequestParameterTypes();

  /**
   * Returns the return type of the method to be invoked on the server.
   * 
   * @return a Class object representing the return type
   */
  Class<?> getReturnType();

  /**
   * Returns whether the domain method is an instance method.
   *
   * @return {@code true} if the domain method is an instance method
   */
  boolean isInstance();

  /**
   * Returns the name.
   * 
   * @return the name as a String
   */
  String name();
}