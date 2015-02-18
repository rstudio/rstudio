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
package com.google.gwt.dev.cfg;

import com.google.gwt.thirdparty.guava.common.base.Objects;

import java.io.Serializable;

/**
 * Produces a deferred binding property value by executing JavaScript code.
 */
public class PropertyProvider implements Serializable {

  private final String body;

  public PropertyProvider(String body) {
    this.body = body;
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof PropertyProvider) {
      PropertyProvider that = (PropertyProvider) object;
      return Objects.equal(this.body, that.body);
    }
    return false;
  }

  public String getBody() {
    return body;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(body);
  }
}
