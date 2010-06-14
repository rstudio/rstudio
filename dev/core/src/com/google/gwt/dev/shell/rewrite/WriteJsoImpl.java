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

import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.CANONICAL_FIELD;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.JAVASCRIPTOBJECT_DESC;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.REFERENCE_FIELD;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.REWRAP_METHOD;

import com.google.gwt.dev.asm.ClassAdapter;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.FieldVisitor;
import com.google.gwt.dev.asm.Label;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Opcodes;

/**
 * Writes the implementation classes for JSO and its subtypes.
 */
class WriteJsoImpl {

  /**
   * This type implements JavaScriptObject.
   * 
   * <ul>
   * <li>JavaScriptObject itself gets a new synthetic field to store the
   * underlying hosted mode reference.</li>
   * <li>It also receives a field to retain the canonical JavaScriptObject when
   * creating wrapper subclasses.</li>
   * <li>A rewrap method is added that simply returns the input object's
   * canonical object.</li>
   * <li>The zero-arg constructor is made public and makes the JavaScriptObject
   * its own canonical object.</li>
   * <li>A one-arg constructor is added for use by subclasses that copies the
   * hosted mode reference and canonical identity object.</li>
   * </ul>
   * 
   */
  private static class ForJso extends ClassAdapter {
    public ForJso(ClassVisitor cv) {
      super(cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature,
        String superName, String[] interfaces) {

      super.visit(version, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC
          | Opcodes.ACC_SYNTHETIC, name, signature, superName, interfaces);

      // Generate JavaScriptObject.rewrap$()
      MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC
          | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, REWRAP_METHOD, "(L"
          + JAVASCRIPTOBJECT_DESC + ";)L" + name + ";", null, null);
      if (mv != null) {
        writeRewrapMethod(mv);
      }

      /*
       * Generate the synthetic "hostedModeReferece" field to contain the
       * underlying real reference to the JavaScript object.
       */
      FieldVisitor fv = visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC
          | Opcodes.ACC_FINAL, REFERENCE_FIELD, "Ljava/lang/Object;", null,
          null);
      if (fv != null) {
        fv.visitEnd();
      }

      /*
       * Generate a synthetic "canonical" field.
       */
      fv = visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC
          | Opcodes.ACC_FINAL, CANONICAL_FIELD, "L" + JAVASCRIPTOBJECT_DESC
          + ";", null, null);
      if (fv != null) {
        fv.visitEnd();
      }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
        String signature, String[] exceptions) {
      if (isCtor(name)) {
        writeConstructors(name);
        return null;
      } else if ("equals".equals(name) && "(Ljava/lang/Object;)Z".equals(desc)) {
        writeEquals(access, name, desc, signature, exceptions);
        return null;
      }
      return super.visitMethod(access, name, desc, signature, exceptions);
    }

    /**
     * Generates a method to return the canonical object.
     * 
     * <pre>
     * public JavaScriptObject rewrap$(JavaScriptObject o) {
     *   if (o == null) {
     *     return null;
     *   }
     *   return o.canonical;
     * }
     * </pre>
     */
    protected void writeRewrapMethod(MethodVisitor mv) {
      Label start = new Label();
      Label end = new Label();

      mv.visitCode();
      mv.visitLabel(start);

      mv.visitVarInsn(Opcodes.ALOAD, 0);
      // Stack is: jso
      mv.visitInsn(Opcodes.DUP);
      // Stack is: jso, jso

      Label ret = new Label();
      mv.visitJumpInsn(Opcodes.IFNULL, ret);
      // Stack is: jso

      mv.visitFieldInsn(Opcodes.GETFIELD, JAVASCRIPTOBJECT_DESC,
          CANONICAL_FIELD, "L" + JAVASCRIPTOBJECT_DESC + ";");
      // Stack is: canonical

      mv.visitLabel(ret);
      mv.visitFrame(Opcodes.F_NEW, 1, new Object[] {JAVASCRIPTOBJECT_DESC}, 1,
          new Object[] {JAVASCRIPTOBJECT_DESC});
      mv.visitInsn(Opcodes.ARETURN);

      mv.visitMaxs(2, 1);
      mv.visitLabel(end);
      mv.visitLocalVariable("jso", "L" + JAVASCRIPTOBJECT_DESC + ";", null,
          start, end, 0);
      mv.visitEnd();
    }

    /**
     * Write JavaScriptObject's constructors.
     * 
     * <pre>
     * public JavaScriptObject(Object hostedModeReference) {
     *   this.canonical = this;
     *   this.hostedModeReference = hostedModeReference;
     * }
     * protected JavaScriptObject(JavaScriptObject jso) {
     *   this.canonical = jso.canonical;
     *   this.hostedModeReference = jso.hostedModeReference;
     * }
     * </pre>
     */
    private void writeConstructors(String name) {
      // Write the zero-arg constructor
      MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC
          | Opcodes.ACC_SYNTHETIC, name, "(Ljava/lang/Object;)V", null, null);
      if (mv != null) {
        mv.visitCode();
        // Call Object's constructor
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(Opcodes.DUP);
        // Stack: this, this
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>",
            "()V");
        // Stack: this

        // this.canonical = this;
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        // Stack: this, this, this, hostedModeReference
        mv.visitFieldInsn(Opcodes.PUTFIELD, JAVASCRIPTOBJECT_DESC,
            REFERENCE_FIELD, "Ljava/lang/Object;");
        // Stack: this, this
        mv.visitFieldInsn(Opcodes.PUTFIELD, JAVASCRIPTOBJECT_DESC,
            CANONICAL_FIELD, "L" + JAVASCRIPTOBJECT_DESC + ";");
        // Stack: <empty>

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(4, 2);
        mv.visitEnd();
      }

      // Write the protected one-arg constructor
      mv = super.visitMethod(Opcodes.ACC_PROTECTED | Opcodes.ACC_SYNTHETIC,
          name, "(L" + JAVASCRIPTOBJECT_DESC + ";)V", null, null);
      if (mv != null) {
        Label start = new Label();
        Label end = new Label();

        // Call Object's constructor
        mv.visitCode();
        mv.visitLabel(start);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        // Stack: this
        mv.visitInsn(Opcodes.DUP);
        // Stack: this, this
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>",
            "()V");
        // Stack: this

        // this.canonical = otherJso;
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        // Stack: this, otherJso
        mv.visitInsn(Opcodes.DUP2);
        // Stack: this, otherJso, this, otherJso
        mv.visitFieldInsn(Opcodes.GETFIELD, JAVASCRIPTOBJECT_DESC,
            CANONICAL_FIELD, "L" + JAVASCRIPTOBJECT_DESC + ";");
        // Stack: this, otherJso, this, canonical
        mv.visitFieldInsn(Opcodes.PUTFIELD, JAVASCRIPTOBJECT_DESC,
            CANONICAL_FIELD, "L" + JAVASCRIPTOBJECT_DESC + ";");
        // Stack: this, otherJso

        // this.hostedModeReference = otherJso.hostedModeReference
        mv.visitFieldInsn(Opcodes.GETFIELD, JAVASCRIPTOBJECT_DESC,
            REFERENCE_FIELD, "Ljava/lang/Object;");
        // Stack: this, hostedModeReference
        mv.visitFieldInsn(Opcodes.PUTFIELD, JAVASCRIPTOBJECT_DESC,
            REFERENCE_FIELD, "Ljava/lang/Object;");
        // Stack: <empty>

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(4, 2);
        mv.visitLabel(end);
        mv.visitLocalVariable("this", "L" + JAVASCRIPTOBJECT_DESC + ";", null,
            start, end, 0);
        mv.visitLocalVariable("jso", "L" + JAVASCRIPTOBJECT_DESC + ";", null,
            start, end, 1);
        mv.visitEnd();
      }
    }

    /**
     * Write the implementation of JSO.equals() to use a regular object-identity
     * comparison that can be rewritten further.
     * 
     * <pre>
     * public boolean equals(Object other) {
     *   return this == other;
     * }
     * </pre>
     */
    private void writeEquals(int access, String name, String desc,
        String signature, String[] exceptions) {
      MethodVisitor mv = super.visitMethod(access, name, desc, signature,
          exceptions);
      if (mv == null) {
        return;
      }
      mv.visitCode();

      Label returnTrue = new Label();
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitVarInsn(Opcodes.ALOAD, 1);
      // Stack: this, other

      mv.visitJumpInsn(Opcodes.IF_ACMPEQ, returnTrue);
      mv.visitInsn(Opcodes.ICONST_0);
      // Stack: 0
      mv.visitInsn(Opcodes.IRETURN);

      mv.visitLabel(returnTrue);
      mv.visitFrame(Opcodes.F_NEW, 2, new Object[] {
          JAVASCRIPTOBJECT_DESC, "java/lang/Object"}, 0, new Object[0]);
      mv.visitInsn(Opcodes.ICONST_1);
      // Stack: 1
      mv.visitInsn(Opcodes.IRETURN);

      mv.visitMaxs(2, 2);
      mv.visitEnd();
    }
  }

  /**
   * This type is used to implement subtypes of JSO.
   * 
   * <ul>
   * <li>The type's zero-arg constructor is replaced with a one-arg copy
   * constructor that delegates to the one-arg super-constructor.</li>
   * <li>A static rewrap method is added</li>
   * </ul>
   */
  private static class ForJsoSubclass extends ClassAdapter {
    private String superName;
    private String typeName;

    public ForJsoSubclass(ClassVisitor cv) {
      super(cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature,
        String superName, String[] interfaces) {
      this.superName = superName;
      this.typeName = name;
      super.visit(version, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC
          | Opcodes.ACC_SYNTHETIC, name, signature, superName, interfaces);

      // Generate JsoSubtype.rewrap$()
      MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC
          | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, REWRAP_METHOD, "(L"
          + JAVASCRIPTOBJECT_DESC + ";)L" + name + ";", null, null);
      if (mv != null) {
        writeRewrapMethod(mv);
      }
    }

    /**
     * Rewrite the JSO's constructor.
     * 
     * <pre>
     * protected SomeJso(JavaScriptObject other) {
     *   super(other);
     * }
     * </pre>
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
        String signature, String[] exceptions) {
      boolean isCtor = isCtor(name);
      if (isCtor) {
        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PROTECTED
            | Opcodes.ACC_SYNTHETIC, name,
            "(L" + JAVASCRIPTOBJECT_DESC + ";)V", null, null);
        if (mv == null) {
          return null;
        }

        Label start = new Label();
        Label end = new Label();
        mv.visitCode();
        mv.visitLabel(start);
        // super(otherJso)
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        // Stack: this
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        // Stack: this, other
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "(L"
            + JAVASCRIPTOBJECT_DESC + ";)V");
        // Stack: <empty>
        mv.visitInsn(Opcodes.RETURN);

        mv.visitMaxs(2, 2);
        mv.visitLabel(end);

        // For debugging
        mv.visitLocalVariable("this", "L" + typeName + ";", null, start, end, 0);
        mv.visitLocalVariable("jso", "L" + JAVASCRIPTOBJECT_DESC + ";", null,
            start, end, 1);
        mv.visitEnd();

        return null;
      }
      return super.visitMethod(access, name, desc, signature, exceptions);
    }

    /**
     * Constructs a type-specific rewrap method.
     * 
     * <pre>
     * public static JsoSubclass rewrap$(JavaScriptObject jso) {
     *   start:
     *   if (jso == null) {
     *     topOfStack = null;
     *     goto doReturn;
     *   }
     *   
     *   notNull: if (jso instanceof JsoSubclass) {
     *     topOfStack = (JsoSubclass) jso;
     *     goto doReturn;
     *   }
     *   
     *   notMySubclass: topOfStack = new JsoSubclass(jso);
     *   
     *   doReturn: return topOfStack;
     *   end:
     * }
     * </pre>
     */
    protected void writeRewrapMethod(MethodVisitor mv) {
      Label start = new Label();
      Label notNull = new Label();
      Label notMySubclass = new Label();
      Label doReturn = new Label();
      Label end = new Label();

      mv.visitCode();
      mv.visitLabel(start);
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitInsn(Opcodes.DUP);
      // Stack is: jso, jso

      mv.visitJumpInsn(Opcodes.IFNONNULL, notNull);
      // Stack is: jso
      // Push a null instead of using dup so that we don't need a useless cast
      mv.visitInsn(Opcodes.POP);
      mv.visitInsn(Opcodes.ACONST_NULL);
      // Stack is: null
      mv.visitJumpInsn(Opcodes.GOTO, doReturn);

      mv.visitLabel(notNull);
      mv.visitFrame(Opcodes.F_NEW, 1, new Object[] {JAVASCRIPTOBJECT_DESC}, 1,
          new Object[] {JAVASCRIPTOBJECT_DESC});
      mv.visitInsn(Opcodes.DUP);
      // Stack is: jso, jso
      mv.visitTypeInsn(Opcodes.INSTANCEOF, typeName);
      // Stack is: jso, boolean
      mv.visitJumpInsn(Opcodes.IFEQ, notMySubclass);
      // Stack is: jso
      mv.visitTypeInsn(Opcodes.CHECKCAST, typeName);
      mv.visitJumpInsn(Opcodes.GOTO, doReturn);

      mv.visitLabel(notMySubclass);
      mv.visitFrame(Opcodes.F_NEW, 1, new Object[] {JAVASCRIPTOBJECT_DESC}, 1,
          new Object[] {JAVASCRIPTOBJECT_DESC});
      // Stack is: jso

      // Allocate the new wrapper instance.
      mv.visitTypeInsn(Opcodes.NEW, typeName);
      // Stack is: jso, wrapper

      mv.visitInsn(Opcodes.DUP_X1);
      // Stack is: wrapper, jso, wrapper
      mv.visitInsn(Opcodes.SWAP);
      // Stack is: wrapper, wrapper, jso

      // Invoke the constructor, which will access the canonical object
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, typeName, "<init>", "(L"
          + JAVASCRIPTOBJECT_DESC + ";)V");
      // Stack is: wrapper

      mv.visitLabel(doReturn);
      mv.visitFrame(Opcodes.F_NEW, 1, new Object[] {JAVASCRIPTOBJECT_DESC}, 1,
          new Object[] {JAVASCRIPTOBJECT_DESC});
      // Stack is: toReturn
      mv.visitInsn(Opcodes.ARETURN);

      mv.visitLabel(end);
      mv.visitMaxs(3, 1);
      mv.visitLocalVariable("jso", "L" + JAVASCRIPTOBJECT_DESC + ";", null,
          start, end, 0);
      mv.visitEnd();
    }
  }

  /**
   * Creates a ClassVisitor to implement a JavaScriptObject subtype. This will
   * select between a simple implementation for user-defined JSO subtypes and
   * the complex implementation for implementing JavaScriptObject$.
   */
  public static ClassVisitor create(ClassVisitor cv, String classDescriptor) {
    if (classDescriptor.equals(JAVASCRIPTOBJECT_DESC)) {
      return new ForJso(cv);
    } else {
      return new ForJsoSubclass(cv);
    }
  }

  private static boolean isCtor(String name) {
    return "<init>".equals(name);
  }

  /**
   * Utility class.
   */
  private WriteJsoImpl() {
  }
}
