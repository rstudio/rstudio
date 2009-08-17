/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.dev.util.collect.Sets;

import java.util.Iterator;
import java.util.Set;

/**
 * Abstract base class for various kinds of compound deferred binding
 * conditions.
 */
public abstract class CompoundCondition extends Condition {

  private final Conditions conditions = new Conditions();

  public Conditions getConditions() {
    return conditions;
  }

  @Override
  public Set<String> getRequiredProperties() {
    Set<String> toReturn = Sets.create();
    for (Iterator<Condition> it = conditions.iterator(); it.hasNext();) {
      toReturn = Sets.addAll(toReturn, it.next().getRequiredProperties());
    }
    return toReturn;
  }
}
