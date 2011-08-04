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
package com.google.gwt.core.ext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A class for returning the result of a rebind operation.
 */
public class RebindResult {
  private final RebindMode rebindMode;
  private final String resultTypeName;
  private Map<String, Serializable> clientData;

  /**
   * Constructs a result using the provided rebindMode and resultTypeName.
   * 
   * @see RebindMode
   * 
   * @param rebindMode
   * @param resultType
   */
  public RebindResult(RebindMode rebindMode, String resultType) {
    this.rebindMode = rebindMode;
    this.resultTypeName = resultType;
  }

  /**
   * Returns a map containing all client data added to this result.
   * 
   * @return A map containing all client data added to this result. Returns
   *         <code>null</code> if no client data has been added.
   */
  public Map<String, Serializable> getClientDataMap() {
    return clientData;
  }

  /**
   * @return The rebind mode used to construct this result.
   */
  public RebindMode getRebindMode() {
    return rebindMode;
  }

  /**
   * @return The type name used to construct this result.
   */
  public String getResultTypeName() {
    return resultTypeName;
  }

  /**
   * Adds keyed, serializable data to a rebind result. This data will be made
   * available, as part of a {@link CachedGeneratorResult}, to subsequent
   * invocations of the same generator, when called under the same conditions
   * (e.g. for the same rebind rule and requested type name). A generator
   * implementation can use this to remember information needed for subsequent
   * regeneration, such as for making cache reuse decisions.
   * 
   * @see CachedGeneratorResult
   * @see GeneratorContext#getCachedGeneratorResult
   * 
   * @param key
   * @param data
   */
  public void putClientData(String key, Serializable data) {
    if (clientData == null) {
      clientData = new HashMap<String, Serializable>();
    }
    clientData.put(key, data);
  } 
}