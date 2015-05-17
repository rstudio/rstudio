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
package com.google.gwt.place.rebind;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.thirdparty.guava.common.collect.ComparisonChain;

import java.util.Comparator;

/**
 * Sorts types from most derived to least derived, falling back to alphabetical
 * sorting.
 */
class MostToLeastDerivedPlaceTypeComparator implements Comparator<JClassType> {
  public int compare(JClassType o1, JClassType o2) {
    if (o1.equals(o2)) {
      return 0;
    }
    return ComparisonChain.start()
        // We want the longest first, so we put o2 before o1 in the comparison chain.
        .compare(o2.getFlattenedSupertypeHierarchy().size(), o1.getFlattenedSupertypeHierarchy().size())
        .compare(o1.getQualifiedSourceName(), o2.getQualifiedSourceName())
        .result();
  }
}
