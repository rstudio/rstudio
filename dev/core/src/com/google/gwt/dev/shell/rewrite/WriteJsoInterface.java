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
package com.google.gwt.dev.shell.rewrite;

import com.google.gwt.dev.asm.AnnotationVisitor;
import com.google.gwt.dev.asm.Attribute;
import com.google.gwt.dev.asm.ClassAdapter;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.FieldVisitor;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Opcodes;

/**
 * Writes an empty interface to stand in for a JSO type.
 */
class WriteJsoInterface extends ClassAdapter {
  /**
   * Construct a instance.
   * 
   * @param cv the visitor to chain to
   */
  public WriteJsoInterface(ClassVisitor cv) {
    super(cv);
  }

  @Override
  public void visit(int version, int access, String name, String signature,
      String superName, String[] interfaces) {
    if ("java/lang/Object".equals(superName)) {
      interfaces = null;
    } else {
      interfaces = new String[] {superName};
    }
    super.visit(version, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE, name,
        signature, "java/lang/Object", interfaces);
  }

  @Override
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    return null;
  }

  @Override
  public void visitAttribute(Attribute attr) {
  }

  @Override
  public void visitEnd() {
    super.visitEnd();
  }

  @Override
  public FieldVisitor visitField(int access, String name, String desc,
      String signature, Object value) {
    return null;
  }

  @Override
  public void visitInnerClass(String name, String outerName, String innerName,
      int access) {
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
      String signature, String[] exceptions) {
    return null;
  }

  @Override
  public void visitOuterClass(String owner, String name, String desc) {
  }

  @Override
  public void visitSource(String source, String debug) {
  }
}
