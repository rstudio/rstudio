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
package com.google.gwt.resources.client;

/**
 * A callback interface for asynchronous operations on resources.
 * 
 * @param <R> the type of resource
 */
public interface ResourceCallback<R extends ResourcePrototype> {
  /**
   * Invoked if the asynchronous operation failed.
   * @param e an exception describing the failure
   */
  void onError(ResourceException e);

  /**
   * Invoked if the asynchronous operation was successfully completed.
   * @param resource the resource on which the operation was performed
   */
  void onSuccess(R resource);
}
