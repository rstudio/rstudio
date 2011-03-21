/*
 * Copyright 2011 Google Inc.
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
 * An object capable of providing an instance of type T asynchronously
 * via {@link Callback}. For example, the instance might be
 * created within a GWT.runAsync block using the following template:
 *
 *  <pre style=code>
 *      public void get(final Callback<T, Throwable> callback) {
 *        GWT.runAsync(new RunAsyncCallback() {
 *          public void onSuccess() {
 *            callback.onSuccess(javax.inject.Provider<T>.get());
 *          }
 *          public void onFailure(Throwable ex) {
 *            callback.onFailure(ex);
 *          }
 *        }
 *      }
 *  </pre>
 *
 * @param <T> the type of the provided value
 * @param <F> the type returned on failure
 */
public interface AsyncProvider<T, F> {

  /**
   * @param callback Callback used to pass the instance of T or an exception
   *    if there is an issue creating that instance.
   */
  void get(Callback<? super T, ? super F> callback);
}
