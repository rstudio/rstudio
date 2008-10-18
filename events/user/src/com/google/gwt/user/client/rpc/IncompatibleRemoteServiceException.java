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
package com.google.gwt.user.client.rpc;

/**
 * Exception that will be passed to the
 * {@link AsyncCallback#onFailure(Throwable)} method when an incompatibility is
 * detected between a {@link RemoteService} client and its corresponding
 * {@link RemoteService} server.
 * 
 * <p>
 * The correct response to receiving an instance of this exception in the
 * {@link AsyncCallback#onFailure(Throwable)} method is to get the application
 * into a state where a browser refresh can be done.
 * </p>
 * 
 * <p>
 * This exception can be caused by the following problems:
 * <ul>
 * <li>The requested {@link RemoteService} cannot be located via
 * {@link Class#forName(String)} on the server.</li>
 * <li>The requested {@link RemoteService} interface is not implemented by the
 * {@link com.google.gwt.user.server.rpc.RemoteServiceServlet RemoteServiceServlet}
 * instance which is configured to process the request.</li>
 * <li>The requested service method is not defined or inherited by the
 * requested {@link RemoteService} interface.</li>
 * <li>One of the types used in the {@link RemoteService} method invocation has
 * had fields added or removed.</li>
 * <li>The client code receives a type from the server which it cannot
 * deserialize.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Note that on the client, the {@link #getCause()} always return
 * <code>null</code>.
 * </p>
 */
public final class IncompatibleRemoteServiceException extends RuntimeException
    implements IsSerializable {

  private static final String DEFAULT_MESSAGE = "This application is out of "
      + "date, please click the refresh button on your browser.";

  /**
   * Constructor used by RPC serialization. Note that the client side code will
   * always get a generic error message.
   */
  public IncompatibleRemoteServiceException() {
    super(DEFAULT_MESSAGE);
  }

  /**
   * Constructs an instance with the specified message.
   */
  public IncompatibleRemoteServiceException(String msg) {
    super(DEFAULT_MESSAGE + " ( " + msg + " )");
  }

  /**
   * Constructs an instance with the specified message and cause.
   */
  public IncompatibleRemoteServiceException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
