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

import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.commons.Remapper;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.InstanceMethodOracle;

import java.util.Set;

/**
 * Rewrites references to modified JSO subtypes.
 *
 * <ol>
 * <li>Changes the owner type for instructions that reference items in a JSO
 * class to the implementation class.</li>
 * <li>Rewrites instance calls to JSO classes into static calls.</li>
 * <li>Updates the descriptor for such call sites to includes a synthetic
 * <code>this</code> parameter. This modified method has same stack behavior
 * as the original instance method.</li>
 * </ol>
 */
class RewriteRefsToJsoClasses extends ClassVisitor {

  /**
   * A method body rewriter to actually rewrite call sites.
   */
  private class MyMethodAdapter extends MethodVisitor {

    private Remapper remapper = new Remapper() {
      @Override
      public String map(String typeName) {
        if (jsoDescriptors.contains(typeName)) {
          return HostedModeClassRewriter.JAVASCRIPTOBJECT_IMPL_DESC;
        }
        return typeName;
      }
    };

    public MyMethodAdapter(MethodVisitor mv) {
      super(Opcodes.ASM4, mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name,
        String desc) {
      if (jsoDescriptors.contains(owner)) {
        // Change the owner to the rewritten class.
        owner += "$";
      }
      super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitLdcInsn(Object cst) {
      cst = remapper.mapValue(cst);
      super.visitLdcInsn(cst);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
        String desc) {
      if (jsoDescriptors.contains(owner)) {
        // Find the class that actually declared the method.
        if (opcode == Opcodes.INVOKEVIRTUAL) {
          owner = mapper.findOriginalDeclaringClass(owner, name + desc);
        }
        if (!owner.equals("java/lang/Object")) {
          if (opcode == Opcodes.INVOKEVIRTUAL
              || opcode == Opcodes.INVOKESPECIAL) {
            // Instance/super call to JSO; rewrite as static.
            opcode = Opcodes.INVOKESTATIC;
            desc = HostedModeClassRewriter.addSyntheticThisParam(owner, desc);
            name += "$";
          }
          // Change the owner to implementation class.
          owner += "$";
        }
      }
      super.visitMethodInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
      desc = remapper.mapType(desc);
      super.visitMultiANewArrayInsn(desc, dims);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
      if (opcode == Opcodes.ANEWARRAY) {
        type = remapper.mapType(type);
      }
      super.visitTypeInsn(opcode, type);
    }
  }

  /**
   * An unmodifiable set of descriptors containing <code>JavaScriptObject</code>
   * and all subclasses.
   */
  protected final Set<String> jsoDescriptors;

  /**
   * Maps methods to the class in which they are declared.
   */
  private InstanceMethodOracle mapper;

  /**
   * Construct a new rewriter instance.
   *
   * @param cv the visitor to chain to
   * @param jsoDescriptors an unmodifiable set of descriptors containing
   *          <code>JavaScriptObject</code> and all subclasses
   * @param mapper maps methods to the class in which they are declared
   */
  public RewriteRefsToJsoClasses(ClassVisitor cv, Set<String> jsoDescriptors,
      InstanceMethodOracle mapper) {
    super(Opcodes.ASM4, cv);
    this.jsoDescriptors = jsoDescriptors;
    this.mapper = mapper;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
      String signature, String[] exceptions) {
    // Wrap the returned method visitor in my own.
    MethodVisitor mv = super.visitMethod(access, name, desc, signature,
        exceptions);
    return new MyMethodAdapter(mv);
  }

}
