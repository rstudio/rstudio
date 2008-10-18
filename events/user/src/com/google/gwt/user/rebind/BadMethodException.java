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
package com.google.gwt.user.rebind;

/**
 * Used by <code>Creators</code> to indicate that the method that is being
 * generated cannot be instantiated.
 */
public class BadMethodException extends Exception {
  /**
   * <code>BadMethodException</code> constructor.
   * 
   * @param msg message to read.
   */
  public BadMethodException(String msg) {
    super(msg);
  }

  /**
   * <code>BadMethodException</code> constructor.
   * 
   * @param msg message to read
   * @param e cause of exception
   */
  public BadMethodException(String msg, Throwable e) {
    super(msg, e);
  }
}
