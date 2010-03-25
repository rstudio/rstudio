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

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

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
  private static String collapse(Permutation permutation,
      SortedSet<String> liveRebindRequests) {
    // Accumulates state
    SortedMap<String, SortedSet<String>> answers = new TreeMap<String, SortedSet<String>>();

    // Iterate over each map of rebind answers
    for (SortedMap<String, String> rebinds : permutation.getOrderedRebindAnswers()) {
      for (Map.Entry<String, String> rebind : rebinds.entrySet()) {
        if (!liveRebindRequests.contains(rebind.getKey())) {
          // Ignore rebinds that aren't actually used
          continue;
        }

        // Get-or-put
        SortedSet<String> set = answers.get(rebind.getKey());
        if (set == null) {
          set = new TreeSet<String>();
          answers.put(rebind.getKey(), set);
        }

        // Record rebind value
        set.add(rebind.getValue());
      }
    }

    // Create string
    return answers.toString();
  }

  public RebindAnswersPermutationKey(Permutation permutation,
      SortedSet<String> liveRebindRequests) {
    super(collapse(permutation, liveRebindRequests));
  }
}