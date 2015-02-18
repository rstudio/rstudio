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

import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.thirdparty.guava.common.base.Objects;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * A typed collection of {@link Condition} objects.
 */
public class Conditions implements Iterable<Condition>, Serializable {

  private List<Condition> list = Lists.create();

  /**
   * Appends a condition.
   */
  public void add(Condition condition) {
    list = Lists.add(list, condition);
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof Conditions) {
      Conditions that = (Conditions) object;
      return Objects.equal(this.list, that.list);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(list);
  }

  @Override
  public Iterator<Condition> iterator() {
    return list.iterator();
  }
}
