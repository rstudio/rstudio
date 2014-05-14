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
package com.google.gwt.dev;

import com.google.gwt.dev.util.StringKey;

import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Creates a string representation of live rebound types to all possible
 * answers.
 *
 * <pre>
 * {
 *   Foo = [ FooImpl ], // A "hard" rebind
 *   Bundle = [ Bundle_EN, Bundle_FR ]  // A "soft" rebind
 * }
 * </pre>
 */
class RebindAnswersPermutationKey extends StringKey {
  private static String collapse(Permutation permutation, Set<String> liveRebindRequests) {

    SortedMap<String, SortedSet<String>> answers =
        GwtCreateMap.getPossibleAnswers(permutation.getGwtCreateAnswers(), liveRebindRequests);

    // Create string
    return answers.toString();
  }

  public RebindAnswersPermutationKey(Permutation permutation, Set<String> liveRebindRequests) {
    super(collapse(permutation, liveRebindRequests));
  }
}
