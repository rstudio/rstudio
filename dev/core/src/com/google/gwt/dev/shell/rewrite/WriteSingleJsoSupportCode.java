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

import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.JAVASCRIPTOBJECT_DESC;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.REWRAP_METHOD;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.SINGLE_JSO_IMPL_ADJUNCT_SUFFIX;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.SINGLE_JSO_IMPL_CAST_METHOD;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.SINGLE_JSO_IMPL_CAST_TO_OBJECT_METHOD;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.SINGLE_JSO_IMPL_FIELD;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.SINGLE_JSO_IMPL_INSTANCEOF_METHOD;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.SINGLE_JSO_IMPL_SUPPORT_CLASS;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.SYSTEM_CLASS_VERSION;

import com.google.gwt.dev.asm.ClassAdapter;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.ClassWriter;
import com.google.gwt.dev.asm.FieldVisitor;
import com.google.gwt.dev.asm.Label;
import com.google.gwt.dev.asm.MethodAdapter;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.RewriterOracle;

/**
 * Adds code to JavaScriptObject subtypes to register themselves as the
 * implementation type for a given interface. This class also contains a utility
 * method for generating an interface's adjunct type to support SingleJsoImpl
 * dispatch.
 */
class WriteSingleJsoSupportCode extends ClassAdapter {
  public static String SINGLE_JSO_IMPL_ASSIGNMENT_METHOD = "assignSingleJso$";

  private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

  /**
   * Create an adjunct class for every interface type that a JavaScriptObject
   * might implement. It will contain methods for assisting with casts and
   * instanceof checks. Given the interface <code>IFoo</code> the following type
   * will be generated:
   * 
   * <pre>
   * class IFoo$singleJsoImpl {
   * //Initialized by the JSO subtype
   * public static Class jsoImplType;
   * 
   * static {
   *   jsoImplType = SingleJsoImplSupport.getDeclaredSingleJsoImplType(IFoo.class)
   * }
   * 
   * public static Object cast(Object o) {
   *   return SingleJsoImplSupport.cast(o, IFoo.class, jsoImplType);
   * }
   *
   * public static Object castToObject$(InterfaceType o) {
   *   if (o instanceof JavaScriptObject) {
   *     o = JavaScriptObject.rewrap$((JavaScriptObject o));
   *   }
   *   return o;
   * }
   * public static boolean instanceOf(Object o) {
   *   return SingleJsoImplSupport.instanceOf(o, IFoo.class, jsoImplType);
   * }
   * }
   * </pre>
   */
  static byte[] writeSingleJsoImplAdjunct(String className) {
    assert className.endsWith(SINGLE_JSO_IMPL_ADJUNCT_SUFFIX) : "Bad className "
        + className;
    String internalName = toInternalName(className);
    String intfName = internalName.substring(0, internalName.length()
        - SINGLE_JSO_IMPL_ADJUNCT_SUFFIX.length());

    ClassWriter writer = new ClassWriter(0);

    writer.visit(SYSTEM_CLASS_VERSION, Opcodes.ACC_PUBLIC
        | Opcodes.ACC_SYNTHETIC, internalName, null, "java/lang/Object", null);

    // Create jsoImplType field
    FieldVisitor fv = writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC
        | Opcodes.ACC_SYNTHETIC, SINGLE_JSO_IMPL_FIELD, "Ljava/lang/Class;",
        null, null);
    if (fv != null) {
      fv.visitEnd();
    }

    // Write static initializer
    MethodVisitor mv = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>",
        "()V", null, null);
    if (mv != null) {
      mv.visitCode();
      mv.visitLdcInsn(Type.getObjectType(intfName));
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, SINGLE_JSO_IMPL_SUPPORT_CLASS,
          "getDeclaredSingleJsoImplType",
          "(Ljava/lang/Class;)Ljava/lang/Class;");
      mv.visitFieldInsn(Opcodes.PUTSTATIC, internalName, SINGLE_JSO_IMPL_FIELD,
          "Ljava/lang/Class;");
      mv.visitInsn(Opcodes.RETURN);
      mv.visitMaxs(1, 0);
      mv.visitEnd();
    }

    // Write cast method
    mv = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC
        | Opcodes.ACC_SYNTHETIC, SINGLE_JSO_IMPL_CAST_METHOD,
        "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
    if (mv != null) {
      Label start = new Label();
      Label end = new Label();

      mv.visitCode();
      mv.visitLabel(start);
      // Stack is: empty
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      // Stack is: object
      mv.visitLdcInsn(Type.getType("L" + intfName + ";"));
      // Stack is: object, interfaceType
      mv.visitFieldInsn(Opcodes.GETSTATIC, internalName, SINGLE_JSO_IMPL_FIELD,
          "Ljava/lang/Class;");
      // Stack is: object, interfaceType, jsoType (may be null)

      mv.visitMethodInsn(Opcodes.INVOKESTATIC, SINGLE_JSO_IMPL_SUPPORT_CLASS,
          "cast",
          "(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/Object;");
      // Stack is: object (maybe JSO wrapper)

      mv.visitInsn(Opcodes.ARETURN);
      mv.visitLabel(end);
      mv.visitMaxs(3, 1);
      mv.visitLocalVariable("obj", "Ljava/lang/Object;", null, start, end, 0);
      mv.visitEnd();
    }

    // Write castToObject method
    mv = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC
        | Opcodes.ACC_SYNTHETIC, SINGLE_JSO_IMPL_CAST_TO_OBJECT_METHOD, "(L"
        + intfName + ";)Ljava/lang/Object;", null, null);
    if (mv != null) {
      Label start = new Label();
      Label beforeReturn = new Label();
      Label end = new Label();

      mv.visitCode();
      mv.visitLabel(start);
      // Stack is: empty
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      // Stack is: object
      mv.visitTypeInsn(Opcodes.INSTANCEOF, JAVASCRIPTOBJECT_DESC);
      // Stack is: int
      mv.visitJumpInsn(Opcodes.IFEQ, beforeReturn);
      // Stack is: empty

      mv.visitVarInsn(Opcodes.ALOAD, 0);
      // Stack is: object
      mv.visitTypeInsn(Opcodes.CHECKCAST, JAVASCRIPTOBJECT_DESC);
      // Stack is: JSO
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, JAVASCRIPTOBJECT_DESC,
          REWRAP_METHOD, "(L" + JAVASCRIPTOBJECT_DESC + ";)L"
              + JAVASCRIPTOBJECT_DESC + ";");
      // Stack is: canonical JSO
      mv.visitVarInsn(Opcodes.ASTORE, 0);
      // Stack is: empty (local 0 contains canonical object)

      mv.visitLabel(beforeReturn);
      mv.visitFrame(Opcodes.F_NEW, 1, new Object[] {"java/lang/Object"}, 0,
          EMPTY_OBJECT_ARRAY);
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitInsn(Opcodes.ARETURN);
      mv.visitLabel(end);
      mv.visitMaxs(1, 1);
      mv.visitLocalVariable("obj", "Ljava/lang/Object;", null, start, end, 0);
      mv.visitEnd();
    }

    // Write instanceOf method
    mv = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC
        | Opcodes.ACC_SYNTHETIC, SINGLE_JSO_IMPL_INSTANCEOF_METHOD,
        "(Ljava/lang/Object;)Z", null, null);
    if (mv != null) {
      Label start = new Label();
      Label end = new Label();

      mv.visitCode();
      mv.visitLabel(start);
      // Stack is: empty
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      // Stack is: object
      mv.visitLdcInsn(Type.getType("L" + intfName + ";"));
      // Stack is: object, interfaceType
      mv.visitFieldInsn(Opcodes.GETSTATIC, internalName, SINGLE_JSO_IMPL_FIELD,
          "Ljava/lang/Class;");
      // Stack is: object, interfaceType, jsoType (may be null)

      mv.visitMethodInsn(Opcodes.INVOKESTATIC, SINGLE_JSO_IMPL_SUPPORT_CLASS,
          "instanceOf",
          "(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/Class;)Z");
      // Stack is: boolean

      mv.visitInsn(Opcodes.IRETURN);
      mv.visitLabel(end);
      mv.visitMaxs(3, 1);
      mv.visitLocalVariable("obj", "Ljava/lang/Object;", null, start, end, 0);
      mv.visitEnd();
    }

    writer.visitEnd();
    return writer.toByteArray();
  }

  private static String toInternalName(String jsoSubtype) {
    return jsoSubtype.replace('.', '/');
  }

  private String className;
  private boolean hasClinit;
  private String[] interfaces;
  private final RewriterOracle oracle;

  public WriteSingleJsoSupportCode(ClassVisitor cv, RewriterOracle oracle) {
    super(cv);
    this.oracle = oracle;
  }

  @Override
  public void visit(int version, int access, String name, String signature,
      String superName, String[] interfaces) {
    className = name;
    if (oracle.isJsoOrSubtype(name)) {
      this.interfaces = oracle.getAllSuperInterfaces(interfaces);
    } else {
      this.interfaces = null;
    }
    super.visit(version, access, name, signature, superName, interfaces);
  }

  /**
   * Write the interface assignment code and possibly introduce a static
   * initializer.
   */
  @Override
  public void visitEnd() {
    if (interfaces != null) {
      writeSingleJsoImplAssignments();

      if (!hasClinit) {
        MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>",
            "()V", null, null);
        if (mv != null) {
          mv.visitCode();
          mv.visitFrame(Opcodes.F_NEW, 0, EMPTY_OBJECT_ARRAY, 0,
              EMPTY_OBJECT_ARRAY);
          mv.visitMethodInsn(Opcodes.INVOKESTATIC, className,
              SINGLE_JSO_IMPL_ASSIGNMENT_METHOD, "()V");
          mv.visitInsn(Opcodes.RETURN);
          mv.visitEnd();
        }
      }
    }

    super.visitEnd();
  }

  /**
   * Possibly update the existing static initializer to call the assignment
   * code.
   */
  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
      String signature, String[] exceptions) {
    if (interfaces != null && "<clinit>".equals(name)) {
      // Disable code in visitEnd()
      hasClinit = true;
      MethodVisitor mv = super.visitMethod(access, name, desc, signature,
          exceptions);

      if (mv == null) {
        return null;
      }

      return new MethodAdapter(mv) {
        /**
         * Write the call to the assignment method as the first code in the
         * static initializer.
         */
        @Override
        public void visitCode() {
          super.visitCode();
          mv.visitMethodInsn(Opcodes.INVOKESTATIC, className,
              SINGLE_JSO_IMPL_ASSIGNMENT_METHOD, "()V");
        }
      };
    }
    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  /**
   * Generate code to register the JSO subtype as the implementation type for
   * its interfaces. For every interface (and super-interface) implemented by
   * the JSO type, we'll register the JSO type in the interfaces' adjunct types.
   * <p>
   * For tag interfaces:
   * 
   * <pre>
   * if (IFoo$singleJsoImpl.singleJsoImpl$ == null) {
   *   IFoo$singleJsoImpl.singleJsoImpl$ = JsoFoo.class;
   * }
   * </pre>
   * </p>
   * <p>
   * For non-trivial interfaces, we check to see if any existing type is a
   * supertype of this JSO:
   * 
   * <pre>
   * if (IFoo$singleJsoImpl.singleJsoImpl$ == null) {
   *   IFoo$singleJsoImpl.singleJsoImpl$ = JsoFoo.class;
   * } else if (!IFoo$singleJsoImpl.singleJsoImpl$.isAssignableFrom(JsoFoo.class) {
   *   throw new RuntimeException();
   * }
   * </pre>
   * </p>
   */
  private void writeSingleJsoImplAssignments() {
    MethodVisitor mv = super.visitMethod(Opcodes.ACC_PRIVATE
        | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
        SINGLE_JSO_IMPL_ASSIGNMENT_METHOD, "()V", null, null);
    if (mv == null) {
      return;
    }

    int stack = 0;
    mv.visitCode();
    mv.visitFrame(Opcodes.F_NEW, 0, EMPTY_OBJECT_ARRAY, 0, EMPTY_OBJECT_ARRAY);
    for (String intf : interfaces) {

      mv.visitFieldInsn(Opcodes.GETSTATIC, intf
          + SINGLE_JSO_IMPL_ADJUNCT_SUFFIX, SINGLE_JSO_IMPL_FIELD,
          "Ljava/lang/Class;");
      // Stack is: classLit (may be null)

      if (oracle.isTagInterface(intf)) {
        Label noActionNeeded = new Label();
        /*
         * Multiple JSO types may implement tag interfaces, so we'll ignore any
         * existing type.
         */
        mv.visitJumpInsn(Opcodes.IFNONNULL, noActionNeeded);
        mv.visitLdcInsn(Type.getObjectType(className));
        mv.visitFieldInsn(Opcodes.PUTSTATIC, intf
            + SINGLE_JSO_IMPL_ADJUNCT_SUFFIX, SINGLE_JSO_IMPL_FIELD,
            "Ljava/lang/Class;");
        mv.visitLabel(noActionNeeded);
        mv.visitFrame(Opcodes.F_NEW, 0, EMPTY_OBJECT_ARRAY, 0,
            EMPTY_OBJECT_ARRAY);
        stack = Math.max(stack, 1);
      } else {
        /*
         * Otherwise, throw an exception if the existing JSO implementation is
         * not a supertype of the current class.
         */
        Label noPreviousClass = new Label();
        Label noActionNeeded = new Label();
        mv.visitJumpInsn(Opcodes.IFNULL, noPreviousClass);
        // Stack is: empty

        // Ensure the existing type is one of my supertypes
        mv.visitFieldInsn(Opcodes.GETSTATIC, intf
            + SINGLE_JSO_IMPL_ADJUNCT_SUFFIX, SINGLE_JSO_IMPL_FIELD,
            "Ljava/lang/Class;");
        mv.visitLdcInsn(Type.getObjectType(className));
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class",
            "isAssignableFrom", "(Ljava/lang/Class;)Z");
        mv.visitJumpInsn(Opcodes.IFNE, noActionNeeded);
        // Stack is: empty

        mv.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException");
        // Stack is: uninitialized
        mv.visitInsn(Opcodes.DUP);
        // Stack is: uninitialized, uninitialized
        mv.visitLdcInsn("Multiple JavaScriptObject subclasses implement an "
            + "interface declared on this type");
        // Stack is: uninitialized, uninitialized, string
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException",
            "<init>", "(Ljava/lang/String;)V");
        // Stack is: RuntimeException
        mv.visitInsn(Opcodes.ATHROW);

        mv.visitLabel(noPreviousClass);
        // Stack is: empty
        mv.visitFrame(Opcodes.F_NEW, 0, EMPTY_OBJECT_ARRAY, 0,
            EMPTY_OBJECT_ARRAY);
        mv.visitLdcInsn(Type.getObjectType(className));
        // Stack is: class literal
        mv.visitFieldInsn(Opcodes.PUTSTATIC, intf
            + SINGLE_JSO_IMPL_ADJUNCT_SUFFIX, SINGLE_JSO_IMPL_FIELD,
            "Ljava/lang/Class;");
        // Stack is: empty

        mv.visitLabel(noActionNeeded);
        // Stack is: empty
        mv.visitFrame(Opcodes.F_NEW, 0, EMPTY_OBJECT_ARRAY, 0,
            EMPTY_OBJECT_ARRAY);

        stack = Math.max(stack, 3);
      }
    }
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(stack, 0);
    mv.visitEnd();
  }
}
