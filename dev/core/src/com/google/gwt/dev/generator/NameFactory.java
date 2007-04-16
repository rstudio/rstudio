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
package com.google.gwt.dev.generator;

import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

/**
 * Generates unique identifiers. Use this class to avoid generating
 * conflicting names with user code. This class isn't smart enough to know
 * about scopes (which isn't generally a problem for generators in any case).
 */
public class NameFactory {

  private Set usedNames = new HashSet();

  /**
   * Creates a new NameFactory that knows about <code> existingNames</code>.
   *
   * @param existingNames a Collection of strings, may be null
   */
  public NameFactory(Collection existingNames) {
    this.usedNames = new HashSet();
    if (existingNames == null) {
      return;
    }
    usedNames.addAll(existingNames);
  }

  /**
   * Creates a new NameFactory that doesn't know about any existing names.
   */
  public NameFactory() {
    this(null);
  }

  /**
   * Adds a name to the set of already known identifiers.  This implementation
   * asserts that the identifier is unique.
   *
   * @param name a non-null, unique name
   */
  public void addName(String name) {
    assert (!usedNames.contains(name));
    usedNames.add(name);
  }

  /**
   * Creates a new unique name based off of <code>name</code> and adds it to
   * the list of known names.
   *
   * @param name a non-null name to base the new unique name from
   * @return a new unique, non-null name. This name may be possibly identical
   *         to <code>name</code>
   */
  public String createName(String name) {
    assert (name != null);
    
    String newName = name;

    for (int count = 0; true; ++count) {
      if (usedNames.contains(newName)) {
        newName = name + count;
      } else {
        return newName;
      }
    }
  }
}
