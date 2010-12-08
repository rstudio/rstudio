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
 * Callback object for {@link Request#fire(Receiver)} and
 * {@link RequestContext#fire(Receiver)}.
 * 
 * @param <V> value type
 */
public abstract class Receiver<V> {
  /**
   * Receives general failure notifications. The default implementation looks at
   * {@link ServerFailure#isFatal()}, and throws a runtime exception with the
   * failure object's error message if it is true.
   * 
   * @param error a {@link ServerFailure} instance
   */
  public void onFailure(ServerFailure error) {
    if (error.isFatal()) {
      throw new RuntimeException(error.getMessage());
    }
  }

  /**
   * Called when a Request has been successfully executed on the server.
   * 
   * @param response a response of type V
   */
  public abstract void onSuccess(V response);

  /**
   * Called if an object sent to the server could not be validated. The default
   * implementation calls {@link #onFailure(ServerFailure)} if <code>errors
   * </code> is not empty.
   * 
   * @param errors a Set of {@link Violation} instances
   */
  public void onViolation(Set<Violation> errors) {
    if (!errors.isEmpty()) {
      onFailure(new ServerFailure(
          "The call failed on the server due to a ConstraintViolation"));
    }
  }
}
