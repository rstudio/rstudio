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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.Transferable;
import com.google.gwt.core.ext.linker.impl.PermutationsUtil.PermutationId;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Artifact that contains a mapping from deferred binding properties to a string (which typically
 * represents another artifact). These are typically paired with an emitted
 * artifact containing the results of {@link #getSerialized()}.
 */
@Transferable
public class PropertiesMappingArtifact extends Artifact<PropertiesMappingArtifact> {

  private final Map<PermutationId, List<Map<String, String>>> mappings;
  private final String serialized;

  public PropertiesMappingArtifact(Class<? extends Linker> linker,
      Map<PermutationId, List<Map<String, String>>> mappings) {
    super(linker);
    this.mappings = mappings;

  StringBuilder sb = new StringBuilder();
  for (Entry<PermutationId, List<Map<String, String>>> mapping : mappings.entrySet()) {
    for (Map<String, String> deferredBindings : mapping.getValue()) {
      sb.append(mapping.getKey().getStrongName() + ".cache.js");
      sb.append('\n');
      for (Entry<String, String> oneBinding : deferredBindings.entrySet()) {
        sb.append(oneBinding.getKey());
        sb.append(' ');
        sb.append(oneBinding.getValue());
        sb.append('\n');
      }
      sb.append('\n');
    }
  }

    serialized = sb.toString();
  }

  @Override
  public int compareToComparableArtifact(PropertiesMappingArtifact that) {
    return serialized.compareTo(that.getSerialized());
  }

  @Override
  public Class<PropertiesMappingArtifact> getComparableArtifactType() {
    return PropertiesMappingArtifact.class;
  }

  public String getSerialized() {
    return serialized;
  }

  @Override
  public int hashCode() {
    return serialized.hashCode();
  }
}
