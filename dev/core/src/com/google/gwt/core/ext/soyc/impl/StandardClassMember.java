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

import com.google.gwt.core.ext.soyc.ClassMember;
import com.google.gwt.core.ext.soyc.FieldMember;
import com.google.gwt.core.ext.soyc.Member;
import com.google.gwt.core.ext.soyc.MethodMember;
import com.google.gwt.dev.jjs.ast.JDeclaredType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An implementation of ClassMember. This implementation always returns
 * unmodifiable collections since it is exposed directly to user code via the
 * Linker API.
 */
public class StandardClassMember extends AbstractMemberWithDependencies implements ClassMember {
  private final MemberFactory factory;
  private final SortedSet<FieldMember> fields = new TreeSet<FieldMember>(
      Member.SOURCE_NAME_COMPARATOR);
  private final SortedSet<FieldMember> fieldsView = Collections.unmodifiableSortedSet(fields);
  private final SortedSet<MethodMember> methods = new TreeSet<MethodMember>(
      Member.SOURCE_NAME_COMPARATOR);
  private final SortedSet<MethodMember> methodsView = Collections.unmodifiableSortedSet(methods);
  private SortedSet<ClassMember> overridesView; // Initialized lazily
  private final String packageName;
  private final String sourceName;
  private final JDeclaredType type;

  /**
   * Constructed by {@link MemberFactory#get(JReferenceType)}.
   */
  public StandardClassMember(MemberFactory factory, JDeclaredType type) {
    this.factory = factory;
    this.type = type;

    int index = type.getName().lastIndexOf('.');
    if (index < 0) {
      packageName = "";
    } else {
      packageName = type.getName().substring(0, index).intern();
    }
    sourceName = type.getName().intern();
  }

  public void addField(FieldMember field) {
    fields.add(field);
  }

  public void addMethod(MethodMember method) {
    methods.add(method);
  }

  public SortedSet<FieldMember> getFields() {
    return fieldsView;
  }

  public SortedSet<MethodMember> getMethods() {
    return methodsView;
  }

  public SortedSet<ClassMember> getOverrides() {
    synchronized (StandardClassMember.class) {
      if (overridesView == null) {
        computeOverrides();
      }
      return overridesView;
    }
  }

  public String getPackage() {
    return packageName;
  }

  @Override
  public String getSourceName() {
    return sourceName;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return "ClassMember " + getSourceName();
  }

  /**
   * Compute overrides on demand.
   */
  private void computeOverrides() {
    Set<JDeclaredType> seen = new HashSet<JDeclaredType>();
    Set<JDeclaredType> toTraverse = new HashSet<JDeclaredType>();
    toTraverse.add(type);

    SortedSet<ClassMember> overrides = new TreeSet<ClassMember>(Member.SOURCE_NAME_COMPARATOR);

    while (!toTraverse.isEmpty()) {
      JDeclaredType currentType = toTraverse.iterator().next();
      seen.add(currentType);

      if (currentType != type) {
        overrides.add(factory.get(currentType));
      }

      if (currentType.getSuperClass() != null) {
        toTraverse.add(currentType.getSuperClass());
      }

      toTraverse.addAll(currentType.getImplements());

      toTraverse.removeAll(seen);
    }
    overridesView = Collections.unmodifiableSortedSet(overrides);
  }
}
