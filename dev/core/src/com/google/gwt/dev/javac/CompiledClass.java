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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.javac.TypeOracleMediator.TypeData;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.util.DiskCache;
import com.google.gwt.dev.util.DiskCacheToken;
import com.google.gwt.dev.util.Name.InternalName;
import com.google.gwt.dev.util.StringInterner;

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the state of a single compiled class file.
 */
public final class CompiledClass implements Serializable {

  private static final DiskCache diskCache = DiskCache.INSTANCE;

  static Collection<CompiledClass> copyForUnit(Collection<CompiledClass> in, CompilationUnit newUnit) {
    if (in == null) {
      return null;
    }
    CompiledClass[] orig = new CompiledClass[in.size()];
    List<CompiledClass> copy = new ArrayList<CompiledClass>();

    Map<CompiledClass, CompiledClass> enclosingClassMap = new HashMap<CompiledClass, CompiledClass>();
    for (CompiledClass cc : in) {
      CompiledClass copyCc = new CompiledClass(cc, newUnit);
      copy.add(copyCc);
      enclosingClassMap.put(cc, copyCc);
    }

    // Update the enclosing class references.   With enough effort, we could determine the
    // hierarchical relationship of compiled classes and initialize the copies with the
    // copied enclosing class, but this is less effort.
    for (CompiledClass copyCc : copy) {
      if (copyCc.enclosingClass == null) {
        continue;
      }
      CompiledClass newRef = enclosingClassMap.get(copyCc.enclosingClass);
      if (null == newRef) {
        throw new InternalCompilerException("Enclosing type not found for " + copyCc.sourceName);
      }
      copyCc.enclosingClass = newRef;
    }
    return Collections.unmodifiableCollection(copy);
  }

  /**
   * A token to retrieve this object's bytes from the disk cache. byte code is
   * placed in the cache when the object is deserialized.
   */
  private final DiskCacheToken classBytesToken;
  private CompiledClass enclosingClass;
  private final String internalName;
  private final boolean isLocal;
  private transient NameEnvironmentAnswer nameEnvironmentAnswer;
  private String signatureHash;

  private final String sourceName;
  private transient TypeData typeData;

  private CompilationUnit unit;

  /**
   * Create a compiled class from raw class bytes.
   * 
   * @param classBytes - byte code for this class
   * @param enclosingClass - outer class
   * @param isLocal Is this class a local class? (See the JLS rev 2 section
   *          14.3)
   * @param internalName the internal binary name for this class. e.g.
   *          {@code java/util/Map$Entry}. See
   *          {@link "http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html#14757"}
   */
  CompiledClass(byte[] classBytes, CompiledClass enclosingClass, boolean isLocal,
      String internalName) {
    this.enclosingClass = enclosingClass;
    this.internalName = StringInterner.get().intern(internalName);
    this.sourceName = StringInterner.get().intern(InternalName.toSourceName(internalName));
    this.classBytesToken = new DiskCacheToken(diskCache.writeByteArray(classBytes));
    this.isLocal = isLocal;
  }

  /**
   * Used for cloning all compiled classes in one compilation unit.
   */
  private CompiledClass(CompiledClass orig, CompilationUnit newUnit) {
    this.enclosingClass = orig.enclosingClass;
    this.internalName = orig.internalName;
    this.sourceName = orig.sourceName;
    this.classBytesToken = orig.classBytesToken;
    this.isLocal = orig.isLocal;
    this.typeData = orig.typeData;
    this.unit = newUnit;
    this.signatureHash = orig.signatureHash;
  }

  /**
   * Returns the bytes of the compiled class.
   */
  public byte[] getBytes() {
    return classBytesToken.readByteArray();
  }

  public CompiledClass getEnclosingClass() {
    return enclosingClass;
  }

  /**
   * Returns the class internal binary name for this type, e.g.
   * {@code java/util/Map$Entry}.
   */
  public String getInternalName() {
    return internalName;
  }

  /**
   * Returns the enclosing package, e.g. {@code java.util}.
   */
  public String getPackageName() {
    return Shared.getPackageNameFromBinary(internalName);
  }

  /**
   * Returns a hash code on the byte code of the class.
   */
  public String getSignatureHash() {
    if (signatureHash == null) {
      signatureHash = BytecodeSignatureMaker.getCompileDependencySignature(getBytes());
    }
    return signatureHash;
  }

  /**
   * Returns the qualified source name, e.g. {@code java.util.Map.Entry}.
   */
  public String getSourceName() {
    return sourceName;
  }

  public TypeData getTypeData() {
    if (typeData == null) {
      typeData =
          new TypeData(getPackageName(), getSourceName(), getInternalName(), null, getBytes(),
              getUnit().getLastModified());
    }
    return typeData;
  }

  public CompilationUnit getUnit() {
    return unit;
  }

  /**
   * Returns <code>true</code> if this is a local type, or if this type is
   * nested inside of any local type.
   */
  public boolean isLocal() {
    return isLocal;
  }

  @Override
  public String toString() {
    return internalName;
  }

  NameEnvironmentAnswer getNameEnvironmentAnswer() throws ClassFormatException {
    if (nameEnvironmentAnswer == null) {
      ClassFileReader cfr =
          new ClassFileReader(getBytes(), unit.getResourceLocation().toCharArray(), true);
      nameEnvironmentAnswer = new NameEnvironmentAnswer(cfr, null);
    }
    return nameEnvironmentAnswer;
  }

  void initUnit(CompilationUnit unit) {
    this.unit = unit;
  }
}
