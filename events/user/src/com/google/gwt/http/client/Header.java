/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.http.client;

/**
 * Class for describing an HTTP header.
 * 
 * <h3>Required Module</h3>
 * Modules that use this class should inherit
 * <code>com.google.gwt.http.HTTP</code>.
 * 
 * {@gwt.include com/google/gwt/examples/http/InheritsExample.gwt.xml}
 */
public abstract class Header {
  /**
   * Returns the name of the HTTP header.
   * 
   * @return name of the HTTP header 
   */
  public abstract String getName();
  
  /**
   * Returns the value of the HTTP header.
   * 
   * @return value of the HTTP header 
   */
  public abstract String getValue();
}
