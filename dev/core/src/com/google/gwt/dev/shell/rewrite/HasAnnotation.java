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
package com.google.gwt.dev.shell.rewrite;

import com.google.gwt.dev.asm.AnnotationVisitor;
import com.google.gwt.dev.asm.ClassAdapter;
import com.google.gwt.dev.asm.ClassReader;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.asm.commons.EmptyVisitor;

import java.lang.annotation.Annotation;

/**
 * A simple ClassAdapter that determines if a specific annotation is declared on
 * a type (ignoring any annotatons that may be present on supertypes or
 * superinterfaces).
 */
public class HasAnnotation extends ClassAdapter {
  /**
   * A utility method to determine if the class defined in
   * <code>classBytes</code> has a particular annotation.
   * 
   * @param classBytes the class's bytecode
   * @param annotation the type of annotation to look for
   * @return <code>true</code> if the class defined in <code>classBytes</code>
   *         possesses the desired annotation
   */
  public static boolean hasAnnotation(byte[] classBytes,
      Class<? extends Annotation> annotation) {
    HasAnnotation v = new HasAnnotation(new EmptyVisitor(), annotation);
    new ClassReader(classBytes).accept(v, ClassReader.SKIP_CODE
        | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

    return v.isFound();
  }

  private boolean found;

  private final String targetDesc;

  public HasAnnotation(ClassVisitor v, Class<? extends Annotation> annotation) {
    super(v);
    targetDesc = Type.getDescriptor(annotation);
  }

  public boolean isFound() {
    return found;
  }

  @Override
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    if (targetDesc.equals(desc)) {
      found = true;
    }
    return super.visitAnnotation(desc, visible);
  }
}