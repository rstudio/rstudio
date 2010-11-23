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

import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.dev.cfg.BindingProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The standard implementation of {@link SelectionProperty} from a
 * {@link BindingProperty}.
 */
public class StandardSelectionProperty implements SelectionProperty {
  private static final String FALLBACK_TOKEN = "/*-FALLBACK-*/";

  private final String activeValue;
  private final boolean isDerived;
  private final String name;
  private final String provider;
  private final SortedSet<String> values;

  public StandardSelectionProperty(BindingProperty p) {
    activeValue = p.getConstrainedValue();
    isDerived = p.isDerived();
    name = p.getName();
    String fallback = p.getFallback();
    provider = p.getProvider() == null ? null
        : p.getProvider().getBody().replace(FALLBACK_TOKEN, fallback);
    values = Collections.unmodifiableSortedSet(new TreeSet<String>(
        Arrays.asList(p.getDefinedValues())));
  }

  public String getName() {
    return name;
  }

  public SortedSet<String> getPossibleValues() {
    return values;
  }

  public String getPropertyProvider() {
    return provider;
  }

  public boolean isDerived() {
    return isDerived;
  }

  @Override
  public String toString() {
    StringBuffer b = new StringBuffer();
    b.append(getName()).append(" : [");
    for (String value : getPossibleValues()) {
      b.append(" ").append(value);
    }
    b.append(" ]");
    return b.toString();
  }

  public String tryGetValue() {
    return activeValue;
  }
}
