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
package com.google.gwt.core.client;

/**
 * Any JavaScript exceptions occurring within JSNI methods are wrapped as this
 * class when caught in Java code. The wrapping does not occur until the
 * exception passes out of JSNI into Java. Before that, the thrown object
 * remains a native JavaScript exception object, and can be caught in JSNI as
 * normal.
 */
public final class JavaScriptException extends RuntimeException {

  /**
   * The original type name of the JavaScript exception this class wraps,
   * initialized as <code>e.name</code>.
   */
  private final String name;

  /**
   * The original description of the JavaScript exception this class wraps,
   * initialized as <code>e.message</code>.
   */
  private final String description;

  /**
   * @param name the original JavaScript type name of the exception
   * @param description the original JavaScript message of the exception
   */
  public JavaScriptException(String name, String description) {
    super("JavaScript " + name + " exception: " + description);
    this.name = name;
    this.description = description;
  }

  /**
   * Useful for server-side instantiation.
   * 
   * @param message the detail message.
   */
  protected JavaScriptException(String message) {
    super(message);
    this.name = null;
    this.description = message;
  }

  /**
   * @return the original JavaScript message of the exception
   */
  public String getDescription() {
    return description;
  }

  /**
   * @return the original JavaScript type name of the exception
   */
  public String getName() {
    return name;
  }

}
