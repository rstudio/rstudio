/*
 * Copyright 2013 Google Inc.
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
 * Simple request factory which exposes {@link JsonRpcService} {@link RequestContext}s.
 */
@SkipInterfaceValidation // test infrastructure and annotation processor are not ready for JsonRpcService
public interface SimpleJsonRpcRequestFactory extends RequestFactory {

  SimpleContext simple();

  /** Simple {@link JsonRpcService} {@link RequestContext}s. */
  @JsonRpcService
  public interface SimpleContext extends RequestContext {

    FooRequest foo();

    /** Simple request */
    @JsonRpcWireName(value = "simple.foo", version = "v1")
    public interface FooRequest extends Request<Void> {

      /** Simple enum */
      public enum FooEnum { VALUE; }

      FooRequest setEnums(java.util.List<FooEnum> enums);
    }
  }
}

