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
package com.google.gwt.user.client.rpc;

/**
 * Superclass for exceptions thrown from RPC methods (those appearing in
 * interfaces derived from {@link RemoteService}).
 * 
 * @deprecated As of GWT 1.5, {@link Exception} implements
 *             {@link java.io.Serializable Serializable} and can be used in place
 *             of this class
 */
@Deprecated
public class SerializableException extends Exception implements IsSerializable {

  /**
   * The default constructor. This constructor is used implicitly during
   * serialization or when constructing subclasses.
   */
  public SerializableException() {
  }

  /**
   * Constructs a serializable exception with the specified message. This
   * constructor is most often called by subclass constructors.
   */
  public SerializableException(String msg) {
    super(msg);
  }

  /**
   * Exception chaining is not currently supported for serialized exceptions.
   * 
   * @return always <code>null</code>
   */
  @Override
  public Throwable getCause() {
    return null;
  }

  /**
   * No effect; exception chaining is not currently supported for serialized
   * exceptions.
   */
  @Override
  public Throwable initCause(Throwable cause) {
    // nothing
    return null;
  }
}
