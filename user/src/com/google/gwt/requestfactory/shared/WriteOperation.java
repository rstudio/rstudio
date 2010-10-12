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
package com.google.gwt.requestfactory.shared;

/**
 * The enum used in {@link EntityProxyChange}.
 * <ul>
 * <li>A PERSIST event is fired after a proxy that was created on the client has
 * been persisted on the server.
 * <li>An UPDATE event is fired whenever a client encounters a proxy for the
 * first time, or encounters a proxy with new fields, or encounters a proxy
 * whose fields have been updated.
 * <li>A DELETE event is fired after a proxy that was deleted on the client is
 * deleted on the server as well.
 * </ul>
 */
public enum WriteOperation {
  PERSIST("PERSIST"), 
  UPDATE("UPDATE"), 
  DELETE("DELETE");
  
  // use an unObfuscatedEnumName field to bypass the implicit name() method, 
  // to be safe in the case enum name obfuscation is enabled.
  private final String unObfuscatedEnumName;
  
  private WriteOperation(String unObfuscatedEnumName) {
    this.unObfuscatedEnumName = unObfuscatedEnumName;
  }
  
  public String getUnObfuscatedEnumName() {
    return this.unObfuscatedEnumName;
  }
}