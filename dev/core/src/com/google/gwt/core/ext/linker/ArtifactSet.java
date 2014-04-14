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
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Provides stable ordering and de-duplication of artifacts.
 */
public final class ArtifactSet implements SortedSet<Artifact<?>>, Serializable {

  private final TypeIndexedSet<Artifact<?>> delegate =
      new TypeIndexedSet<Artifact<?>>(new TreeSet<Artifact<?>>());

  public ArtifactSet() {
  }

  public ArtifactSet(Collection<? extends Artifact<?>> copyFrom) {
    addAll(copyFrom);
  }

  @Override
  public boolean add(Artifact<?> o) {
    return delegate.add(o);
  }

  @Override
  public boolean addAll(Collection<? extends Artifact<?>> c) {
    return delegate.addAll(c);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public Comparator<? super Artifact<?>> comparator() {
    return delegate.comparator();
  }

  @Override
  public boolean contains(Object o) {
    return delegate.contains(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return delegate.containsAll(c);
  }

  @Override
  public boolean equals(Object o) {
    return delegate.equals(o);
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
  public <T extends Artifact<? super T>> SortedSet<T> find(Class<T> artifactType) {
    return delegate.getTypeIndex().findAssignableTo(artifactType);
  }

  @Override
  public Artifact<?> first() {
    return delegate.first();
  }

  /**
   * Prevent further modification of the ArtifactSet. Any attempts to alter
   * the ArtifactSet after invoking this method will result in an
   * UnsupportedOperationException.
   */
  public void freeze() {
    delegate.freeze();
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public SortedSet<Artifact<?>> headSet(Artifact<?> toElement) {
    return delegate.headSet(toElement);
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public Iterator<Artifact<?>> iterator() {
    return delegate.iterator();
  }

  @Override
  public Artifact<?> last() {
    return delegate.last();
  }

  @Override
  public boolean remove(Object o) {
    return delegate.remove(o);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return delegate.removeAll(c);
  }

  /**
   * Possibly replace an existing Artifact.
   *
   * @param artifact the replacement Artifact
   * @return <code>true</code> if an equivalent Artifact was already present.
   */
  public boolean replace(Artifact<?> artifact) {
    boolean toReturn = delegate.remove(artifact);
    delegate.add(artifact);
    return toReturn;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return delegate.retainAll(c);
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public SortedSet<Artifact<?>> subSet(Artifact<?> fromElement,
      Artifact<?> toElement) {
    return delegate.subSet(fromElement, toElement);
  }

  @Override
  public SortedSet<Artifact<?>> tailSet(Artifact<?> fromElement) {
    return delegate.tailSet(fromElement);
  }

  @Override
  public Object[] toArray() {
    return delegate.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return delegate.toArray(a);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
