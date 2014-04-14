/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.jjs.impl.gflow;

import com.google.gwt.thirdparty.guava.common.base.Preconditions;

import java.util.List;

/**
 * Utilities for working with assumption values.
 */
public class AssumptionUtil {
  /**
   * Check assumptions for equality.
   */
  public static <A extends Assumption<A>> boolean equals(A a1, A a2) {
    if (a1 == null || a2 == null) {
      return a1 == a2;
    }

    return (a1 == a2) || (a1.equals(a2));
  }

  /**
   * Join assumptions.
   */
  public static <A extends Assumption<A>> A join(A a1, A a2) {
    if (a1 == null) {
      return a2;
    }

    if (a2 == null) {
      return a1;
    }

    return a1 != a2 ? a1.join(a2) : a1;
  }

  /**
   * Join assumptions from the list.
   */
  public static <A extends Assumption<A>> A join(List<A> assumptions) {
    A result = null;
    for (int i = 0; i < assumptions.size(); ++i) {
      result = join(result, assumptions.get(i));
    }
    return result;
  }

  public static <E, A extends Assumption<A>> A join(List<E> edges,
      AssumptionMap<E, A> assumptionMap) {
    A result = null;
    for (int i = 0; i < edges.size(); ++i) {
      result = join(result, assumptionMap.getAssumption(edges.get(i)));
    }
    return result;
  }

  public static <E, A extends Assumption<A>> void setAssumptions(List<E> edges,
      List<A> assumptions, AssumptionMap<E, A> assumptionMap) {
    Preconditions.checkArgument(assumptions.size() == edges.size());
    for (int i = 0; i < edges.size(); ++i) {
      assumptionMap.setAssumption(edges.get(i), assumptions.get(i));
    }
  }

  public static <E, A extends Assumption<A>> void setAssumptions(List<E> edges,
      A assumption, AssumptionMap<E, A> assumptionMap) {
    for (int i = 0; i < edges.size(); ++i) {
      assumptionMap.setAssumption(edges.get(i), assumption);
    }
  }

  public static <E, A extends Assumption<A>> String toString(
      List<E> inEdges, List<E> outEdges,
      AssumptionMap<E, A> assumptionMap) {
    StringBuilder result = new StringBuilder();
    for (E e : inEdges) {
      if (result.length() != 0) {
        result.append("; ");
      }
      result.append(e);
      result.append("=");
      result.append(assumptionMap.getAssumption(e));
    }
    for (E e : outEdges) {
      if (result.length() != 0) {
        result.append("; ");
      }
      result.append(e);
      result.append("=");
      result.append(assumptionMap.getAssumption(e));
    }
    return result.toString();
  }
}
