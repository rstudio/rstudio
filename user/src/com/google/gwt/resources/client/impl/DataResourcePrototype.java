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
package com.google.gwt.resources.client.impl;

import com.google.gwt.resources.client.DataResource;
import com.google.gwt.safehtml.shared.SafeUri;

/**
 * Simple implementation of {@link DataResource}.
 */
public class DataResourcePrototype implements DataResource {
  private final String name;
  private final SafeUri uri;

  /**
   * Only called by generated code.
   */
  public DataResourcePrototype(String name, SafeUri uri) {
    this.name = name;
    this.uri = uri;
  }

  public String getName() {
    return name;
  }

  public SafeUri getSafeUri() {
    return uri;
  }

  public String getUrl() {
    return uri.asString();
  }
}
