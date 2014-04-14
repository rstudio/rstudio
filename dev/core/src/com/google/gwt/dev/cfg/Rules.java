/*
 * Copyright 2007 Google Inc.
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

import java.util.Iterator;
import java.util.LinkedList;

/**
 * A typed collection of {@link Rule} objects.
 */
public class Rules {

  private final LinkedList<Rule> list = new LinkedList<Rule>();

  public void dispose() {
    for (Rule rule : list) {
      rule.dispose();
    }
  }

  public boolean isEmpty() {
    return list.isEmpty();
  }

  public Iterator<Rule> iterator() {
    return list.iterator();
  }

  /**
   * Prepends a rule, giving it the highest priority.
   */
  public void prepend(Rule rule) {
    list.addFirst(rule);
  }
}
