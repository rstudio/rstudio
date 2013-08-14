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

package com.google.gwt.dev.javac.asmbridge;

import com.google.gwt.dev.asm.AnnotationVisitor;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.FieldVisitor;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Opcodes;

/**
 * This class is a replacement of the EmptyVisitor class in ASM 3.1, no longer provided in ASM 4.
 */
public class EmptyVisitor extends ClassVisitor {

  protected AnnotationVisitor av = new AnnotationVisitor(Opcodes.ASM4) {

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc) {
      return this;
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
      return this;
    }
  };

  public EmptyVisitor() {
    super(Opcodes.ASM4);
  }

  protected MethodVisitor mv = new MethodVisitor(Opcodes.ASM4) {

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
      return av;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return av;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(
        int parameter, String desc, boolean visible) {
      return av;
    }
  };

  protected FieldVisitor fv = new FieldVisitor(Opcodes.ASM4) {

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return av;
    }
  };

  @Override
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    return av;
  }

  @Override
  public FieldVisitor visitField(int access, String name, String desc, String signature,
      Object value) {
    return fv;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature,
      String[] exceptions) {
    return mv;
  }
}