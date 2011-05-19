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
package com.google.gwt.core.ext.soyc.impl;

import com.google.gwt.core.ext.soyc.HasDependencies;
import com.google.gwt.core.ext.soyc.Member;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Provides a common implementation of HasDependencies.
 */
public abstract class AbstractMemberWithDependencies extends AbstractMember implements
    HasDependencies {
  private final SortedSet<Member> dependencies = new TreeSet<Member>(
      Member.TYPE_AND_SOURCE_NAME_COMPARATOR);
  private final SortedSet<Member> dependenciesView = Collections
      .unmodifiableSortedSet(dependencies);

  /**
   * Add a dependency.
   * 
   * @return <code>true</code> if the dependency was not previously added.
   */
  public boolean addDependency(Member member) {
    return dependencies.add(member);
  }

  public SortedSet<Member> getDependencies() {
    return dependenciesView;
  }
}
