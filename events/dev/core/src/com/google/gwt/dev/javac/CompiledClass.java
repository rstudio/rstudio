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

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the state of a single compiled class file.
 */
public final class CompiledClass {

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

  private static String getPackagePrefix(String packageName) {
    return packageName.length() > 0 ? packageName + "." : "";
  }

  protected final String binaryName;
  protected final byte[] bytes;
  protected final CompiledClass enclosingClass;
  protected final String location;
  protected final String packageName;
  protected final String sourceName;
  protected final CompilationUnit unit;

  // The state below is transient.
  private List<JsniMethod> jsniMethods;
  private NameEnvironmentAnswer nameEnvironmentAnswer;
  private JRealClassType realClassType;
  // Can be killed after parent is CHECKED.
  private TypeDeclaration typeDeclaration;

  CompiledClass(CompilationUnit unit, TypeDeclaration typeDeclaration,
      CompiledClass enclosingClass) {
    this.unit = unit;
    this.typeDeclaration = typeDeclaration;
    this.enclosingClass = enclosingClass;
    SourceTypeBinding binding = typeDeclaration.binding;
    this.binaryName = CharOperation.charToString(binding.constantPoolName());
    this.packageName = Shared.getPackageNameFromBinary(binaryName);
    if (binding instanceof LocalTypeBinding) {
      // The source name of a local type must be determined from binary.
      String qualifiedName = binaryName.replace('/', '.');
      this.sourceName = qualifiedName.replace('$', '.');
    } else {
      this.sourceName = getPackagePrefix(packageName)
          + String.valueOf(binding.qualifiedSourceName());
    }
    ClassFile classFile = getClassFile(typeDeclaration, binaryName);
    this.bytes = classFile.getBytes();
    this.location = String.valueOf(classFile.fileName());
  }

  /**
   * Returns the binary class name, e.g. {@code java/util/Map$Entry}.
   */
  public String getBinaryName() {
    return binaryName;
  }

  /**
   * Returns the bytes of the compiled class.
   */
  public byte[] getBytes() {
    return bytes;
  }

  public CompiledClass getEnclosingClass() {
    return enclosingClass;
  }

  public List<JsniMethod> getJsniMethods() {
    return jsniMethods;
  }

  /**
   * Returns the enclosing package, e.g. {@code java.util}.
   */
  public String getPackageName() {
    return packageName;
  }

  /**
   * Returns the qualified source name, e.g. {@code java.util.Map.Entry}.
   */
  public String getSourceName() {
    return sourceName;
  }

  public CompilationUnit getUnit() {
    return unit;
  }

  @Override
  public String toString() {
    return binaryName;
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
        ClassFileReader cfr = new ClassFileReader(bytes, location.toCharArray());
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

  void invalidate() {
    nameEnvironmentAnswer = null;
    typeDeclaration = null;
    jsniMethods = null;
    if (realClassType != null) {
      realClassType.invalidate();
      realClassType = null;
    }
  }

  void setJsniMethods(List<JsniMethod> jsniMethods) {
    this.jsniMethods = Collections.unmodifiableList(jsniMethods);
  }

  void setRealClassType(JRealClassType realClassType) {
    this.realClassType = realClassType;
  }
}
