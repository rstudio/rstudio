/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.server.rpc;

/**
 * The GWT RPC class throws UnexpectedException when a service method, being
 * invoked by GWT RPC, throws a checked exception that is not in the service
 * method's signature. Such exceptions are "unexpected" and the specific
 * exception will be the cause of the UnexpectedException.
 */
public class UnexpectedException extends RuntimeException {
  /**
   * Construct an UnexpectedException.
   * 
   * @param message the detail message
   * @param cause the cause of this {@link UnexpectedException}
   */
  public UnexpectedException(String message, Throwable cause) {
    super(message, cause);
  }
}
