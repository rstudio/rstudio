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
package com.google.web.bindery.requestfactory.shared;

/**
 * The values returned by {@link EntityProxyChange#getWriteOperation()} to
 * describe the type of change being announced.
 * 
 * <dl>
 * <dt>PERSIST
 * <dd>An {@link EntityProxy} that was created on the client has been persisted
 * on the server
 * 
 * <dt>UPDATE
 * <dd>An {@link EntityProxy} has been encountered by the client for the first
 * time, or its version value has changed
 * 
 * <dt>DELETE
 * <dd>The server has confirmed the success of a client request to delete an
 * {@link EntityProxy}
 * </dl>
 * 
 * @see EntityProxyChange#getWriteOperation()
 */
public enum WriteOperation {
  PERSIST("PERSIST"), UPDATE("UPDATE"), DELETE("DELETE");

  // use an unObfuscatedEnumName field to bypass the implicit name() method,
  // to be safe in the case enum name obfuscation is enabled.
  private final String unObfuscatedEnumName;

  private WriteOperation(String unObfuscatedEnumName) {
    this.unObfuscatedEnumName = unObfuscatedEnumName;
  }

  /**
   * Returns the unobfuscated name of the event associated with this
   * {@link WriteOperation}.
   * 
   * @return one of "PERSIST", "UPDATE", or "DELETE"
   */
  public String getUnObfuscatedEnumName() {
    return this.unObfuscatedEnumName;
  }
}