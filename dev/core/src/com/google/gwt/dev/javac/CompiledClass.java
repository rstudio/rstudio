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

import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.dev.javac.impl.Shared;
import com.google.gwt.dev.util.DiskCache;
import com.google.gwt.dev.util.Name.InternalName;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.NestedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;

/**
 * Encapsulates the state of a single compiled class file.
 */
public final class CompiledClass {

  private static final DiskCache diskCache = new DiskCache();

  private static ClassFile getClassFile(TypeDeclaration typeDecl,
      String binaryName) {
    for (ClassFile tryClassFile : typeDecl.compilationResult().getClassFiles()) {
      char[] tryBinaryName = CharOperation.concatWith(
          tryClassFile.getCompoundName(), '/');
      if (binaryName.equals(String.valueOf(tryBinaryName))) {
        return tryClassFile;
      }
    }
    assert false;
    return null;
  }

  /**
   * Returns <code>true</code> if this is a local type, or if this type is
   * nested inside of any local type.
   */
  private static boolean isLocalType(SourceTypeBinding binding) {
    SourceTypeBinding b = binding;
    while (!b.isStatic()) {
      if (b instanceof LocalTypeBinding) {
        return true;
      }
      b = ((NestedTypeBinding) b).enclosingType;
    }
    return false;
  }

  protected final CompiledClass enclosingClass;
  protected final String internalName;
  protected final boolean isLocal;
  protected final CompilationUnit unit;

  /**
   * A token to retrieve this object's bytes from the disk cache.
   */
  private final long cacheToken;

  // The state below is transient.
  private transient NameEnvironmentAnswer nameEnvironmentAnswer;
  private transient JRealClassType realClassType;
  // Can be killed after parent is CHECKED.
  private transient TypeDeclaration typeDeclaration;

  CompiledClass(CompilationUnit unit, TypeDeclaration typeDeclaration,
      CompiledClass enclosingClass) {
    this.unit = unit;
    this.typeDeclaration = typeDeclaration;
    this.enclosingClass = enclosingClass;
    SourceTypeBinding binding = typeDeclaration.binding;
    this.internalName = CharOperation.charToString(binding.constantPoolName());
    ClassFile classFile = getClassFile(typeDeclaration, internalName);
    byte[] bytes = classFile.getBytes();
    this.cacheToken = diskCache.writeByteArray(bytes);
    this.isLocal = isLocalType(binding);
  }

  /**
   * Returns the bytes of the compiled class.
   */
  public byte[] getBytes() {
    return diskCache.readByteArray(cacheToken);
  }

  public CompiledClass getEnclosingClass() {
    return enclosingClass;
  }

  /**
   * Returns the binary class name, e.g. {@code java/util/Map$Entry}.
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
   * Returns the qualified source name, e.g. {@code java.util.Map.Entry}.
   */
  public String getSourceName() {
    return InternalName.toSourceName(internalName);
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

  /**
   * All checking is done, free up internal state.
   */
  void checked() {
    this.typeDeclaration = null;
  }

  NameEnvironmentAnswer getNameEnvironmentAnswer() {
    if (nameEnvironmentAnswer == null) {
      try {
        ClassFileReader cfr = new ClassFileReader(getBytes(),
            unit.getDisplayLocation().toCharArray(), true);
        nameEnvironmentAnswer = new NameEnvironmentAnswer(cfr, null);
      } catch (ClassFormatException e) {
        throw new RuntimeException("Unexpectedly unable to parse class file", e);
      }
    }
    return nameEnvironmentAnswer;
  }

  JRealClassType getRealClassType() {
    return realClassType;
  }

  TypeDeclaration getTypeDeclaration() {
    return typeDeclaration;
  }

  void graveyard() {
    if (realClassType != null) {
      realClassType.invalidate();
    }
  }

  void invalidate() {
    nameEnvironmentAnswer = null;
    typeDeclaration = null;
    if (realClassType != null) {
      realClassType.invalidate();
      realClassType = null;
    }
  }

  void setRealClassType(JRealClassType realClassType) {
    this.realClassType = realClassType;
  }
}
