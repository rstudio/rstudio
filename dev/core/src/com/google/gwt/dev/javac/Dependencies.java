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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Tracks dependencies from a {@link CompilationUnit} to {@link CompiledClass
 * CompiledClasses}.
 */
class Dependencies implements Serializable {

  /**
   * A {@Ref} that directly holds a {@link CompiledClass}. When
   * serialized, turns into a {@link SerializedRef}.
   */
  static class DirectRef extends Ref {
    private final CompiledClass target;

    private DirectRef(CompiledClass target) {
      assert target != null;
      this.target = target;
    }

    public CompiledClass getCompiledClass() {
      return target;
    }

    @Override
    public String getInternalName() {
      return target.getInternalName();
    }

    @Override
    public String getSignatureHash() {
      return target.getSignatureHash();
    }

    protected Object writeReplace() {
      return new SerializedRef(target.getInternalName(), getSignatureHash());
    }
  }

  /**
   * A Ref can hold either a direct reference to byte code of another class, or
   * a signature of the byte code. The signature is used when this class is
   * persisted.
   */
  abstract static class Ref implements Serializable {
    public abstract String getInternalName();

    public abstract String getSignatureHash();
  }

  /**
   * Represents a {@link Ref} that has been previously persisted.
   */
  private static class SerializedRef extends Ref {
    private final String internalName;
    private final String hash;

    private SerializedRef(String internalName, String hash) {
      this.internalName = internalName;
      this.hash = hash;
    }

    @Override
    public String getInternalName() {
      return internalName;
    }

    @Override
    public String getSignatureHash() {
      return hash;
    }
  }

  Map<String, Ref> qualified = new HashMap<String, Ref>();
  Map<String, Ref> simple = new HashMap<String, Ref>();
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
  Dependencies(String myPackage, List<String> unresolvedQualified,
      List<String> unresolvedSimple, List<String> apiRefs) {
    this.myPackage =
        StringInterner.get().intern(
            (myPackage.length() == 0) ? "" : (myPackage + '.'));
    for (String qualifiedRef : unresolvedQualified) {
      qualified.put(qualifiedRef, null);
    }
    for (String simpleRef : unresolvedSimple) {
      simple.put(simpleRef, null);
    }
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
    for (String ref : qualified.keySet()) {
      CompiledClass cc = allValidClasses.get(ref);
      if (cc != null) {
        qualified.put(ref, new DirectRef(cc));
      }
    }

    for (String ref : simple.keySet()) {
      CompiledClass cc = findBySimpleName(ref, allValidClasses);
      if (cc != null) {
        simple.put(ref, new DirectRef(cc));
      }
    }
  }

  /**
   * Validate that all of my existing dependencies can be found in the global
   * set of valid classes, and resolve to structurally identical APIs.
   * 
   * @return <code>true</code> if all of my dependencies are valid
   */
  boolean validate(Map<String, CompiledClass> allValidClasses,
      Map<CompiledClass, CompiledClass> cachedStructurallySame) {
    for (Entry<String, Ref> entry : qualified.entrySet()) {
      CompiledClass theirs = allValidClasses.get(entry.getKey());
      if (!validateClass(cachedStructurallySame, entry, theirs)) {
        return false;
      }
    }
    for (Entry<String, Ref> entry : simple.entrySet()) {
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

  private boolean structurallySame(Ref myRef, CompiledClass theirs,
      Map<CompiledClass, CompiledClass> cachedStructurallySame) {
    if (myRef == null && theirs == null) {
      return true;
    }
    assert myRef != null;
    assert theirs != null;

    // TODO(zundel): When we have a better hashing function that only
    // works on the public type signature, we can always use the hash
    if (myRef instanceof SerializedRef) {
      // compare hashes
      return myRef.getSignatureHash().equals(theirs.getSignatureHash());
    }
    assert myRef instanceof DirectRef;
    CompiledClass mine = ((DirectRef) myRef).getCompiledClass();
    if (mine == theirs) {
      // Identical.
      return true;
    }
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
      Entry<String, Ref> entry, CompiledClass theirs) {
    Ref mine = entry.getValue();
    boolean result;
    if ((mine == null) != (theirs == null)) {
      result = false;
    } else if (mine == null && theirs == null) {
      return true;
    } else if (structurallySame(mine, theirs, cachedStructurallySame)) {
      // Update our entry for identity.
      entry.setValue(new DirectRef(theirs));
      result = true;
    } else {
      result = false;
    }
    return result;
  }
}
