/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.javac.asm;

import com.google.gwt.dev.asm.AnnotationVisitor;
import com.google.gwt.dev.asm.FieldVisitor;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.asm.commons.EmptyVisitor;
import com.google.gwt.dev.asm.signature.SignatureReader;
import com.google.gwt.dev.asm.signature.SignatureVisitor;

import java.util.HashSet;
import java.util.Set;

/**
 * Collect all the types which are referenced by a particular class.
 */
public class CollectReferencesVisitor extends EmptyVisitor {

  /**
   * Collect type names from generic signatures.
   * 
   * All we care about is picking up type names, so we just return ourselves for
   * nested visitors.
   */
  private class CollectGenericTypes implements SignatureVisitor {
    public SignatureVisitor visitArrayType() {
      return this;
    }

    public void visitBaseType(char descriptor) {
    }

    public SignatureVisitor visitClassBound() {
      return this;
    }

    public void visitClassType(String name) {
      referencedTypes.add(name);
    }

    public void visitEnd() {
    }

    public SignatureVisitor visitExceptionType() {
      return this;
    }

    public void visitFormalTypeParameter(String name) {
    }

    public void visitInnerClassType(String name) {
    }

    public SignatureVisitor visitInterface() {
      return this;
    }

    public SignatureVisitor visitInterfaceBound() {
      return this;
    }

    public SignatureVisitor visitParameterType() {
      return this;
    }

    public SignatureVisitor visitReturnType() {
      return this;
    }

    public SignatureVisitor visitSuperclass() {
      return this;
    }

    public void visitTypeArgument() {
    }

    public SignatureVisitor visitTypeArgument(char wildcard) {
      return this;
    }

    public void visitTypeVariable(String name) {
    }
  }

  // internal names
  protected Set<String> referencedTypes = new HashSet<String>();

  public Set<String> getReferencedTypes() {
    return referencedTypes;
  }

  @Override
  public void visit(int version, int access, String name, String signature,
      String superName, String[] interfaces) {
    if (superName != null) {
      referencedTypes.add(superName);
    }
    if (interfaces != null) {
      for (String intf : interfaces) {
        referencedTypes.add(intf);
      }
    }
    collectTypesFromClassSignature(signature);
  }

  @Override
  public void visit(String name, Object value) {
    if (value instanceof Type) {
      addTypeIfClass((Type) value);
    }
  }

  @Override
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    // don't mark this annotation as a reference or its arguments, so we can
    // handle binary-only annotations.
    // TODO(jat): consider implications of updating the annotation class
    return null;
  }

  @Override
  public void visitEnum(String name, String desc, String value) {
    addTypeIfClass(desc);
  }

  @Override
  public FieldVisitor visitField(int access, String name, String desc,
      String signature, Object value) {
    addTypeIfClass(desc);
    collectTypesFromFieldSignature(signature);
    // we don't use visitEnd, so we can just use ourselves for nested visitors
    return this;
  }

  /**
   * @param name internal name of the inner class
   * @param outerName internal name of the enclosing class
   */
  @Override
  public void visitInnerClass(String name, String outerName, String innerName,
      int access) {
    referencedTypes.add(name);
    if (outerName != null) {
      referencedTypes.add(outerName);
    }
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
      String signature, String[] exceptions) {
    for (Type type : Type.getArgumentTypes(desc)) {
      addTypeIfClass(type);
    }
    addTypeIfClass(Type.getReturnType(desc));
    collectTypesFromClassSignature(signature);
    // we don't use visitEnd, so we can just use ourselves for nested visitors
    return this;
  }

  /**
   * @param owner internal name of owning class
   */
  @Override
  public void visitOuterClass(String owner, String name, String desc) {
    referencedTypes.add(owner);
  }

  protected void addTypeIfClass(String desc) {
    addTypeIfClass(Type.getType(desc));
  }

  protected void addTypeIfClass(Type type) {
    if (type.getSort() == Type.OBJECT) {
      referencedTypes.add(type.getInternalName());
    }
  }

  private void collectTypesFromClassSignature(String signature) {
    if (signature == null) {
      return;
    }
    SignatureReader reader = new SignatureReader(signature);
    reader.accept(new CollectGenericTypes());
  }

  private void collectTypesFromFieldSignature(String signature) {
    if (signature == null) {
      return;
    }
    SignatureReader reader = new SignatureReader(signature);
    reader.acceptType(new CollectGenericTypes());
  }
}
