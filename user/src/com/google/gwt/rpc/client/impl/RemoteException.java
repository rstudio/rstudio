/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.rpc.client.impl;

/**
 * A wrapper exception type indicating that the remote end threw an exception
 * over the wire.
 */
public class RemoteException extends RuntimeException {
  public RemoteException() {
  }

  public RemoteException(String msg, Throwable cause) {
    super(msg, cause);
  }

  public RemoteException(Throwable cause) {
    super(cause);
  }
}
