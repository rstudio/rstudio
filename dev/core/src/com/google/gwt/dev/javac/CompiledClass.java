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

import com.google.gwt.dev.util.DiskCache;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.Name.InternalName;

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Encapsulates the state of a single compiled class file.
 */
public final class CompiledClass implements Serializable {

  private static final DiskCache diskCache = DiskCache.INSTANCE;

  private final CompiledClass enclosingClass;
  private final String internalName;
  private final boolean isLocal;
  private CompilationUnit unit;
  private String signatureHash;

  /**
   * A token to retrieve this object's bytes from the disk cache. byte code is
   * placed in the cache when the object is deserialized.
   */
  private transient long cacheToken;
  private transient NameEnvironmentAnswer nameEnvironmentAnswer;

  /**
   * Create a compiled class from raw class bytes.
   * 
   * @param classBytes - byte code for this class
   * @param enclosingClass - outer class
   * @param isLocal Is this class a local class? (See the JLS rev 2 section
   *          14.3)
   * @param internalName the internal binary name for this class. e.g. {@code
   *          java/util/Map$Entry}. See
   *          {@link "http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html#14757"}
   */
  CompiledClass(byte[] classBytes, CompiledClass enclosingClass,
      boolean isLocal, String internalName) {
    this.enclosingClass = enclosingClass;
    this.internalName = StringInterner.get().intern(internalName);
    this.cacheToken = diskCache.writeByteArray(classBytes);
    this.isLocal = isLocal;
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
   * Returns the class internal binary name for this type, e.g. {@code
   * java/util/Map$Entry}.
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
   * 
   * TODO(zundel): should be a hash on only the public API for this class. 
   */
  public String getSignatureHash() {
    if (signatureHash == null) {
      signatureHash = Util.computeStrongName(getBytes());
    }
    return signatureHash;
  }

  /**
   * Returns the qualified source name, e.g. {@code java.util.Map.Entry}.
   */
  public String getSourceName() {
    return StringInterner.get().intern(InternalName.toSourceName(internalName));
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

  NameEnvironmentAnswer getNameEnvironmentAnswer() {
    if (nameEnvironmentAnswer == null) {
      try {
        ClassFileReader cfr =
            new ClassFileReader(getBytes(),
                unit.getDisplayLocation().toCharArray(), true);
        nameEnvironmentAnswer = new NameEnvironmentAnswer(cfr, null);
      } catch (ClassFormatException e) {
        throw new RuntimeException("Unexpectedly unable to parse class file", e);
      }
    }
    return nameEnvironmentAnswer;
  }

  void initUnit(CompilationUnit unit) {
    this.unit = unit;
  }

  private void readObject(ObjectInputStream inputStream)
      throws ClassNotFoundException, IOException {
    inputStream.defaultReadObject();
    this.cacheToken = diskCache.transferFromStream(inputStream);
  }

  private void writeObject(ObjectOutputStream outputStream) throws IOException {
    outputStream.defaultWriteObject();
    diskCache.transferToStream(cacheToken, outputStream);
  }
}
