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

import java.util.Iterator;
import java.util.LinkedList;

public class Rules {

  private final LinkedList list = new LinkedList();

  public boolean isEmpty() {
    return list.isEmpty();
  }

  public Iterator iterator() {
    return list.iterator();
  }

  /**
   * Prepends a rule, giving it the highest priority.
   */
  public void prepend(Rule rule) {
    list.addFirst(rule);
  }
}
