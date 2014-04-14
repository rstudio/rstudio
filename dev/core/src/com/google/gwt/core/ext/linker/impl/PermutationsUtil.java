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

package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.dev.util.StringKey;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.dev.util.collect.Lists;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A utility class to help linkers generate a list of permutation mappings and
 * and then either output them to javascript code which selects the correct
 * permutation, or to a file which can be parsed by server-side code.
 */
public class PermutationsUtil {

  /**
   * This represents the combination of a unique content hash (i.e. the MD5 of
   * the bytes to be written into the cache.html file) and a soft permutation
   * id.
   */
  protected static class PermutationId extends StringKey {
    private final int softPermutationId;
    private final String strongName;

    public PermutationId(String strongName, int softPermutationId) {
      super(strongName + ":" + softPermutationId);
      this.strongName = strongName;
      this.softPermutationId = softPermutationId;
    }

    public int getSoftPermutationId() {
      return softPermutationId;
    }

    public String getStrongName() {
      return strongName;
    }
  }

  /**
   * This maps each unique permutation to the property settings for that
   * compilation. A single compilation can have multiple property settings if
   * the compiles for those settings yielded the exact same compiled output.
   */
  protected SortedMap<PermutationId, List<Map<String, String>>> propMapsByPermutation =
      new TreeMap<PermutationId, List<Map<String, String>>>();

  /**
   * Uses the internal map to insert JS to select a permutation into the
   * selection script.
   *
   * @param selectionScript
   * @param logger
   * @param context
   * @return the modified selectionScript buffer
   * @throws UnableToCompleteException
   */
  public StringBuffer addPermutationsJs(StringBuffer selectionScript,
      TreeLogger logger, LinkerContext context)
      throws UnableToCompleteException {
    int startPos;

    PropertiesUtil.addPropertiesJs(selectionScript, logger, context);

    // Possibly add permutations
    startPos = selectionScript.indexOf("// __PERMUTATIONS_END__");
    if (startPos != -1) {
      StringBuffer text = new StringBuffer();
      if (propMapsByPermutation.size() == 0) {
        // Hosted mode link.
        text.append("alert(\"GWT module '" + context.getModuleName()
            + "' may need to be (re)compiled\");");
        text.append("return;");

      } else if (propMapsByPermutation.size() == 1) {
        // Just one distinct compilation; no need to evaluate properties
        text.append("strongName = '"
            + propMapsByPermutation.keySet().iterator().next().getStrongName()
            + "';");
      } else {
        Set<String> propertiesUsed = new HashSet<String>();
        for (PermutationId permutationId : propMapsByPermutation.keySet()) {
          for (Map<String, String> propertyMap : propMapsByPermutation.get(permutationId)) {
            // unflatten([v1, v2, v3], 'strongName' + ':softPermId');
            // The soft perm ID is concatenated to improve string interning
            text.append("unflattenKeylistIntoAnswers([");
            boolean needsComma = false;
            for (SelectionProperty p : context.getProperties()) {
              if (p.tryGetValue() != null) {
                continue;
              } else if (p.isDerived()) {
                continue;
              }

              if (needsComma) {
                text.append(",");
              } else {
                needsComma = true;
              }
              text.append("'" + propertyMap.get(p.getName()) + "'");
              propertiesUsed.add(p.getName());
            }

            text.append("], '").append(permutationId.getStrongName()).append(
                "'");
            /*
             * For compatibility with older linkers, skip the soft permutation
             * if it's 0
             */
            if (permutationId.getSoftPermutationId() != 0) {
              text.append(" + ':").append(permutationId.getSoftPermutationId()).append(
                  "'");
            }
            text.append(");\n");
          }
        }

        // strongName = answers[compute('p1')][compute('p2')];
        text.append("strongName = answers[");
        boolean needsIndexMarkers = false;
        for (SelectionProperty p : context.getProperties()) {
          if (!propertiesUsed.contains(p.getName())) {
            continue;
          }
          if (needsIndexMarkers) {
            text.append("][");
          } else {
            needsIndexMarkers = true;
          }
          text.append("computePropValue('" + p.getName() + "')");
        }
        text.append("];");
      }
      selectionScript.insert(startPos, text);
    }

    return selectionScript;
  }

  public SortedMap<PermutationId, List<Map<String, String>>> getPermutationsMap() {
    return propMapsByPermutation;
  }

  /**
   * Find all instances of {@link SelectionInformation} and add them to the
   * internal map of selection information.
   */
  public void setupPermutationsMap(ArtifactSet artifacts) {
    propMapsByPermutation =
      new TreeMap<PermutationId, List<Map<String, String>>>();
    for (SelectionInformation selInfo : artifacts.find(SelectionInformation.class)) {
      TreeMap<String, String> entries = selInfo.getPropMap();
      PermutationId permutationId = new PermutationId(selInfo.getStrongName(),
          selInfo.getSoftPermutationId());
      if (!propMapsByPermutation.containsKey(permutationId)) {
        propMapsByPermutation.put(permutationId,
            Lists.<Map<String, String>> create(entries));
      } else {
        propMapsByPermutation.put(permutationId, Lists.add(
            propMapsByPermutation.get(permutationId), entries));
      }
    }
  }
}
