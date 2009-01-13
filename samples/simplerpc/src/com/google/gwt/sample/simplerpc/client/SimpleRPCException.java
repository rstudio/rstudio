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
package com.google.gwt.sample.simplerpc.client;

/**
 * Simple RPC exception.
 */
public class SimpleRPCException extends Exception {

  /**
   * Constructor for <code>SimpleRPCException</code>. Needed to support
   * serialization.
   */
  public SimpleRPCException() {
  }

  /**
   * Constructor for <code>SimpleRPCException</code>.
   * 
   * @param message message for the <code>SimplePRCException</code>
   */
  public SimpleRPCException(String message) {
    super(message);
  }
}
