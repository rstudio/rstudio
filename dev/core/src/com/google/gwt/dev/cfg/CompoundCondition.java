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
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.base.Strings;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Abstract base class for various kinds of compound deferred binding
 * conditions.
 */
public abstract class CompoundCondition extends Condition {

  protected final Conditions conditions = new Conditions();

  public CompoundCondition(Condition... conditions) {
    for (Condition condition : conditions) {
      this.conditions.add(condition);
    }
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof CompoundCondition) {
      CompoundCondition that = (CompoundCondition) object;
      return Objects.equal(this.conditions, that.conditions);
    }
    return false;
  }

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

  @Override
  public int hashCode() {
    return Objects.hashCode(conditions);
  }

  @Override
  public String toSource() {
    List<String> conditionSources = Lists.newArrayList();

    // Translate all the contained conditions.
    for (Condition condition : getConditions()) {
      String conditionSource = condition.toSource();
      if (Strings.isNullOrEmpty(conditionSource)) {
        continue;
      }
      conditionSources.add("(" + conditionSource + ")");
    }

    // If we still have something then wrap it and return.
    if (!conditionSources.isEmpty()) {
      return "(" + Joiner.on(" " + getBinaryOperator() + " ").join(conditionSources) + ")";
    }
    // When there are no contained conditions, the whole thing is a NOP.
    return "";
  }

  protected abstract String getBinaryOperator();
}
