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

import com.google.gwt.dev.asm.ClassAdapter;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.MethodAdapter;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.asm.commons.Method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Effects the renaming of {@code @SingleJsoImpl} methods from their original
 * name to their mangled name. Let us call the original method an "unmangled
 * method" and the new method a "mangled method". There are three steps in this
 * process:
 * <ol>
 * <li>Within {@code @SingleJsoImpl} interfaces rename all unmangled methods to
 * become mangled methods.</li>
 * <li>Within non-JSO classes containing a concrete implementation of an
 * unmangled method, add a mangled method which is implemented as a simple
 * trampoline to the unmangled method. (We don't do this in JSO classes here
 * because the one-and-only trampoline lives in JavaScriptObject$ and is emitted
 * in {@link WriteJsoImpl}).
 * <li>Update all call sites targeting unmangled methods to target mangled
 * methods instead, provided the caller is binding to the interface rather than
 * a concrete type.</li>
 * </ol>
 */
public class RewriteSingleJsoImplDispatches extends ClassAdapter {
  private class MyMethodVisitor extends MethodAdapter {
    public MyMethodVisitor(MethodVisitor mv) {
      super(mv);
    }

    /*
     * Implements objective #3: updates call sites to unmangled methods.
     */
    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
        String desc) {
      if (singleJsoImplTypes.contains(owner)) {
        name = owner.replace('/', '_') + "_" + name;
      }

      super.visitMethodInsn(opcode, owner, name, desc);
    }
  }

  private String currentTypeName;
  private final Set<String> implementedMethods = new HashSet<String>();
  private final SortedMap<String, Method> mangledNamesToImplementations;
  private final Set<String> singleJsoImplTypes;
  private boolean inSingleJsoImplInterfaceType;

  public RewriteSingleJsoImplDispatches(ClassVisitor v,
      Set<String> singleJsoImplTypes,
      SortedMap<String, Method> mangledNamesToImplementations) {
    super(v);
    this.singleJsoImplTypes = Collections.unmodifiableSet(singleJsoImplTypes);
    this.mangledNamesToImplementations = Collections.unmodifiableSortedMap(mangledNamesToImplementations);
  }

  @Override
  public void visit(int version, int access, String name, String signature,
      String superName, String[] interfaces) {
    assert currentTypeName == null;
    super.visit(version, access, name, signature, superName, interfaces);

    currentTypeName = name;
    inSingleJsoImplInterfaceType = singleJsoImplTypes.contains(name);

    /*
     * Implements objective #2: non-JSO types that implement a SingleJsoImpl
     * interface don't have their original instance methods altered. Instead, we
     * add trampoline methods with mangled names that simply call over to the
     * original methods.
     */
    if (interfaces != null && (access & Opcodes.ACC_INTERFACE) == 0) {
      List<String> toStub = new ArrayList<String>();
      Collections.addAll(toStub, interfaces);
      toStub.retainAll(singleJsoImplTypes);

      for (String stubIntr : toStub) {
        writeTrampoline(stubIntr);
      }
    }
  }

  @Override
  public void visitEnd() {
    /*
     * Add any missing methods that are defined by a super-interface, but that
     * may be referenced via a more specific interface.
     */
    if (inSingleJsoImplInterfaceType) {
      for (Map.Entry<String, Method> entry : toImplement(currentTypeName).entrySet()) {
        writeEmptyMethod(entry.getKey(), entry.getValue());
      }
    }
    super.visitEnd();
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
      String signature, String[] exceptions) {

    /*
     * Implements objective #2: Rename unmangled methods in a @SingleJsoImpl
     * into mangled methods (except for clinit, LOL).
     */
    if (inSingleJsoImplInterfaceType && !"<clinit>".equals(name)) {
      name = currentTypeName.replace('/', '_') + "_" + name;
      implementedMethods.add(name);
    }

    MethodVisitor mv = super.visitMethod(access, name, desc, signature,
        exceptions);
    if (mv == null) {
      return null;
    }

    return new MyMethodVisitor(mv);
  }

  /**
   * Given a resource name of a class, find all mangled method names that must
   * be implemented.
   */
  private SortedMap<String, Method> toImplement(String typeName) {
    String name = typeName.replace('/', '_');
    String prefix = name + "_";
    String suffix = name + "`";
    SortedMap<String, Method> toReturn = new TreeMap<String, Method>(
        mangledNamesToImplementations.subMap(prefix, suffix));
    toReturn.keySet().removeAll(implementedMethods);
    return toReturn;
  }

  private void writeEmptyMethod(String mangledMethodName, Method method) {
    assert method.getArgumentTypes().length > 0;
    // Remove the first argument, which would be the implementing JSO type
    String descriptor = "("
        + method.getDescriptor().substring(
            1 + method.getArgumentTypes()[0].getDescriptor().length());

    // Create the stub method entry in the interface
    MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC
        | Opcodes.ACC_ABSTRACT, mangledMethodName, descriptor, null, null);
    mv.visitEnd();
  }

  /**
   * For regular Java objects that implement a SingleJsoImpl interface, write
   * instance trampoline dispatchers for mangled method names to the
   * implementing method.
   */
  private void writeTrampoline(String stubIntr) {
    /*
     * This is almost the same kind of trampoline as the ones generated in
     * WriteJsoImpl, however there are enough small differences between the
     * semantics of the dispatches that would make a common implementation far
     * more awkward than the duplication of code.
     */
    for (Map.Entry<String, Method> entry : toImplement(stubIntr).entrySet()) {
      String mangledName = entry.getKey();
      Method method = entry.getValue();

      String descriptor = "("
          + method.getDescriptor().substring(
              1 + method.getArgumentTypes()[0].getDescriptor().length());
      String localName = method.getName().substring(0,
          method.getName().length() - 1);
      Method toCall = new Method(localName, descriptor);

      // Must not be final
      MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC
          | Opcodes.ACC_SYNTHETIC, mangledName, descriptor, null, null);
      if (mv != null) {
        mv.visitCode();

        /*
         * It just so happens that the stack and local variable sizes are the
         * same, but they're kept distinct to aid in clarity should the dispatch
         * logic change.
         * 
         * These start at 1 because we need to load "this" onto the stack
         */
        int var = 1;
        int size = 1;

        // load this
        mv.visitVarInsn(Opcodes.ALOAD, 0);

        // then the rest of the arguments
        for (Type t : toCall.getArgumentTypes()) {
          size += t.getSize();
          mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), var);
          var += t.getSize();
        }

        // Make sure there's enough room for the return value
        size = Math.max(size, toCall.getReturnType().getSize());

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, currentTypeName,
            toCall.getName(), toCall.getDescriptor());
        mv.visitInsn(toCall.getReturnType().getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(size, var);
        mv.visitEnd();
      }
    }
  }
}
