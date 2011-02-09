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
package com.google.gwt.core.ext.soyc;

import java.io.Serializable;
import java.util.Comparator;

/**
 * The Member type hierarchy represents structural or logical structures in the
 * compiled output. Members don't have a getRange() function because the bytes
 * derived from the member are likely disjoint.
 */
public interface Member extends Serializable {

  /**
   * Compares Members based solely on source name. This comparator is faster
   * than {@link #TYPE_AND_SOURCE_NAME_COMPARATOR}, but is only appropriate for
   * use with homogeneous collections of Members.
   */
  Comparator<Member> SOURCE_NAME_COMPARATOR = new SourceNameComparator();

  /**
   * Compares Members based on type and source name.
   */
  Comparator<Member> TYPE_AND_SOURCE_NAME_COMPARATOR = new TypeAndSourceNameComparator();

  /**
   * Returns the name of the Member in the original source code.
   */
  String getSourceName();

  /**
   * Returns the Member if it is a ClassMember or <code>null</code>.
   */
  ClassMember isClass();

  /**
   * Returns the Member if it is a FieldMember or <code>null</code>.
   */
  FieldMember isField();

  /**
   * Returns the Member if it is a MethodMember or <code>null</code>.
   */
  MethodMember isMethod();
}