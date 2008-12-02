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
package com.google.gwt.core.ext.soyc.impl;

import com.google.gwt.core.ext.soyc.Member;
import com.google.gwt.core.ext.soyc.Story;
import com.google.gwt.dev.jjs.SourceInfo.Mutation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;

/**
 * An implementation of the Story interface. This type has two additional pieces
 * of information not required by the Story interface. The first is a unique id
 * number and the second is a length. Instead of storing range objects for each
 * StoryImpl, we simply store the StoryImpls in order and calculate the Range
 * for the StoryImpl based on its length.
 * 
 * @see SnippetIterator#next()
 */
public class StoryImpl implements Story, Serializable {
  /**
   * Orders StoryImpl's by their id number.
   */
  public static final Comparator<Story> ID_COMPARATOR = new StoryImplComparator();

  private final int id;
  private final int fragment;
  private final int length;
  private final String literalDescription;
  private final SortedSet<Member> members;
  private final List<String> mutations;
  private final SortedSet<Origin> origins;

  /**
   * Standard constructor. This constructor will create unmodifiable versions of
   * the collections passed into it.
   */
  public StoryImpl(int id, SortedSet<Member> members, List<Mutation> mutations,
      SortedSet<Origin> origins, String literalDescription, int fragment,
      int length) {
    assert members != null;
    assert mutations != null;
    assert origins != null;
    assert fragment >= 0;
    assert length > 0;
    // literalDescription may be null

    this.id = id;
    this.fragment = fragment;
    this.length = length;
    this.literalDescription = literalDescription == null ? null
        : literalDescription.intern();
    this.members = Collections.unmodifiableSortedSet(members);

    List<String> mutableMutations = new ArrayList<String>(mutations.size());
    for (Mutation m : mutations) {
      mutableMutations.add(m.getDescription() + " by " + m.getCaller());
    }
    this.mutations = Collections.unmodifiableList(mutableMutations);
    this.origins = Collections.unmodifiableSortedSet(origins);
  }

  /**
   * This is a copy-constructor that's used when we subdivide a Range. All we
   * really care about in the shadow version is having a different length; all
   * of the other fields are initialized from the original.
   */
  public StoryImpl(StoryImpl other, int length) {
    this.id = other.id;
    this.fragment = other.fragment;
    this.length = length;
    this.literalDescription = other.literalDescription;
    this.members = other.members;
    this.mutations = other.mutations;
    this.origins = other.origins;
  }

  /**
   * Identity is based on the <code>id</code> field.
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof StoryImpl)) {
      return false;
    }
    StoryImpl o = (StoryImpl) obj;
    return id == o.id;
  }

  public int getFragment() {
    return fragment;
  }

  public int getId() {
    return id;
  }

  /**
   * Used internally, and not specified by the Story interface.
   */
  public int getLength() {
    return length;
  }

  public String getLiteralTypeName() {
    return literalDescription;
  }

  public SortedSet<Member> getMembers() {
    return members;
  }

  public List<String> getMutations() {
    return mutations;
  }

  public SortedSet<Origin> getSourceOrigin() {
    return origins;
  }

  /**
   * Identity is based on the <code>id</code> field.
   */
  @Override
  public int hashCode() {
    return id;
  }
}