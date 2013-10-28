/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.core.ext.soyc.coderef;

import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The abstraction of any possible entity in the code that is register by soyc: classes, methods
 * and fields. It includes all contributed sizes per fragment.
 *
 */
public abstract class EntityDescriptor {

  /**
   * Stores the size contribution to each fragment for this entity.
   * Fragments are 0-based, and -1 means in no fragment
   */
  public static class Fragment {
    private int id = -1;
    private int size;

    public Fragment(int fragmentId, int fragmentSize) {
      id = fragmentId;
      size = fragmentSize;
    }

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public int getSize() {
      return size;
    }

    public void setSize(int size) {
      this.size = size;
    }
  }

  protected final String name;
  /**
   * Stores obfuscated names. An entity can have several obfuscated names, because it can be
   * presented in several forms, eg. in methods: original and its static version.
   */
  protected final Set<String> obfuscatedNames = Sets.newHashSet();
  // Some entities can be in several fragments
  protected final List<Fragment> fragments = Lists.newArrayList();

  public EntityDescriptor(String name) {
    this.name = name;
  }

  public void addFragment(Fragment fragment) {
    fragments.add(fragment);
  }

  /**
   * Returns the list of sizes per fragment contributed  by this entity.
   */
  public Collection<Fragment> getFragments() {
    return Collections.unmodifiableCollection(fragments);
  }

  /**
   * Returns the full qualified name.
   */
  public abstract String getFullName();

  /**
   * Returns the name of the entity. For instance, class entities return the short name
   * {@link com.google.gwt.dev.jjs.ast.JDeclaredType#getShortName()}, fields return the field name,
   * and methods return the method name without its signature.
   */
  public String getName() {
    return name;
  }

  public Set<String> getObfuscatedNames() {
    return Collections.unmodifiableSet(obfuscatedNames);
  }

  public void addObfuscatedName(String obfuscatedName) {
    this.obfuscatedNames.add(obfuscatedName);
  }
}
