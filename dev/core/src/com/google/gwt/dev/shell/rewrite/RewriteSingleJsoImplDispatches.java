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

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.asm.commons.Method;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.SingleJsoImplData;
import com.google.gwt.dev.util.collect.Maps;
import com.google.gwt.dev.util.collect.Sets;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
public class RewriteSingleJsoImplDispatches extends ClassVisitor {
  private class MyMethodVisitor extends MethodVisitor {
    public MyMethodVisitor(MethodVisitor mv) {
      super(Opcodes.ASM4, mv);
    }

    /*
     * Implements objective #3: updates call sites to unmangled methods.
     */
    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
        String desc) {
      if (opcode == Opcodes.INVOKEINTERFACE) {
        if (jsoData.getSingleJsoIntfTypes().contains(owner)) {
          // Simple case; referring directly to a SingleJso interface.
          name = owner.replace('/', '_') + "_" + name;
          assert jsoData.getMangledNames().contains(name) : "Missing " + name;

        } else {
          /*
           * Might be referring to a subtype of a SingleJso interface:
           *
           * interface IA { void foo() }
           *
           * interface JA extends JSO implements IA;
           *
           * interface IB extends IA {}
           *
           * void bar() { ((IB) object).foo(); }
           */
          outer : for (String intf : computeAllInterfaces(owner)) {
            if (jsoData.getSingleJsoIntfTypes().contains(intf)) {
              /*
               * Check that it really should be mangled and is not a reference
               * to a method defined in a non-singleJso super-interface. If
               * there are two super-interfaces that define methods with
               * identical names and descriptors, the choice of implementation
               * is undefined.
               */
              String maybeMangled = intf.replace('/', '_') + "_" + name;
              List<Method> methods = jsoData.getImplementations(maybeMangled);
              if (methods != null) {
                for (Method method : methods) {
                  /*
                   * Found a method with the right name, but we need to check
                   * the parameters and the return type. In order to do this,
                   * we'll look at the arguments and return type of the target
                   * method, removing the first argument, which is the instance.
                   */
                  assert method.getArgumentTypes().length >= 1;
                  Type[] argumentTypes = new Type[method.getArgumentTypes().length - 1];
                  System.arraycopy(method.getArgumentTypes(), 1, argumentTypes,
                      0, argumentTypes.length);
                  String maybeDescriptor = Type.getMethodDescriptor(
                      method.getReturnType(), argumentTypes);
                  if (maybeDescriptor.equals(desc)) {
                    name = maybeMangled;
                    break outer;
                  }
                }
              }
            }
          }
        }
      }

      super.visitMethodInsn(opcode, owner, name, desc);
    }
  }

  private String currentTypeName;
  private final Set<String> implementedMethods = new HashSet<String>();
  private boolean inSingleJsoImplInterfaceType;
  private Map<String, Set<String>> intfNamesToAllInterfaces = Maps.create();
  private final SingleJsoImplData jsoData;
  private final TypeOracle typeOracle;

  public RewriteSingleJsoImplDispatches(ClassVisitor v, TypeOracle typeOracle,
      SingleJsoImplData jsoData) {
    super(Opcodes.ASM4, v);
    this.typeOracle = typeOracle;
    this.jsoData = jsoData;
  }

  @Override
  public void visit(int version, int access, String name, String signature,
      String superName, String[] interfaces) {
    assert currentTypeName == null;
    super.visit(version, access, name, signature, superName, interfaces);

    /*
     * This visitor would mangle JSO$ since it acts as a roll-up of all
     * SingleJso types and the result would be repeated method definitions due
     * to the trampoline methods this visitor would create.
     */
    if (name.equals(HostedModeClassRewriter.JAVASCRIPTOBJECT_IMPL_DESC)) {
      return;
    }

    currentTypeName = name;
    inSingleJsoImplInterfaceType = jsoData.getSingleJsoIntfTypes().contains(
        name);

    /*
     * Implements objective #2: non-JSO types that implement a SingleJsoImpl
     * interface don't have their original instance methods altered. Instead, we
     * add trampoline methods with mangled names that simply call over to the
     * original methods.
     */
    if (interfaces != null && (access & Opcodes.ACC_INTERFACE) == 0) {
      Set<String> toStub = computeAllInterfaces(interfaces);
      toStub.retainAll(jsoData.getSingleJsoIntfTypes());

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
      for (String mangledName : toImplement(currentTypeName)) {
        for (Method method : jsoData.getDeclarations(mangledName)) {
          writeEmptyMethod(mangledName, method);
        }
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

  private Set<String> computeAllInterfaces(String intfName) {
    Set<String> toReturn = intfNamesToAllInterfaces.get(intfName);
    if (toReturn != null) {
      return toReturn;
    }

    toReturn = Sets.create();
    List<JClassType> q = new LinkedList<JClassType>();
    JClassType intf = typeOracle.findType(intfName.replace('/', '.').replace(
        '$', '.'));

    /*
     * If the interface's compilation unit wasn't retained due to an error, then
     * it won't be available in the typeOracle for us to rewrite
     */
    if (intf != null) {
      q.add(intf);
    }

    while (!q.isEmpty()) {
      intf = q.remove(0);
      String resourceName = getResourceName(intf);
      if (!toReturn.contains(resourceName)) {
        toReturn = Sets.add(toReturn, resourceName);
        Collections.addAll(q, intf.getImplementedInterfaces());
      }
    }

    intfNamesToAllInterfaces = Maps.put(intfNamesToAllInterfaces, intfName,
        toReturn);
    return toReturn;
  }

  private Set<String> computeAllInterfaces(String[] interfaces) {
    Set<String> toReturn = new HashSet<String>();
    for (String intfName : interfaces) {
      toReturn.addAll(computeAllInterfaces(intfName));
    }
    return toReturn;
  }

  private String getResourceName(JClassType type) {
    if (type.getEnclosingType() != null) {
      return getResourceName(type.getEnclosingType()) + "$"
          + type.getSimpleSourceName();
    }
    return type.getQualifiedSourceName().replace('.', '/');
  }

  /**
   * Given a resource name of a class, find all mangled method names that must
   * be implemented.
   */
  private SortedSet<String> toImplement(String typeName) {
    String name = typeName.replace('/', '_');
    String prefix = name + "_";
    String suffix = name + "`";
    SortedSet<String> toReturn = new TreeSet<String>();
    for (String mangledName : jsoData.getMangledNames().subSet(prefix, suffix)) {
      if (!implementedMethods.contains(mangledName)) {
        toReturn.add(mangledName);
      }
    }
    return toReturn;
  }

  private void writeEmptyMethod(String mangledMethodName, Method declMethod) {
    MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC
        | Opcodes.ACC_ABSTRACT, mangledMethodName, declMethod.getDescriptor(),
        null, null);
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
    for (String mangledName : toImplement(stubIntr)) {
      for (Method method : jsoData.getDeclarations(mangledName)) {

        Method toCall = new Method(method.getName(), method.getDescriptor());

        // Must not be final
        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC
            | Opcodes.ACC_SYNTHETIC, mangledName, method.getDescriptor(), null, null);
        if (mv != null) {
          mv.visitCode();

          /*
           * It just so happens that the stack and local variable sizes are the
           * same, but they're kept distinct to aid in clarity should the
           * dispatch logic change.
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
}
