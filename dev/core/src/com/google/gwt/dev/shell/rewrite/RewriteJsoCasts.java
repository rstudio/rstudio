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

import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.CANONICAL_FIELD;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.EXTRA_DEBUG_DATA;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.JAVASCRIPTOBJECT_DESC;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.REWRAP_METHOD;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.SINGLE_JSO_IMPL_ADJUNCT_SUFFIX;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.SINGLE_JSO_IMPL_CAST_METHOD;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.SINGLE_JSO_IMPL_CAST_TO_OBJECT_METHOD;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.SINGLE_JSO_IMPL_INSTANCEOF_METHOD;
import static com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.SINGLE_JSO_IMPL_SUPPORT_CLASS;

import com.google.gwt.dev.asm.Attribute;
import com.google.gwt.dev.asm.ByteVector;
import com.google.gwt.dev.asm.ClassAdapter;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.ClassWriter;
import com.google.gwt.dev.asm.Label;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.asm.commons.AnalyzerAdapter;
import com.google.gwt.dev.asm.commons.Method;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.RewriterOracle;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ensure that an appropriate JSO wrapper type or canonical JavaScriptObject is
 * used. Assignments to fields, locals, arrays, return statements, and method
 * parameters will be updated with the following rules:
 * <ul>
 * <li>Changing from one JSO type to another results in a replacement JSO
 * Wrapper type being placed on the stack</li>
 * <li>Referring to a JSO or a JSO subype as Object results in the canonical JSO
 * instance being placed on the stack</li>
 * <li>An invocation of <code>getClass()</code> on a JSO subtype results in
 * <code>JavaScriptObject.class</code> being placed on the stack.</li>
 * </ul>
 */
class RewriteJsoCasts extends ClassAdapter {

  /**
   * Injects necessary casting logic.
   */
  private class MyMethodAdapter extends AnalyzerAdapter {
    /*
     * NOTE TO MAINTAINERS: It's tempting to sprinkle one-off cast-handling
     * logic around this class. That way lies madness while trying to track down
     * exactly how a particular bit of cast logic wound up in the bytecode.
     * Instead, all casting logic should be routed through generateCast.
     */

    private final Map<Label, String> debugData = EXTRA_DEBUG_DATA
        ? new LinkedHashMap<Label, String>() : null;
    private final boolean returnNeedsCanonical;
    private final Type returnType;

    public MyMethodAdapter(int access, String name, String desc,
        MethodVisitor mv) {
      super(currentClass, access, name, desc, mv);

      Method m = new Method(name, desc);
      returnType = m.getReturnType();
      returnNeedsCanonical = returnType.getSort() == Type.OBJECT
          && rewriterOracle.jsoAssignmentRequiresCanonicalization(returnType.getInternalName());
    }

    @Override
    public void visitEnd() {
      if (EXTRA_DEBUG_DATA && !debugData.isEmpty()) {
        /*
         * When things go wrong in the field, users can enable rewritten
         * class-file dumping and send us the class files. Most of the other
         * rewriting passes add whole classes or methods so their effects are
         * easy to find. Since this rewriting pass is rather subtle, we'll add
         * an extra method attribute that contains a UTF8-encoded diagnostic
         * log.
         */
        Attribute attr = new Attribute("com.google.gwt.RewriteJsoCasts") {
          @Override
          protected Label[] getLabels() {
            return debugData.keySet().toArray(new Label[debugData.size()]);
          }

          @Override
          protected ByteVector write(ClassWriter cw, byte[] code, int len,
              int maxStack, int maxLocals) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Label, String> entry : debugData.entrySet()) {
              String line = "Offset " + entry.getKey().getOffset() + " : "
                  + entry.getValue() + "\n";
              sb.append(line);
            }
            ByteVector toReturn = new ByteVector();
            toReturn.putUTF8(sb.toString());
            return toReturn;
          }
        };
        super.visitAttribute(attr);
      }
      super.visitEnd();
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name,
        String desc) {
      if (desc.charAt(0) == 'L' && !owner.equals(JAVASCRIPTOBJECT_DESC)
          && !name.equals(CANONICAL_FIELD)) {
        switch (opcode) {
          case Opcodes.PUTFIELD:
          case Opcodes.PUTSTATIC:
            generateCast(desc.substring(1, desc.length() - 1));
        }
      }
      super.visitFieldInsn(opcode, owner, name, desc);
    }

    /**
     * Possibly canonicalize returned values.
     */
    @Override
    public void visitInsn(int opcode) {
      if (opcode == Opcodes.ARETURN && returnNeedsCanonical) {
        // Stack is: returnValue
        generateCast(returnType.getInternalName());
        // Stack is: canonical
      } else if (opcode == Opcodes.AASTORE) {
        // Stack is: ..., arrayRef, index, value
        Object topType = stack.get(stack.size() - 1);
        if (topType instanceof String) {
          String objectType = (String) topType;
          String arrayType = (String) stack.get(stack.size() - 3);
          Type t = Type.getObjectType(arrayType);
          if (t.getDimensions() == 1
              && rewriterOracle.jsoAssignmentRequiresCanonicalization(objectType)) {
            /*
             * If the object being assigned needs canonicalization, then we want
             * to ensure the canonical object is being stored in the array.
             */
            generateCast(objectType);
          }
        }
      }
      super.visitInsn(opcode);
    }

    /**
     * Replace references to JSO subtype class literals with references to
     * JavaScriptObject.
     */
    @Override
    public void visitLdcInsn(Object cst) {
      if (cst instanceof Type) {
        Type upcast = upcastJsoType((Type) cst);
        if (upcast != null) {
          cst = upcast;
        }
      }
      super.visitLdcInsn(cst);
    }

    /**
     * Canonicalize the arguments to methods that require Object or
     * JavaScriptObject as parameters. Additional local variables will be
     * allocated to save as many of the arguments as necessary in order to cast
     * the first parameter that requires canonicalization.
     */
    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
        String desc) {

      // Update calls to getClass()
      if ("getClass".equals(name) && "()Ljava/lang/Class;".equals(desc)) {
        // Stack is: instance
        Type t = Type.getObjectType((String) stack.get(stack.size() - 1));
        if (t.getSort() == Type.OBJECT
            && rewriterOracle.couldContainJso(t.getInternalName())) {
          /*
           * Make sure the canonical object is on the stack. This method call
           * will pass nulls straight through, so the getClass() invocation will
           * still trigger an NPE.
           */
          super.visitMethodInsn(Opcodes.INVOKESTATIC,
              SINGLE_JSO_IMPL_SUPPORT_CLASS, "ensureCanonical",
              "(Ljava/lang/Object;)Ljava/lang/Object;");
          // Stack is: canonical object
          super.visitMethodInsn(opcode, "java/lang/Object", name, desc);
          // Stack is: classLit
          return;

        } else if (t.getSort() == Type.ARRAY
            && t.getElementType().getSort() == Type.OBJECT
            && rewriterOracle.isJsoOrSubtype(t.getElementType().getInternalName())) {
          // Call the method to throw an NPE if necessary
          super.visitMethodInsn(opcode, "java/lang/Object", name, desc);
          // Stack is: classLit
          super.visitInsn(Opcodes.POP);
          // Stack is: <empty>
          super.visitLdcInsn(upcastJsoType(t));
          // Stack is: classLit
          return;
        }
      } else if ("cast".equals(name)
          && ("()L" + JAVASCRIPTOBJECT_DESC + ";").equals(desc)
          && rewriterOracle.isJsoOrSubtype(owner)) {
        // Remove calls to cast, since they trigger NPEs in older code
        return;
      }

      /*
       * We want to ensure that all arguments passed into the method have been
       * canonicalized. To do this, we'll find the first argument on the stack
       * that requires canonicalization. Each argument on above the first
       * argument on the stack will be stored in a temporary local variable and
       * eventually restored.
       * 
       * Assume the stack looks like this:
       * 
       * instance, Ok, NeedsCanonicalization, Ok, Ok, NeedsCanonicalization
       * 
       * (1) We'll store the top three arguments in locals until we have this:
       * 
       * instance, Ok, NeedsCanonicalization
       * 
       * (2) Then, we'll apply whatever canonicalization is necessary to the
       * top-most element:
       * 
       * instance, Ok, Canonicalized
       * 
       * (3) The arguments stored in the temporary local variables will be
       * pushed back onto the stack, possibly canonicalizing them in the
       * process:
       * 
       * instance, Ok, Canonicalized, Ok, Ok, Canonicalized
       * 
       * This could be accomplished without the use of local variables if we
       * were to track the last opcode that pushes an argument value onto the
       * stack and buffer intervening opcodes or use a two-pass approach. Once
       * the relevant opcode has been identified, the canonicalization logic
       * could be immediately inserted.
       */
      Method m = new Method(name, desc);
      Type[] args = m.getArgumentTypes();
      int firstCast = -1;
      boolean[] needsCanonicalization = new boolean[args.length];

      // (1) Works backwards, since the last argument is on top of the stack
      for (int i = args.length - 1, s = stack.size(); i >= 0; i--) {
        Type arg = args[i];
        s -= arg.getSize();

        // Ignore arguments that never require a canonicalizing cast
        if (arg.getSort() != Type.OBJECT
            || !rewriterOracle.jsoAssignmentRequiresCanonicalization(arg.getInternalName())) {
          continue;
        }

        Object onStack = stack.get(s);
        assert !onStack.equals(Opcodes.TOP) : "Bad offset computation";

        if (onStack instanceof String) {
          String stackType = (String) onStack;
          if (rewriterOracle.couldContainJso(stackType)) {
            needsCanonicalization[i] = true;
            firstCast = i;
          }
        }
      }

      // Short-circuit if none of the arguments need canonicalization
      if (firstCast >= 0) {
        // These labels are used to provide information to the debugger
        Label castStackStart = new Label();
        Label castStackEnd = new Label();

        // (1) Pop N - 1 arguments off of the stack, storing them in locals
        super.visitLabel(castStackStart);
        @SuppressWarnings("unchecked")
        List<Object> originalLocals = new ArrayList<Object>(locals);
        int[] slots = new int[args.length];
        for (int i = args.length - 1; i >= firstCast + 1; i--) {
          Type arg = args[i];
          int slot = locals.size();
          slots[i] = slot;
          super.visitVarInsn(arg.getOpcode(Opcodes.ISTORE), slot);
        }

        // (2) Convert the first argument
        generateCast(args[firstCast].getInternalName());

        // (3) Load arguments, converting those that need them
        for (int i = firstCast + 1; i < args.length; i++) {
          Type arg = args[i];
          int slot = slots[i];
          super.visitVarInsn(arg.getOpcode(Opcodes.ILOAD), slot);
          if (needsCanonicalization[i]) {
            generateCast(arg.getInternalName());
          }
        }

        super.visitLabel(castStackEnd);

        // Record locals to aid debbuging and manual inspection of bytecode.
        for (int i = firstCast + 1; i < args.length; i++) {
          super.visitLocalVariable("$cast_" + i, args[i].getDescriptor(), null,
              castStackStart, castStackEnd, slots[i]);
        }

        // Record new stack / locals size
        super.visitMaxs(stack.size(), locals.size());

        /*
         * Reset the local variables as though they were declared inside of a
         * scoped block in the middle of a method.
         */
        @SuppressWarnings("unchecked")
        Object[] stackArray = stack.toArray(new Object[stack.size()]);
        super.visitFrame(Opcodes.F_NEW, originalLocals.size(),
            originalLocals.toArray(new Object[originalLocals.size()]),
            stack.size(), stackArray);
      }

      // Call the method
      super.visitMethodInsn(opcode, owner, name, desc);
    }

    /**
     * Rewrite casts and instanceof tests.
     */
    @Override
    public void visitTypeInsn(int opcode, String type) {
      Type parsed = Type.getObjectType(type);
      switch (opcode) {
        case Opcodes.CHECKCAST:
          if (rewriterOracle.couldContainJso(type)) {
            // Maybe generate casting code for JSO types
            if (generateCast(type)) {
              return;
            }
          } else if (parsed.getSort() == Type.ARRAY) {
            // Always upcast JSO array types
            Type t = upcastJsoType(parsed);
            if (t != null) {
              type = t.getInternalName();
            }
          }
          // Intentional fall-through to super invocation below
          break;

        case Opcodes.INSTANCEOF: {
          if (rewriterOracle.isInterface(parsed.getInternalName())
              && rewriterOracle.couldContainJso(parsed.getInternalName())) {
            /*
             * We need to use the interface's adjunct type if the interface
             * could contain a JSO type.
             */
            super.visitMethodInsn(Opcodes.INVOKESTATIC, type
                + SINGLE_JSO_IMPL_ADJUNCT_SUFFIX,
                SINGLE_JSO_IMPL_INSTANCEOF_METHOD, "(Ljava/lang/Object;)Z");
            return;
          }
          // Always try to upcast instanceof JSO tests
          Type t = upcastJsoType(parsed);
          if (t != null) {
            type = t.getInternalName();
          }
          break;
        }
      }
      super.visitTypeInsn(opcode, type);
    }

    /**
     * The do-everything method for determining how to make whatever is on top
     * of the stack fit correctly in a slot of the given type. Casts to Object
     * should result in the canonical object winding up on the stack. Otherwise,
     * we may need a new wrapper instance.
     * <p>
     * This method should generate code that is stack-neutral. Any non-trivial
     * cast logic should be moved to a support class, either
     * {@link SingleJsoImplSupport} or the synthetic interface adjunct classes.
     * 
     * @param internalName the type to which the object on top of the stack
     *          should be assigned
     * @return <code>true</code> if cast code was generated
     */
    private boolean generateCast(String internalName) {
      assert internalName.charAt(0) != 'L'
          && internalName.charAt(internalName.length() - 1) != ';' : "Passed "
          + "a descriptor instead of an internal name: " + internalName;
      Object topStack = stack.get(stack.size() - 1);
      if (!(topStack instanceof String)) {
        return false;
      }

      final String topType = (String) topStack;

      if (EXTRA_DEBUG_DATA) {
        Label castLocation = new Label();
        debugData.put(castLocation, topType + "  ->  " + internalName);
        super.visitLabel(castLocation);
      }

      if (internalName.equals("java/lang/Object")) {
        /*
         * We're looking at code (probably created by one of our other visitors)
         * that is casting a value to java.lang.Object.
         */
        // Stack is: something

        if (rewriterOracle.isJsoOrSubtype(topType)) {
          /*
           * We know that the top of the stack is a wrapper type, so we need to
           * canonicalize it by calling JavaScriptObject's synthetic rewrap
           * method.
           */
          // Stack is: jsoSubtype
          super.visitMethodInsn(Opcodes.INVOKESTATIC, JAVASCRIPTOBJECT_DESC,
              REWRAP_METHOD, "(L" + JAVASCRIPTOBJECT_DESC + ";)L"
                  + JAVASCRIPTOBJECT_DESC + ";");
          // Stack is: canonicalObject
          return true;

        } else if (rewriterOracle.isInterface(topType)
            && rewriterOracle.couldContainJso(topType)) {
          /*
           * We're looking at an interface that may contain a JSO. If the object
           * is a JSO, replace it with the canonical instance by calling into
           * the interface's SingleJsoImpl adjunct class's castToObject()
           * method.
           */
          String supportType = topType + SINGLE_JSO_IMPL_ADJUNCT_SUFFIX;

          // Stack is: interfaceType
          super.visitMethodInsn(Opcodes.INVOKESTATIC, supportType,
              SINGLE_JSO_IMPL_CAST_TO_OBJECT_METHOD, "(L" + topType
                  + ";)Ljava/lang/Object;");
          // Stack is: object (possible canonical JSO)
          return true;

        } else if ("java/lang/Object".equals(topType)) {
          /*
           * This is a situation that we'll run into when dealing with JRE code.
           * See JsoTest.testArrayJreInteractions for the canonical example of
           * loss of type information.
           */
          super.visitMethodInsn(Opcodes.INVOKESTATIC,
              SINGLE_JSO_IMPL_SUPPORT_CLASS, "ensureCanonical",
              "(Ljava/lang/Object;)Ljava/lang/Object;");
          // Stack is: canonical object
          return true;

        } else {
          // Casting some non-JSO type to Object
          return false;
        }

      } else if (internalName.equals(topType)) {
        // Casting an object to its own type
        return false;

      } else if (!rewriterOracle.couldContainJso(internalName)) {
        // It's a cast to something that could never contain a JSO
        return false;

      } else if (rewriterOracle.isInterface(internalName)
          && rewriterOracle.couldContainJso(internalName)) {
        /*
         * Casting to a SingleJsoImpl interface. Need to use the SingleJsoImpl
         * adjunct to possibly create a new wrapper type.
         */

        // Stack is: something
        super.visitMethodInsn(Opcodes.INVOKESTATIC, internalName
            + SINGLE_JSO_IMPL_ADJUNCT_SUFFIX, SINGLE_JSO_IMPL_CAST_METHOD,
            "(Ljava/lang/Object;)Ljava/lang/Object;");
        // Stack is: something (maybe JSO wrapper type)
        super.visitTypeInsn(Opcodes.CHECKCAST, internalName);
        // Stack is: desiredType (maybe JSO wrapper type)

        return true;

      } else if (rewriterOracle.isJsoOrSubtype(internalName)) {
        /*
         * Casting to a JavaScriptObject subtype.
         */

        /*
         * Change the cast to JavaScriptObject to ensure we get a
         * ClassCastException for something like (JsoSubclass) "foo".
         */
        // Stack is: something
        super.visitTypeInsn(Opcodes.CHECKCAST, JAVASCRIPTOBJECT_DESC);
        // Stack is: jso

        /*
         * Put the canonical object onto the stack by calling
         * JsoSubclass.$rewrap().
         */
        super.visitMethodInsn(Opcodes.INVOKESTATIC, internalName,
            REWRAP_METHOD, "(L" + JAVASCRIPTOBJECT_DESC + ";)L" + internalName
                + ";");
        // Stack is: wrapperObject
        return true;
      }

      /*
       * Fail definitively, since getting this wrong is going to break the user
       * in all kinds of unpredictable ways.
       */
      throw new RuntimeException(
          "generateCast was called with an unhandled configuration. topType: "
              + topType + " internalName: " + internalName);
    }

    /**
     * If <code>type</code> is a JSO subtype or JSO subtype array, return the
     * JSO type or JSO array type to which <code>type</code> can be assigned, or
     * <code>null</code>.
     */
    private Type upcastJsoType(Type type) {
      StringBuilder sb = new StringBuilder();
      if (type.getSort() == Type.ARRAY) {
        Type elementType = type.getElementType();
        if (elementType.getSort() != Type.OBJECT
            || !rewriterOracle.isJsoOrSubtype(elementType.getInternalName())) {
          return null;
        }
        for (int i = 0, j = type.getDimensions(); i < j; i++) {
          sb.append("[");
        }
      } else if (type.getSort() == Type.OBJECT) {
        if (type.getInternalName().equals(JAVASCRIPTOBJECT_DESC)
            || !rewriterOracle.isJsoOrSubtype(type.getInternalName())) {
          return null;
        }
      } else {
        return null;
      }
      sb.append("L" + JAVASCRIPTOBJECT_DESC + ";");
      return Type.getType(sb.toString());
    }
  }

  /**
   * Decodes the UTF8-as-octets output from javap.
   */
  public static void main(String args[]) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    Pattern p = Pattern.compile("[0-9A-F]{2}");
    String line;
    while ((line = reader.readLine()) != null) {
      if (line.length() > 0) {
        Matcher m = p.matcher(line);
        while (m.find()) {
          bytes.write(Byte.parseByte(m.group(), 16));
        }
      } else {
        String message = new String(bytes.toByteArray());
        System.out.println(message);
        bytes = new ByteArrayOutputStream();
      }
    }
  }

  private String currentClass;
  private final RewriterOracle rewriterOracle;

  public RewriteJsoCasts(ClassVisitor v, RewriterOracle rewriterOracle) {
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
    if (mv != null && !REWRAP_METHOD.equals(name)) {
      mv = new MyMethodAdapter(access, name, desc, mv);
    }
    return mv;
  }
}
