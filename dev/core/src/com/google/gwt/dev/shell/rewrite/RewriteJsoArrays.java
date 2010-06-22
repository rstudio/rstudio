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

import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.DISAMBIGUATOR_TYPE_INTERNAL_NAME;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.SYSTEM_CLASS_VERSION;

import com.google.gwt.dev.asm.AnnotationVisitor;
import com.google.gwt.dev.asm.ClassAdapter;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.ClassWriter;
import com.google.gwt.dev.asm.FieldVisitor;
import com.google.gwt.dev.asm.Label;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.asm.commons.Method;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.RewriterOracle;
import com.google.gwt.dev.util.Name.InternalName;

import java.util.Arrays;

/**
 * The object of this pass is to turn all JSO subtype arrays into
 * JavaScriptObject arrays. This allows JSO arrays to participate in arbitrary
 * cross-casting.
 */
public class RewriteJsoArrays extends ClassAdapter {

  private static final String ORIGINAL_JSNI_SIGNATURE_DESC = Type.getDescriptor(OriginalJsniSignature.class);

  /**
   * Performs upcasts on JSO subtype arrays.
   */
  private class UpcastAdapter extends DebugAnalyzerAdapter {

    private final int minLocals;
    private int minStack;

    public UpcastAdapter(String owner, int access, String name, String desc,
        MethodVisitor mv, int minLocals) {
      super(owner, access, name, desc, mv);
      this.minLocals = minLocals;
    }

    /**
     * Upcast field access.
     */
    @Override
    public void visitFieldInsn(int opcode, String owner, String name,
        String desc) {
      Type t = Type.getType(desc);
      if (t.getSort() == Type.ARRAY) {
        t = upcastJsoType(t);
        if (t != null) {
          desc = t.getDescriptor();
        }
      }
      super.visitFieldInsn(opcode, owner, name, desc);
    }

    /**
     * Fix JSO array types in framing data to keep the verifier and other
     * AnalyzerAdapters synced up.
     */
    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack,
        Object[] stack) {
      fixFrameData(local);
      fixFrameData(stack);
      super.visitFrame(type, nLocal, local, nStack, stack);
    }

    /**
     * Keep the debugger from going off the rails.
     */
    @Override
    public void visitLocalVariable(String name, String desc, String signature,
        Label start, Label end, int index) {
      Type t = Type.getType(desc);
      if (t.getSort() == Type.ARRAY) {
        t = upcastJsoType(t);
        if (t != null) {
          desc = t.getInternalName();
        }
      }
      super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    /**
     * We need to override the number of locals when rewriting a constructor to
     * account for synthetic disambiguation parameters. The null argument to the
     * disambiguated constructor might require adjustment of the stack size.
     */
    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
      super.visitMaxs(Math.max(minStack, maxStack), Math.max(minLocals,
          maxLocals));
    }

    /**
     * Fix the descriptors used by method invocations to account for the upcast
     * array types. Initializers are handled specially, since we can't simply
     * change their name to provide disambiguation. Instead, we add an
     * additional null argument of a synthetic type.
     */
    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
        String desc) {
      switch (opcode) {
        case Opcodes.INVOKESPECIAL: {
          if ("<init>".equals(name)) {
            String[] disambiguator = rewriterOracle.getArrayDisambiguator(desc);
            // Widen any JSO arrays.
            Method m = upcastMethod(name, desc);
            desc = m == null ? desc : m.getDescriptor();
            if (disambiguator.length != 0) {
              desc = addDisambiguator(desc, disambiguator);
              // Add bogus values to the stack
              recordDebugData("Constructor disambiguation "
                  + Arrays.asList(disambiguator));
              for (int i = 0, j = disambiguator.length; i < j; i++) {
                super.visitInsn(Opcodes.ACONST_NULL);
              }
              minStack = Math.max(minStack, stack.size());
            }
            break;
          }
          // Intentional fallthrough for non-init methods
        }
        case Opcodes.INVOKEINTERFACE:
        case Opcodes.INVOKESTATIC:
        case Opcodes.INVOKEVIRTUAL: {
          Method m = upcastMethod(name, desc);
          if (m != null) {
            recordDebugData("Replaced method call " + name + " " + desc
                + " with " + m.toString());
            name = m.getName();
            desc = m.getDescriptor();
          }
          break;
        }
        default:
          throw new RuntimeException("Unhandled method instruction " + opcode);
      }
      super.visitMethodInsn(opcode, owner, name, desc);
    }

    /**
     * Fix the types of multidimensional arrays allocations.
     */
    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
      Type replacement = upcastJsoType(Type.getObjectType(desc));
      if (replacement != null) {
        recordDebugData("Widened JSO array allocation");
        desc = replacement.getInternalName();
      }
      super.visitMultiANewArrayInsn(desc, dims);
    }

    /**
     * Convert all JSO subtype array allocations into an allocation of a
     * JavaScriptObject array.
     */
    @Override
    public void visitTypeInsn(int opcode, String internalName) {
      if (opcode == Opcodes.ANEWARRAY) {
        Type replacement = upcastJsoType(Type.getObjectType(internalName));
        if (replacement != null) {
          recordDebugData("Widened JSO array allocation");
          internalName = replacement.getInternalName();
        }
      }
      super.visitTypeInsn(opcode, internalName);
    }

    /**
     * Utility method used by {@link #visitFrame} to process the stack and local
     * arrays.
     */
    private void fixFrameData(Object[] data) {
      for (int i = 0, j = data.length; i < j; i++) {
        if (data[i] instanceof String) {
          Type t = Type.getObjectType((String) data[i]);
          if (t.getSort() == Type.ARRAY) {
            t = upcastJsoType(t);
            if (t != null) {
              data[i] = t.getInternalName();
            }
          }
        }
      }
    }
  }

  /**
   * Creates an empty interface type.
   */
  static byte[] writeConstructorDisambiguationType(String className) {
    String internalName = className.replace('.', '/');
    assert internalName.startsWith(DISAMBIGUATOR_TYPE_INTERNAL_NAME) : "Bad className "
        + className;

    ClassWriter writer = new ClassWriter(0);

    writer.visit(SYSTEM_CLASS_VERSION, Opcodes.ACC_PUBLIC
        | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE, internalName, null,
        "java/lang/Object", null);

    writer.visitEnd();
    return writer.toByteArray();
  }

  /**
   * Records the class currently being processed.
   */
  private String owner;
  private final RewriterOracle rewriterOracle;

  public RewriteJsoArrays(ClassVisitor v, RewriterOracle rewriterOracle) {
    super(v);
    this.rewriterOracle = rewriterOracle;
  }

  @Override
  public void visit(int version, int access, String name, String signature,
      String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    owner = name;
  }

  /**
   * Widen all fields of a JSO array type to JSO[].
   */
  @Override
  public FieldVisitor visitField(int access, String name, String desc,
      String signature, Object value) {
    Type t = Type.getType(desc);
    if (t.getSort() == Type.ARRAY) {
      Type newType = upcastJsoType(t);
      if (newType != null) {
        desc = newType.getDescriptor();
      }
    }
    return super.visitField(access, name, desc, signature, value);
  }

  /**
   * Widen all JSO array paramaters to JSO[]. Constructors may have a
   * disambiguator type added to their parameter list.
   */
  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
      String signature, String[] exceptions) {
    int minLocals = 0;
    String originalJsniName = name;
    String originalJsniDesc = desc;
    Method upcast = upcastMethod(name, desc);

    if (upcast != null) {
      if ("<init>".equals(name)) {
        String[] disambiguator = rewriterOracle.getArrayDisambiguator(desc);
        desc = upcast.getDescriptor();
        if (disambiguator.length != 0) {
          desc = addDisambiguator(desc, disambiguator);
          // +1 for this
          minLocals = Type.getArgumentTypes(desc).length + 1;
        }
        originalJsniName = "new";
      } else {
        name = upcast.getName();
        desc = upcast.getDescriptor();
      }
    }

    MethodVisitor mv = super.visitMethod(access, name, desc, signature,
        exceptions);
    if (mv != null) {
      // Record the original JSNI signature if we've mangled the method
      if (upcast != null) {
        AnnotationVisitor av = mv.visitAnnotation(ORIGINAL_JSNI_SIGNATURE_DESC,
            true);
        if (av != null) {
          av.visit("name", originalJsniName);
          // Strip the return type
          av.visit("paramList", originalJsniDesc.substring(0,
              originalJsniDesc.indexOf(')') + 1));
          av.visitEnd();
        }
      }
      mv = new UpcastAdapter(owner, access, name, desc, mv, minLocals);
    }
    return mv;
  }

  /**
   * Add disambiguator types as the last parameters of a method descriptor.
   */
  private String addDisambiguator(String desc, String[] disambiguator) {
    int idx = desc.indexOf(')');
    StringBuilder sb = new StringBuilder();
    sb.append(desc.substring(0, idx));
    for (String d : disambiguator) {
      sb.append("L").append(d).append(";");
    }
    sb.append(desc.substring(idx));
    return sb.toString();
  }

  /**
   * Utility method for constructing a descriptor. If the given type is a JSO
   * subtype array, the appropriate JavaScriptObject array type will be appended
   * to the descriptor and this method will return <code>true</code>. Otherwise,
   * this method will simply append the type to the descriptor and return
   * <code>false</code>.
   */
  private boolean appendTypeMaybeUpcast(StringBuilder newDesc, Type type) {
    if (type.getSort() == Type.ARRAY) {
      Type newType = upcastJsoType(type);
      if (newType != null) {
        newDesc.append(newType.getDescriptor());
        return true;
      }
    }
    newDesc.append(type.getDescriptor());
    return false;
  }

  /**
   * Calls {@link RewriteJsoCasts#upcastJsoType} with the instance's
   * {@link #rewriterOracle}.
   */
  private Type upcastJsoType(Type type) {
    return RewriteJsoCasts.upcastJsoType(rewriterOracle, type);
  }

  /**
   * Determine if a descriptor contains references to JSO subtype arrays. If so,
   * returns an upcast descriptor and a guaranteed-unique name for the method.
   * Otherwise, this method returns <code>null</code>.
   */
  private Method upcastMethod(String name, String desc) {
    boolean didChange = false;
    StringBuilder newName = new StringBuilder(name);
    StringBuilder newDesc = new StringBuilder("(");

    for (Type arg : Type.getArgumentTypes(desc)) {
      // Add the arguments, one at a time, to the new descriptor
      if (appendTypeMaybeUpcast(newDesc, arg)) {
        didChange = true;
        /*
         * Add the original type to the method name. We don't need to worry
         * about the number of parameters or their relative positions when
         * constructing the name, because that information is still in the
         * method descriptor.
         */
        newName.append("_").append(
            InternalName.toIdentifier(arg.getElementType().getInternalName())).append(
            "_").append(arg.getDimensions());
      }
    }
    newDesc.append(")");

    Type returnType = Type.getReturnType(desc);
    if (appendTypeMaybeUpcast(newDesc, returnType)) {
      didChange = true;
    }

    if (!didChange) {
      return null;
    }
    return new Method(newName.toString(), newDesc.toString());
  }
}
