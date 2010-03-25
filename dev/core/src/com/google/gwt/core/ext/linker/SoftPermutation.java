/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.core.ext.linker;

import java.io.Serializable;
import java.util.SortedMap;

/**
 * Represents a permutation of collapsed deferred-binding property values.
 */
public abstract class SoftPermutation implements Serializable {

  /**
   * Returns the soft permutation id that should be passed into
   * <code>gwtOnLoad</code>. The range of ids used for a compilation's soft
   * permutations may be disjoint and may not correspond to the index of the
   * SoftPermutation within the array returned from
   * {@link CompilationResult#getSoftPermutations()}.
   */
  public abstract int getId();

  /**
   * Returns only the collapsed selection properties that resulted in the
   * particular soft permutation. The SelectionProperties used may be disjoint
   * from the properties returned by {@link CompilationResult#getPropertyMap()}.
   */
  public abstract SortedMap<SelectionProperty, String> getPropertyMap();
}