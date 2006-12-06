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
package com.google.gwt.user.rebind.rpc;

/**
 * This exception is thrown whenever we determine that a RemoteService interface
 * declaration has been encountered.  
 */
public class InvalidRemoteServiceInterfaceException extends Exception {
  private String[] errors;
  
  public InvalidRemoteServiceInterfaceException(String message, String[] errors) {
    super(message);
    this.errors = errors;
  }
  
  public String[] getErrorMessages() {
    return errors;
  }
}
