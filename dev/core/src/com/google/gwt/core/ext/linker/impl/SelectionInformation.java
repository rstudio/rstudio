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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.Transferable;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Used by {@link SelectionScriptLinker} to hold selection information about an
 * individual compiled permutation. The linker generates one instance of this
 * class per permutation and then accumulates them in the final link, where it
 * generates a selection script.
 */
@Transferable
public class SelectionInformation extends Artifact<SelectionInformation> {
  private final int hashCode;
  private final TreeMap<String, String> propMap;
  private final int softPermutationId;
  private final String strongName;

  public SelectionInformation(String strongName, int softPermutationId,
      TreeMap<String, String> propMap) {
    super(SelectionScriptLinker.class);
    this.strongName = strongName;
    this.softPermutationId = softPermutationId;
    this.propMap = propMap;
    hashCode = strongName.hashCode() + softPermutationId * 19
        + propMap.hashCode() * 17 + 11;
  }

  public TreeMap<String, String> getPropMap() {
    return propMap;
  }

  public int getSoftPermutationId() {
    return softPermutationId;
  }

  public String getStrongName() {
    return strongName;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  protected int compareToComparableArtifact(SelectionInformation o) {
    // compare the strong names
    int cmp = getStrongName().compareTo(o.getStrongName());
    if (cmp != 0) {
      return cmp;
    }

    cmp = getSoftPermutationId() - o.getSoftPermutationId();
    if (cmp != 0) {
      return cmp;
    }

    // compare the size of the property maps
    if (getPropMap().size() != o.getPropMap().size()) {
      return getPropMap().size() - o.getPropMap().size();
    }

    // compare the key sets of the property maps
    List<String> myKeys = new ArrayList<String>(getPropMap().keySet());
    List<String> oKeys = new ArrayList<String>(o.getPropMap().keySet());
    for (int i = 0; i < myKeys.size(); i++) {
      cmp = myKeys.get(i).compareTo(oKeys.get(i));
      if (cmp != 0) {
        return cmp;
      }
    }

    // compare the property map values
    for (String key : getPropMap().keySet()) {
      cmp = getPropMap().get(key).compareTo(o.getPropMap().get(key));
      if (cmp != 0) {
        return cmp;
      }
    }

    return 0;
  }

  @Override
  protected Class<SelectionInformation> getComparableArtifactType() {
    return SelectionInformation.class;
  }
}