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
package com.google.gwt.dev.shell.rewrite;

import com.google.gwt.dev.asm.ClassAdapter;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.Label;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.commons.AnalyzerAdapter;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.RewriterOracle;

/**
 * This injects artificial casts to Object which will be replaced by
 * {@link RewriteJsoCasts}.
 */
class RewriteObjectComparisons extends ClassAdapter {

  private class MyMethodAdapter extends AnalyzerAdapter {
    public MyMethodAdapter(String owner, int access, String name, String desc,
        MethodVisitor mv) {
      super(owner, access, name, desc, mv);
    }

    /**
     * All object equality comparisons in the JVM are performed via a jump
     * opcode. Even something as simple as <code>boolean x = a == b;</code> is
     * implemented as
     * 
     * <pre>
     * ALOAD 1;
     * ALOAD 2;
     * IF_ACMPEQ label;
     * PUSH false;
     * GOTO: done;
     * label: PUSH true;
     * done: ASTORE 3;
     * </pre>
     */
    @Override
    public void visitJumpInsn(int opcode, Label label) {
      switch (opcode) {
        case Opcodes.IF_ACMPEQ:
        case Opcodes.IF_ACMPNE:
          Object type1 = stack.get(stack.size() - 2);
          boolean jso1 = type1 instanceof String
              && rewriterOracle.couldContainJso((String) type1);
          Object type2 = stack.get(stack.size() - 1);
          boolean jso2 = type2 instanceof String
              && rewriterOracle.couldContainJso((String) type2);

          if (jso1 || jso2) {
            if (jso2) {
              // Stack: something, something
              super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Object");
            }
            if (jso1) {
              // Stack: something, object2
              super.visitInsn(Opcodes.SWAP);
              // Stack: object2, something
              super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Object");
              // Stack: object2, object1
            }
          }
      }
      super.visitJumpInsn(opcode, label);
    }
  }

  private final RewriterOracle rewriterOracle;
  private String currentClass;

  public RewriteObjectComparisons(ClassVisitor v, RewriterOracle rewriterOracle) {
    super(v);
    this.rewriterOracle = rewriterOracle;
  }

  @Override
  public void visit(int version, int access, String name, String signature,
      String superName, String[] interfaces) {
    currentClass = name;
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
      String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature,
        exceptions);
    if (mv != null) {
      mv = new MyMethodAdapter(currentClass, access, name, desc, mv);
    }
    return mv;
  }
}
