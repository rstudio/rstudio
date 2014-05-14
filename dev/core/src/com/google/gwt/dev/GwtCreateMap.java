/*
 * Copyright 2014 Google Inc.
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

import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * A map from GWT.create() arguments to the types that will be constructed.
 * (Both keys and values are Java source type names.)
 *
 * <p>Each map typically contains the answers for one (soft) permutation.
 */
public class GwtCreateMap implements Serializable {
  private final TreeMap<String, String> answers;

  public GwtCreateMap() {
    answers = Maps.newTreeMap();
  }

  private GwtCreateMap(TreeMap<String, String> answers) {
    this.answers = answers;
  }

  /**
   * Returns the Java class that a GWT.create() call will construct.
   *
   * @param key the GWT.create() argument (source type name)
   * @return a source type name
   */
  public String get(String key) {
    return answers.get(key);
  }

  /**
   * Sets the Java class that a GWT.create() call will construct.
   *
   * @param key a source type name
   * @param value a source type name
   */
  void put(String key, String value) {
    answers.put(key, value);
  }

  /**
   * Returns true if the map contains a answer for the given GWT.create() argument.
   */
  public boolean containsKey(String key) {
    return answers.containsKey(key);
  }

  /**
   * Asserts that two maps contain the same answers for the given GWT.create() arguments.
   */
  void assertSameAnswers(GwtCreateMap other, Iterable<String> keysToCheck) {
    for (String key : keysToCheck) {
      assert answers.get(key).equals(other.answers.get(key));
    }
  }

  /**
   * Returns the answers that are the same in every permutation.
   */
  public static GwtCreateMap getCommonAnswers(Iterable<GwtCreateMap> maps) {
    Iterator<GwtCreateMap> it = maps.iterator();

    // Start with an arbitrary copy
    TreeMap<String, String> out = Maps.newTreeMap(it.next().answers);

    while (it.hasNext()) {
      GwtCreateMap next = it.next();
      // Only keep key/value pairs present in both maps.
      out.entrySet().retainAll(next.answers.entrySet());
    }

    return new GwtCreateMap(out);
  }

  /**
   * Returns the Java classes that GWT.create() might return in at least one permutation.
   *
   * @param keysWanted the GWT.create() arguments to include (source type names).
   */
  public static SortedMap<String, SortedSet<String>> getPossibleAnswers(Iterable<GwtCreateMap> maps,
      Set<String> keysWanted) {

    SortedMap<String, SortedSet<String>> out = Maps.newTreeMap();

    for (GwtCreateMap map : maps) {
      for (Map.Entry<String, String> entry : map.answers.entrySet()) {
        if (!keysWanted.contains(entry.getKey())) {
          continue;
        }

        SortedSet<String> answers = out.get(entry.getKey());
        if (answers == null) {
          answers = Sets.newTreeSet();
          out.put(entry.getKey(), answers);
        }

        answers.add(entry.getValue());
      }
    }

    return out;
  }

  /**
   * Given an argument to GWT.create(), returns a map containing each possible return type and the
   * permutations that would create it.
   *
   * @return a map from a Java class (source type name) to a list of indexes into the maps list.
   */
  public static Map<String, List<Integer>> getAnswerPermutations(List<GwtCreateMap> maps, String key) {

    Map<String, List<Integer>> out = Maps.newLinkedHashMap();

    int permutationCount = maps.size();
    for (int i = 0; i < permutationCount; i++) {
      String answerType = maps.get(i).get(key);

      List<Integer> list = out.get(answerType);
      if (list == null) {
        list = new ArrayList<Integer>();
        out.put(answerType, list);
      }

      list.add(i);
    }
    return out;
  }
}
