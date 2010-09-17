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

import java.util.Set;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Implemented by objects that display values.
 * 
 * @param <V> value type
 */
public abstract class Receiver<V> {
  /**
   * Receives general failure notifications. The default implementation throws a
   * RuntimeException with the provided error message.
   */
  public void onFailure(ServerFailure error) {
    String exceptionType = error.getExceptionType();
    String message = error.getMessage();
    throw new RuntimeException(exceptionType
        + ((exceptionType.length() != 0 && message.length() != 0) ? ": " : "")
        + error.getMessage()
        + ((exceptionType.length() != 0 || message.length() != 0) ? ": " : "")
        + error.getStackTraceString());
  }

  /**
   * Called when a RequestObject has been successfully executed on the server.
   */
  public abstract void onSuccess(V response, Set<SyncResult> syncResults);

  /**
   * Called if an object sent to the server could not be validated. The default
   * implementation calls {@link #onFailure(ServerFailure)} if <code>errors
   * </code> is not empty.
   */
  public void onViolation(Set<Violation> errors) {
    if (!errors.isEmpty()) {
      onFailure(new ServerFailure(
          "The call failed on the server due to a ConstraintViolation",
          "", ""));
    }
  }
}
