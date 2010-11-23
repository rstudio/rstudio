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

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An interface for RPC token implementation objects included with each RPC
 * call. RPC tokens can be used to implement XSRF protection for RPC calls.
 */
public interface RpcToken extends Serializable {
  /**
   * {@link RemoteService} interfaces specifying {@link RpcToken} implementation
   * using this annotation will only have serializers for the specific class
   * generated, as opposed to generating serializers for all {@link RpcToken}
   * implementations.    
   */
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface RpcTokenImplementation {
    String value();
  }
}
