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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.linker.ConfigurationProperty;

import java.util.List;

/**
 * The standard implementation of {@link ConfigurationProperty} from a
 * {@link com.google.gwt.dev.cfg.ConfigurationProperty}.
 */
public class StandardConfigurationProperty implements ConfigurationProperty {

  private final String name;
  private final List<String> values;

  public StandardConfigurationProperty(
      com.google.gwt.dev.cfg.ConfigurationProperty p) {
    name = p.getName();
    values = p.getValues();

    if (values == null) {
      throw new IllegalArgumentException("values is null");
    }
    if (!p.allowsMultipleValues() && values.size() != 1) {
      throw new IllegalArgumentException(p.getName()
          + " property requires exactly one value but values = " + values);
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  @Deprecated
  public String getValue() {
    return values.isEmpty() ? null : values.get(0);
  }

  @Override
  public List<String> getValues() {
    return values;
  }

  @Override
  public boolean hasMultipleValues() {
    return values.size() > 1;
  }
}
