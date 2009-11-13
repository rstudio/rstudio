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

import com.google.gwt.dev.asm.ClassAdapter;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.asm.commons.GeneratorAdapter;
import com.google.gwt.dev.asm.commons.Method;
import com.google.gwt.dev.shell.JavaScriptHost;
import com.google.gwt.dev.util.Name.InternalName;

import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;

/**
 * Turns native method declarations into normal Java functions which perform the
 * corresponding JSNI dispatch.
 */
public class RewriteJsniMethods extends ClassAdapter {

  /**
   * Fast way to look up boxing methods.
   */
  private static class Boxing {

    /**
     * A matching list of boxed types for each primitive type.
     */
    private static final Type[] BOXED_TYPES = new Type[] {
        VOID_TYPE, BOOLEAN_TYPE, CHARACTER_TYPE, BYTE_TYPE, SHORT_TYPE,
        INTEGER_TYPE, FLOAT_TYPE, LONG_TYPE, DOUBLE_TYPE,};

    /**
     * The list of primitive types.
     */
    private static final Type[] PRIMITIVE_TYPES = new Type[] {
        Type.VOID_TYPE, Type.BOOLEAN_TYPE, Type.CHAR_TYPE, Type.BYTE_TYPE,
        Type.SHORT_TYPE, Type.INT_TYPE, Type.FLOAT_TYPE, Type.LONG_TYPE,
        Type.DOUBLE_TYPE,};

    /**
     * A map of Type.sort() to valueOf Method. There are 11 possible results
     * from Type.sort(), 0 through 10.
     */
    private static final Method[] SORT_MAP = new Method[11];

    static {
      assert PRIMITIVE_TYPES.length == BOXED_TYPES.length;
      for (int i = 0; i < PRIMITIVE_TYPES.length; ++i) {
        Type primitive = PRIMITIVE_TYPES[i];
        Type boxed = BOXED_TYPES[i];
        if (boxed != null) {
          SORT_MAP[i] = new Method("valueOf", boxed, new Type[] {primitive});
        }
      }
    }

    public static Method getBoxMethod(Type type) {
      int sortType = type.getSort();
      assert (sortType >= 0 && sortType < SORT_MAP.length) : "Unexpected JavaScriptHostInfo.get index - "
          + sortType;
      return SORT_MAP[sortType];
    }
  }

  /**
   * Oracle for info regarding {@link JavaScriptHost}.
   */
  private static class JavaScriptHostInfo {

    /**
     * The {@link JavaScriptHost} type.
     */
    public static final Type TYPE = Type.getType(JavaScriptHost.class);

    /**
     * The parameter signature of {@link JavaScriptHost}'s invoke* methods, in
     * classes.
     */
    private static final Class<?>[] INVOKE_NATIVE_PARAM_CLASSES = new Class[] {
        String.class, Object.class, Class[].class, Object[].class,};

    /**
     * The parameter signature of {@link JavaScriptHost}'s invoke* methods, in
     * types.
     */
    private static final Type[] INVOKE_NATIVE_PARAM_TYPES;

    /**
     * A map of Type.sort() to JavaScriptHostInfo.
     */
    private static final JavaScriptHostInfo[] SORT_MAP = new JavaScriptHostInfo[11];

    static {
      INVOKE_NATIVE_PARAM_TYPES = new Type[INVOKE_NATIVE_PARAM_CLASSES.length];
      for (int i = 0; i < INVOKE_NATIVE_PARAM_TYPES.length; ++i) {
        INVOKE_NATIVE_PARAM_TYPES[i] = Type.getType(INVOKE_NATIVE_PARAM_CLASSES[i]);
      }

      Class<?>[] primitives = {
          void.class, boolean.class, byte.class, char.class, short.class,
          int.class, long.class, float.class, double.class};
      for (Class<?> c : primitives) {
        Type type = Type.getType(c);
        String typeName = type.getClassName();
        String firstChar = typeName.substring(0, 1).toUpperCase(Locale.ENGLISH);
        typeName = firstChar + typeName.substring(1);
        SORT_MAP[type.getSort()] = new JavaScriptHostInfo(type, "invokeNative"
            + typeName);
      }
      JavaScriptHostInfo objectType = new JavaScriptHostInfo(
          Type.getType(Object.class), "invokeNativeObject", true);
      SORT_MAP[Type.ARRAY] = objectType;
      SORT_MAP[Type.OBJECT] = objectType;
      assert noNulls(SORT_MAP) : "Did not fully fill in JavaScriptHostInfo.SORT_MAP";
    }

    public static JavaScriptHostInfo get(int sortType) {
      assert (sortType >= 0 && sortType < SORT_MAP.length) : "Unexpected JavaScriptHostInfo.get index - "
          + sortType;
      return SORT_MAP[sortType];
    }

    /**
     * Validate our model against the real JavaScriptHost class.
     */
    private static boolean matchesRealMethod(String methodName, Type returnType) {
      try {
        java.lang.reflect.Method method = JavaScriptHost.class.getDeclaredMethod(
            methodName, INVOKE_NATIVE_PARAM_CLASSES);
        assert (method.getModifiers() & Modifier.STATIC) != 0 : "Was expecting method '"
            + method + "' to be static";
        Type realReturnType = Type.getType(method.getReturnType());
        return realReturnType.getDescriptor().equals(returnType.getDescriptor());
      } catch (SecurityException e) {
      } catch (NoSuchMethodException e) {
      }
      return false;
    }

    private static boolean noNulls(Object[] array) {
      for (Object element : array) {
        if (element == null) {
          return false;
        }
      }
      return true;
    }

    private final Method method;

    private final boolean requiresCast;

    private JavaScriptHostInfo(Type returnType, String methodName) {
      this(returnType, methodName, false);
    }

    private JavaScriptHostInfo(Type returnType, String methodName,
        boolean requiresCast) {
      this.requiresCast = requiresCast;
      this.method = new Method(methodName, returnType,
          INVOKE_NATIVE_PARAM_TYPES);
      assert matchesRealMethod(methodName, returnType) : "JavaScriptHostInfo for '"
          + this + "' does not match real method";
    }

    public Method getMethod() {
      return method;
    }

    public boolean requiresCast() {
      return requiresCast;
    }

    @Override
    public String toString() {
      return method.toString();
    }
  }

  /**
   * Rewrites native Java methods to dispatch as JSNI.
   */
  private class MyMethodAdapter extends GeneratorAdapter {

    private String descriptor;
    private boolean isStatic;
    private String name;

    public MyMethodAdapter(MethodVisitor mv, int access, String name,
        String desc) {
      super(mv, access, name, desc);
      this.descriptor = desc;
      this.name = name;
      isStatic = (access & Opcodes.ACC_STATIC) != 0;
    }

    /**
     * Replacement for {@link GeneratorAdapter#box(Type)}, which always calls,
     * for example, {@code new Boolean} instead of using
     * {@link Boolean#valueOf(boolean)}.
     */
    @Override
    public void box(Type type) {
      Method method = Boxing.getBoxMethod(type);
      if (method != null) {
        invokeStatic(method.getReturnType(), method);
      }
    }

    /**
     * Does all of the work necessary to do the dispatch to the appropriate
     * variant of {@link JavaScriptHost#invokeNativeVoid
     * JavaScriptHost.invokeNative*}. And example output:
     * 
     * <pre>
     * return JavaScriptHost.invokeNativeInt(
     *     "@com.google.gwt.sample.hello.client.Hello::echo(I)", null,
     *     new Class[] {int.class,}, new Object[] {x,});
     * </pre>
     */
    @Override
    public void visitCode() {
      super.visitCode();

      /*
       * If you modify the generated code, you must recompute the stack size in
       * visitEnd().
       */

      // First argument - JSNI signature
      String jsniTarget = getJsniSignature(name, descriptor);
      visitLdcInsn(jsniTarget);
      // Stack is at 1

      // Second argument - target; "null" if static, otherwise "this".
      if (isStatic) {
        visitInsn(Opcodes.ACONST_NULL);
      } else {
        loadThis();
      }
      // Stack is at 2

      // Third argument - a Class[] describing the types of this method
      loadClassArray();
      // Stack is at 3; reaches 6 internally

      // Fourth argument - all the arguments boxed into an Object[]
      loadArgArray();
      // Stack is at 4; reaches 7 or 8 internally (long/double takes 2)

      // Invoke the matching JavaScriptHost.invokeNative* method
      Type returnType = Type.getReturnType(descriptor);
      JavaScriptHostInfo info = JavaScriptHostInfo.get(returnType.getSort());
      invokeStatic(JavaScriptHostInfo.TYPE, info.getMethod());
      // Stack is at 1
      if (info.requiresCast()) {
        checkCast(returnType);
      }
      returnValue();
      // Stack is at 0
    }

    @Override
    public void visitEnd() {
      // Force code to be visited; required since this was a native method.
      visitCode();

      /*
       * For speed, we don't ask ASM to COMPUTE_MAXS. We manually calculated a
       * max depth of 8.
       * 
       * Also, when tobyr tried getting ASM to compute the correct stack size,
       * ASM seemed to compute the wrong value for reasons we don't understand.
       */
      int maxStack = 8;
      int maxLocals = 0; // Computed by GeneratorAdapter superclass.
      super.visitMaxs(maxStack, maxLocals);
      super.visitEnd();
    }

    private void loadClassArray() {
      Type[] argTypes = Type.getArgumentTypes(descriptor);
      push(argTypes.length);
      newArray(CLASS_TYPE);
      // Stack is at 3
      for (int i = 0; i < argTypes.length; ++i) {
        dup();
        push(i);
        push(argTypes[i]);
        arrayStore(CLASS_TYPE);
      }
    }
  }

  private static final Type BOOLEAN_TYPE = Type.getObjectType("java/lang/Boolean");
  private static final Type BYTE_TYPE = Type.getObjectType("java/lang/Byte");
  private static final Type CHARACTER_TYPE = Type.getObjectType("java/lang/Character");
  private static final Type CLASS_TYPE = Type.getObjectType("java/lang/Class");
  private static final Type DOUBLE_TYPE = Type.getObjectType("java/lang/Double");
  private static final Type FLOAT_TYPE = Type.getObjectType("java/lang/Float");
  private static final Type INTEGER_TYPE = Type.getObjectType("java/lang/Integer");
  private static final Type LONG_TYPE = Type.getObjectType("java/lang/Long");
  private static final Type SHORT_TYPE = Type.getObjectType("java/lang/Short");
  private static final Type VOID_TYPE = Type.getObjectType("java/lang/Void");

  /**
   * The internal name of the class we're operating on.
   */
  private String classDesc;
  private Map<String, String> anonymousClassMap;

  public RewriteJsniMethods(ClassVisitor v,
      Map<String, String> anonymousClassMap) {
    super(v);
    this.anonymousClassMap = anonymousClassMap;
  }

  @Override
  public void visit(final int version, final int access, final String name,
      final String signature, final String superName, final String[] interfaces) {
    this.classDesc = name;
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
      String signature, String[] exceptions) {

    boolean isNative = (access & Opcodes.ACC_NATIVE) != 0;
    access &= ~Opcodes.ACC_NATIVE;

    MethodVisitor mv = super.visitMethod(access, name, desc, signature,
        exceptions);

    if (isNative) {
      mv = new MyMethodAdapter(mv, access, name, desc);
    }

    return mv;
  }

  /**
   * Returns the JSNI signature describing the method.
   * 
   * @param name the name of the method; for example {@code "echo"}
   * @param descriptor the descriptor for the method; for example {@code "(I)I"}
   * @return the JSNI signature for the method; for example, {@code
   *         "@com.google.gwt.sample.hello.client.Hello::echo(I)"}
   */
  private String getJsniSignature(String name, String descriptor) {
    int argsIndexBegin = descriptor.indexOf('(');
    int argsIndexEnd = descriptor.indexOf(')');
    assert argsIndexBegin != -1 && argsIndexEnd != -1
        && argsIndexBegin < argsIndexEnd : "Could not find the arguments in the descriptor, "
        + descriptor;
    String argsDescriptor = descriptor.substring(argsIndexBegin,
        argsIndexEnd + 1);
    String classDescriptor = InternalName.toBinaryName(classDesc);
    String newDescriptor = anonymousClassMap.get(classDesc);
    if (newDescriptor != null) {
      classDescriptor = InternalName.toBinaryName(newDescriptor);
    }
    // Always use binary names for JSNI method names
    return "@" + classDescriptor + "::" + name + argsDescriptor;
  }
}
