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
package com.google.gwt.user.client.rpc;

/**
 * The primary interface a caller must implement to receive a response from a
 * remote procedure call.
 * 
 * <p>
 * If an RPC is successful, then {@link #onSuccess(Object)} is called, otherwise
 * {@link #onFailure(Throwable)} is called.
 * </p>
 * 
 * <p>
 * Each callable asynchronous method corresponds to a method in the correlated
 * service interface. The asynchronous method always takes an
 * <code>AsyncCallback&lt;T&gt;</code> as its last parameter, where
 * <code>T</code> is the return type of the correlated synchronous method.
 * </p>
 * 
 * <p>
 * As an example, suppose the service interface defines a method called
 * <code>getShapes</code> as follows:
 * 
 * <pre>
 * Shape[] getShapes(String databaseName) throws ShapeException, DbException;
 * </pre>
 * 
 * Its asynchronous counterpart method be declared as:
 * 
 * <pre>
 * void getShapes(String databaseName, AsyncCallback&lt;Shape[]&gt; callback);
 * </pre>
 * 
 * Note that <code>throws</code> declaration is not repeated in the async
 * version.
 * </p>
 * 
 * <p>
 * A call with a typical use of <code>AsyncCallback</code> might look like
 * this:
 * 
 * <pre class="code">
 * service.getShapes(dbName, new AsyncCallback<Shape[]>() {
 *   public void onSuccess(Shape[] result) {
 *     // It's always safe to downcast to the known return type. 
 *     controller.processShapes(result);
 *   }
 * 
 *   public void onFailure(Throwable caught) {
 *     // Convenient way to find out which exception was thrown.
 *     try {
 *       throw caught;
 *     } catch (IncompatibleRemoteServiceException e) {
 *       // this client is not compatible with the server; cleanup and refresh the 
 *       // browser
 *     } catch (InvocationException e) {
 *       // the call didn't complete cleanly
 *     } catch (ShapeException e) {
 *       // one of the 'throws' from the original method
 *     } catch (DbException e) {
 *       // one of the 'throws' from the original method
 *     } catch (Throwable e) {
 *       // last resort -- a very unexpected exception
 *     }
 *   }
 * });
 * </pre>
 * 
 * </p>
 * 
 * @param <T> The type of the return value that was declared in the synchronous
 *          version of the method. If the return type is a primitive, use the
 *          boxed version of that primitive (for example, an <code>int</code>
 *          return type becomes an {@link Integer} type argument, and a
 *          <code>void</code> return type becomes a {@link Void} type
 *          argument, which is always <code>null</code>).
 */
public interface AsyncCallback<T> {

  /**
   * Called when an asynchronous call fails to complete normally.
   * {@link IncompatibleRemoteServiceException}s, {@link InvocationException}s,
   * or checked exceptions thrown by the service method are examples of the type
   * of failures that can be passed to this method.
   * 
   * <p>
   * If <code>caught</code> is an instance of an
   * {@link IncompatibleRemoteServiceException} the application should try to
   * get into a state where a browser refresh can be safely done.
   * </p>
   * 
   * @param caught failure encountered while executing a remote procedure call
   */
  void onFailure(Throwable caught);

  /**
   * Called when an asynchronous call completes successfully.
   * 
   * @param result the return value of the remote produced call
   */
  void onSuccess(T result);
}
