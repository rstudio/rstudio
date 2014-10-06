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
package com.google.gwt.resources.gss;

import com.google.gwt.thirdparty.common.css.SubstitutionMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This SubstitutionMap is used for renaming each style classes of the ast by its corresponding
 * obfuscated name.
 * <p/>
 * It takes care of eventual prefix and does not rename external style classes.
 * <p/>
 * It lists the eventual external style class candidates (class without associated obfuscation
 * name)
 */
public class RenamingSubstitutionMap implements SubstitutionMap {
  private final Map<String, String> replacementMap;

  private Set<String> classes;
  private Set<String> externalClassCandidates;

  public RenamingSubstitutionMap(Map<String, Map<String, String>> replacementsWithPrefix) {
    this.replacementMap = computeReplacementMap(replacementsWithPrefix);

    classes = new HashSet<String>();
    externalClassCandidates = new HashSet<String>();
  }

  private Map<String, String> computeReplacementMap(
      Map<String, Map<String, String>> replacementsWithPrefix) {

    Map<String, String> result = new HashMap<String, String>();

    for (Entry<String, Map<String, String>> entry : replacementsWithPrefix.entrySet()) {
      final String prefix = entry.getKey();
      Map<String, String> replacement = new HashMap<String, String>();

      for (Entry<String, String> replacementEntry : entry.getValue().entrySet()) {
        replacement.put(prefix + replacementEntry.getKey(), replacementEntry.getValue());
      }

      result.putAll(replacement);
    }

    return result;
  }

  @Override
  public String get(String key) {
    classes.add(key);

    String replacement = replacementMap.get(key);

    if (replacement == null) {
      // could be an external style class
      externalClassCandidates.add(key);
      return key;
    }

    return replacement;
  }

  public Set<String> getStyleClasses() {
    return classes;
  }

  public Set<String> getExternalClassCandidates() {
    return externalClassCandidates;
  }
}
