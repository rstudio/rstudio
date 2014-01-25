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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Provides stable ordering and de-duplication of artifacts.
 */
public final class ArtifactSet implements SortedSet<Artifact<?>>, Serializable {

  private SortedSet<Artifact<?>> treeSet = new TreeSet<Artifact<?>>();

  public ArtifactSet() {
  }

  public ArtifactSet(Collection<? extends Artifact<?>> copyFrom) {
    addAll(copyFrom);
  }

  @Override
  public boolean add(Artifact<?> o) {
    return treeSet.add(o);
  }

  @Override
  public boolean addAll(Collection<? extends Artifact<?>> c) {
    return treeSet.addAll(c);
  }

  @Override
  public void clear() {
    treeSet.clear();
  }

  @Override
  public Comparator<? super Artifact<?>> comparator() {
    return treeSet.comparator();
  }

  @Override
  public boolean contains(Object o) {
    return treeSet.contains(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return treeSet.containsAll(c);
  }

  @Override
  public boolean equals(Object o) {
    return treeSet.equals(o);
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
   * @param <T> the desired type of Artifact
   * @param artifactType the desired type of Artifact
   * @return all Artifacts in the ArtifactSet assignable to the desired type
   */
  public <T extends Artifact<? super T>> SortedSet<T> find(
      Class<T> artifactType) {
    // TODO make this sub-linear (but must retain order for styles/scripts!)
    SortedSet<T> toReturn = new TreeSet<T>();
    for (Artifact<?> artifact : this) {
      if (artifactType.isInstance(artifact)) {
        toReturn.add(artifactType.cast(artifact));
      }
    }
    return toReturn;
  }

  @Override
  public Artifact<?> first() {
    return treeSet.first();
  }

  /**
   * Prevent further modification of the ArtifactSet. Any attempts to alter
   * the ArtifactSet after invoking this method will result in an
   * UnsupportedOperationException.
   */
  public void freeze() {
    if (treeSet instanceof TreeSet<?>) {
      treeSet = Collections.unmodifiableSortedSet(treeSet);
    }
  }

  @Override
  public int hashCode() {
    return treeSet.hashCode();
  }

  @Override
  public SortedSet<Artifact<?>> headSet(Artifact<?> toElement) {
    return treeSet.headSet(toElement);
  }

  @Override
  public boolean isEmpty() {
    return treeSet.isEmpty();
  }

  @Override
  public Iterator<Artifact<?>> iterator() {
    return treeSet.iterator();
  }

  @Override
  public Artifact<?> last() {
    return treeSet.last();
  }

  @Override
  public boolean remove(Object o) {
    return treeSet.remove(o);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return treeSet.removeAll(c);
  }

  /**
   * Possibly replace an existing Artifact.
   * 
   * @param artifact the replacement Artifact
   * @return <code>true</code> if an equivalent Artifact was already present.
   */
  public boolean replace(Artifact<?> artifact) {
    boolean toReturn = treeSet.remove(artifact);
    treeSet.add(artifact);
    return toReturn;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return treeSet.retainAll(c);
  }

  @Override
  public int size() {
    return treeSet.size();
  }

  @Override
  public SortedSet<Artifact<?>> subSet(Artifact<?> fromElement,
      Artifact<?> toElement) {
    return treeSet.subSet(fromElement, toElement);
  }

  @Override
  public SortedSet<Artifact<?>> tailSet(Artifact<?> fromElement) {
    return treeSet.tailSet(fromElement);
  }

  @Override
  public Object[] toArray() {
    return treeSet.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return treeSet.toArray(a);
  }

  @Override
  public String toString() {
    return treeSet.toString();
  }
}
