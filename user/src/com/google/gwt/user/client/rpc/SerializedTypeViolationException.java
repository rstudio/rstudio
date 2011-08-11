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

package com.google.gwt.user.client.rpc;

/**
 * Exception that will be passed to the
 * {@link AsyncCallback#onFailure(Throwable)} method when the value of an
 * argument to a method in an RPC message is of the incorrect type.
 * 
 * <p>
 * For example, a method may be expecting an Integer argument, while the value
 * in the message is a HashMap. The most likely source of this message in a
 * production system is a security attack where a man-in-the-middle has modified
 * an RPC message by changing the value types within the message.
 * </p>
 * 
 * <p>
 * Note that on the client, the {@link #getCause()} always return
 * <code>null</code>.
 * </p>
 */
public class SerializedTypeViolationException extends SerializationException
    implements IsSerializable {

  /**
   * Constructor used by RPC serialization. Note that the client side code will
   * always get a generic error message.
   */
  public SerializedTypeViolationException() {
    super();
  }

  /**
   * Constructs an instance with the specified message.
   */
  public SerializedTypeViolationException(String msg) {
    super(msg);
  }

  /**
   * Constructs an instance with the specified message and cause.
   */
  public SerializedTypeViolationException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
