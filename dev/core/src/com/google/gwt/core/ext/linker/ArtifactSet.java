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
package com.google.gwt.core.ext.linker;

import java.io.Serializable;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Provides stable ordering and de-duplication of artifacts.
 */
public final class ArtifactSet extends TypeIndexedSet<Artifact<?>> implements Serializable {

  public ArtifactSet() {
    super(new TreeSet<Artifact<?>>());
  }

  public ArtifactSet(Collection<? extends Artifact<?>> copyFrom) {
    this();
    addAll(copyFrom);
  }

  /**
   * Find all Artifacts assignable to some base type. The returned value will be
   * a snapshot of the values in the ArtifactSet. An example of how this could
   * be used:
   * 
   * <pre>
   *   for (EmittedArtifact ea : artifactSet.find(EmittedArtifact.class)) {
   *     ...
   *   }
   * </pre>
   * 
   * <p>The returned SortedSet is immutable.
   *
   * @param <T> the desired type of Artifact
   * @param artifactType the desired type of Artifact
   * @return all Artifacts in the ArtifactSet assignable to the desired type
   */
  @SuppressWarnings("unchecked")
  public <T extends Artifact<? super T>> SortedSet<T> find(Class<T> artifactType) {
    return getTypeIndex().findAssignableTo(artifactType);
  }

  /**
   * Possibly replace an existing Artifact.
   *
   * @param artifact the replacement Artifact
   * @return <code>true</code> if an equivalent Artifact was already present.
   */
  public boolean replace(Artifact<?> artifact) {
    boolean toReturn = remove(artifact);
    add(artifact);
    return toReturn;
  }
}
