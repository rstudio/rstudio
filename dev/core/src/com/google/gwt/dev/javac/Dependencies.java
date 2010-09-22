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

import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.dev.util.collect.Lists;

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Tracks dependencies from a {@link CompilationUnit} to {@link CompiledClass
 * CompiledClasses}.
 */
class Dependencies {
  Map<String, CompiledClass> qualified = new HashMap<String, CompiledClass>();
  Map<String, CompiledClass> simple = new HashMap<String, CompiledClass>();
  private final List<String> apiRefs;
  private final String myPackage;
  private List<String> unresolvedQualified;
  private List<String> unresolvedSimple;

  Dependencies() {
    this.myPackage = "";
    this.apiRefs = Lists.create();
  }

  /**
   * Initializes the set of simple and qualified dependency names, but does not
   * resolve them.
   */
  Dependencies(String myPackage, List<String> unresolvedQualified,
      List<String> unresolvedSimple, List<String> apiRefs) {
    this.myPackage = StringInterner.get().intern(
        (myPackage.length() == 0) ? "" : (myPackage + '.'));
    this.unresolvedQualified = unresolvedQualified;
    this.unresolvedSimple = unresolvedSimple;
    this.apiRefs = apiRefs;
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
    for (String ref : unresolvedQualified) {
      CompiledClass cc = allValidClasses.get(ref);
      qualified.put(ref, cc);
    }

    for (String ref : unresolvedSimple) {
      CompiledClass cc = findBySimpleName(ref, allValidClasses);
      allValidClasses.get(ref);
      simple.put(ref, cc);
    }
    unresolvedQualified = unresolvedSimple = null;
  }

  /**
   * Validate that all of my existing dependencies can be found in the global
   * set of valid classes, and resolve to structurally identical APIs.
   * 
   * @return <code>true</code> if all of my dependencies are valid
   */
  boolean validate(Map<String, CompiledClass> allValidClasses,
      Map<CompiledClass, CompiledClass> cachedStructurallySame) {
    for (Entry<String, CompiledClass> entry : qualified.entrySet()) {
      CompiledClass theirs = allValidClasses.get(entry.getKey());
      if (!validateClass(cachedStructurallySame, entry, theirs)) {
        return false;
      }
    }
    for (Entry<String, CompiledClass> entry : simple.entrySet()) {
      CompiledClass theirs = findBySimpleName(entry.getKey(), allValidClasses);
      if (!validateClass(cachedStructurallySame, entry, theirs)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Tries to resolve a simple name using Java lookup rules, first checking the
   * current package, then java.lang.
   */
  private CompiledClass findBySimpleName(String ref,
      Map<String, CompiledClass> allValidClasses) {
    CompiledClass cc = allValidClasses.get(myPackage + ref);
    if (cc != null) {
      return cc;
    }
    return allValidClasses.get("java.lang." + ref);
  }

  private boolean hasStructuralChanges(CompiledClass mine, CompiledClass theirs) {
    try {
      ClassFileReader cfr = new ClassFileReader(theirs.getBytes(), null);
      return cfr.hasStructuralChanges(mine.getBytes());
    } catch (ClassFormatException e) {
      throw new RuntimeException("Unexpected error reading compiled class", e);
    }
  }

  private boolean structurallySame(CompiledClass mine, CompiledClass theirs,
      Map<CompiledClass, CompiledClass> cachedStructurallySame) {
    if (cachedStructurallySame.get(mine) == theirs) {
      return true;
    }
    if (cachedStructurallySame.containsKey(mine)) {
      return false;
    }
    boolean isSame = !hasStructuralChanges(mine, theirs);
    if (isSame) {
      cachedStructurallySame.put(mine, theirs);
    } else {
      cachedStructurallySame.put(mine, null);
    }
    return isSame;
  }

  /**
   * Returns true if my class is the same as their class. Uses caching to avoid
   * recomputing diffs. Updates the my entry to 'their' class if non-identical
   * objects have the same structure.
   */
  private boolean validateClass(
      Map<CompiledClass, CompiledClass> cachedStructurallySame,
      Entry<String, CompiledClass> entry, CompiledClass theirs) {
    CompiledClass mine = entry.getValue();
    boolean result;
    if (mine == theirs) {
      // Identical.
      result = true;
    } else if ((mine == null) != (theirs == null)) {
      result = false;
    } else if (structurallySame(mine, theirs, cachedStructurallySame)) {
      // Update our entry for identity.
      entry.setValue(theirs);
      result = true;
    } else {
      result = false;
    }
    return result;
  }
}
