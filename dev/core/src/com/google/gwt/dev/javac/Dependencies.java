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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.dev.util.collect.Lists;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Tracks dependencies from a {@link CompilationUnit} to {@link CompiledClass
 * CompiledClasses}.
 */
class Dependencies implements Serializable {
  /**
   * Represents a {@link Ref} that has been previously persisted.
   */
  static class Ref implements Serializable {
    private final String internalName;
    private final String hash;

    private Ref(CompiledClass cc) {
      this(cc.getInternalName(), cc.getSignatureHash());
    }

    private Ref(String internalName, String hash) {
      this.internalName = internalName;
      this.hash = hash;
    }

    public String getInternalName() {
      return internalName;
    }

    public String getSignatureHash() {
      return hash;
    }
  }

  Map<String, Ref> qualified = new HashMap<String, Ref>(true);
  Map<String, Ref> simple = new HashMap<String, Ref>(true);
  private final List<String> apiRefs;
  private final String myPackage;

  Dependencies() {
    this.myPackage = "";
    this.apiRefs = Lists.create();
  }

  /**
   * Initializes the set of simple and qualified dependency names, but does not
   * resolve them.
   */
  Dependencies(String myPackage, List<String> unresolvedQualified, List<String> unresolvedSimple,
      List<String> apiRefs) {
    this.myPackage =
        StringInterner.get().intern((myPackage.length() == 0) ? "" : (myPackage + '.'));
    for (String qualifiedRef : unresolvedQualified) {
      qualified.put(qualifiedRef, null);
    }
    for (String simpleRef : unresolvedSimple) {
      simple.put(simpleRef, null);
    }
    this.apiRefs = apiRefs;
  }

  /**
   * Returns the list of API references used by {@link TypeOracle} to determine type availability.
   */
  List<String> getApiRefs() {
    return apiRefs;
  }

  /**
   * Returns the list of deps that cannot be resolved at all.
   */
  List<String> findMissingApiRefs(Set<String> allValidClasses) {
    List<String> result = Lists.create();
    for (String apiRef : apiRefs) {
      if (!allValidClasses.contains(apiRef)) {
        result = Lists.add(result, apiRef);
      }
    }
    return result;
  }

  /**
   * Resolves unqualified dependencies against the global list of all valid
   * classes. Must be called before {@link #validate(String, Map, Map)}.
   */
  void resolve(Map<String, CompiledClass> allValidClasses) {
    for (Entry<String, Ref> entry : qualified.entrySet()) {
      CompiledClass cc = allValidClasses.get(entry.getKey());
      if (cc != null) {
        entry.setValue(new Ref(cc));
      }
    }

    for (Entry<String, Ref> entry : simple.entrySet()) {
      CompiledClass cc = findBySimpleName(entry.getKey(), allValidClasses);
      if (cc != null) {
        entry.setValue(new Ref(cc));
      }
    }
  }

  /**
   * Validate that all of my existing dependencies can be found in the global
   * set of valid classes, and resolve to structurally identical APIs.
   *
   * @return <code>true</code> if all of my dependencies are valid
   */
  boolean validate(TreeLogger logger, Map<String, CompiledClass> allValidClasses) {
    for (Entry<String, Ref> entry : qualified.entrySet()) {
      CompiledClass theirs = allValidClasses.get(entry.getKey());
      if (!validateClass(logger, entry, theirs)) {
        return false;
      }
    }
    for (Entry<String, Ref> entry : simple.entrySet()) {
      CompiledClass theirs = findBySimpleName(entry.getKey(), allValidClasses);
      if (!validateClass(logger, entry, theirs)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Tries to resolve a simple name using Java lookup rules, first checking the
   * current package, then java.lang.
   */
  private CompiledClass findBySimpleName(String ref, Map<String, CompiledClass> allValidClasses) {
    CompiledClass cc = allValidClasses.get(myPackage + ref);
    if (cc != null) {
      return cc;
    }
    return allValidClasses.get("java.lang." + ref);
  }

  /**
   * Returns true if my class is the same as their class. Uses caching to avoid
   * recomputing diffs. Updates the my entry to 'their' class if non-identical
   * objects have the same structure.
   */
  private boolean validateClass(TreeLogger logger, Entry<String, Ref> entry, CompiledClass theirs) {
    Ref mine = entry.getValue();
    boolean result;
    if ((mine == null) != (theirs == null)) {
      if (logger.isLoggable(TreeLogger.DEBUG)) {
        logger.log(TreeLogger.DEBUG, "Invalid ref: " + entry.getKey() + " mine: "
            + (mine == null ? "null" : "not null") + " theirs: "
            + (theirs == null ? "null" : "not null"));
      }
      result = false;
    } else if (mine == null && theirs == null) {
      // For package dependencies, both references being null is always the case
      result = true;
    } else if (mine.getSignatureHash().equals(theirs.getSignatureHash())) {
      result = true;
    } else {
      if (logger.isLoggable(TreeLogger.DEBUG)) {
        logger.log(TreeLogger.DEBUG, entry.getKey() + " isn't structurally same.");
      }
      result = false;
    }
    return result;
  }
}
