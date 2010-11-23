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
package com.google.gwt.user.client.rpc;

/**
 * Exception that will be passed to the
 * {@link RpcTokenExceptionHandler#onRpcTokenException(RpcTokenException)}
 * method when RPC token processing resulted in an error.
 */
public class RpcTokenException extends RuntimeException
    implements IsSerializable {
  
  private static final String DEFAULT_MESSAGE = "Invalid RPC token";
  
  /**
   * Constructs an instance with the default message.
   */
  public RpcTokenException() {
    super(DEFAULT_MESSAGE);
  }
  
  /**
   * Constructs an instance with the specified message.
   */
  public RpcTokenException(String msg) {
    super(DEFAULT_MESSAGE + " (" + msg + ")");
  }
}
