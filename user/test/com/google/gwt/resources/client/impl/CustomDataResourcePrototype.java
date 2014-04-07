/*
 * Copyright 2014 Google Inc.
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

import com.google.gwt.resources.client.CustomDataResource;
import com.google.gwt.safehtml.shared.SafeUri;

/**
 * Default implementation of {@link CustomDataResource}.
 */
public class CustomDataResourcePrototype extends DataResourcePrototype implements CustomDataResource {
  public CustomDataResourcePrototype(String name, SafeUri uri) {
    super(name, uri);
  }
}
