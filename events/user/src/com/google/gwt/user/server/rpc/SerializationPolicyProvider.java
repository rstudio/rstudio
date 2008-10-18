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
package com.google.gwt.user.server.rpc;

/**
 * Used to obtain a {@link SerializationPolicy} for a given module base URL and
 * serialization policy strong name.
 */
public interface SerializationPolicyProvider {
  /**
   * Returns a {@link SerializationPolicy} for a given module base URL and
   * serialization policy strong name.
   * 
   * @param moduleBaseURL the URL for the module
   * @param serializationPolicyStrongName strong name of the serialization
   *          policy for the specified module URL
   * @return a {@link SerializationPolicy} for a given module base URL and RPC
   *         strong name; must not return <code>null</code>
   */
  SerializationPolicy getSerializationPolicy(String moduleBaseURL,
      String serializationPolicyStrongName);
}
