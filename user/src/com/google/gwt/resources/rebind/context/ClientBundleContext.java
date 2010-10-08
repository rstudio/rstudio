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
package com.google.gwt.resources.rebind.context;

import java.util.HashMap;
import java.util.Map;

/**
 * A context for sharing cache state across Client Bundle Generators.
 */
public class ClientBundleContext {
  /**
   * A general purpose String to object class which backs the interfaces defined
   * in {@link ResourceContext}.
   */
  private final Map<String, Object> cachedData = new HashMap<String, Object>();

  public Object getCachedData(String string) {
    return cachedData.get(string);
  }

  public Object putCachedData(String key, Object value) {
    return cachedData.put(key, value);
  }

}
