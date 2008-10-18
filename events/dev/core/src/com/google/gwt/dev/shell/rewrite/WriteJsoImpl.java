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
import com.google.gwt.dev.asm.FieldVisitor;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.InstanceMethodOracle;

import java.util.ArrayList;
import java.util.Set;

/**
 * Writes the implementation class for a JSO type.
 * 
 * <ol>
 * <li>The new type has the same name as the old type with a '$' appended.</li>
 * <li>The new type's superclass is Object.</li>
 * <li>All instance methods in the original type become static methods taking
 * an explicit <code>this</code> parameter. Such methods have the same stack
 * behavior as the original.</li>
 * <li>JavaScriptObject itself gets a new synthetic field to store the
 * underlying hosted mode reference.</li>
 * </ol>
 */
class WriteJsoImpl extends ClassAdapter {

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
   * The original name of the class being visited.
   */
  private String originalName;

  /**
   * Construct a new rewriter instance.
   * 
   * @param cv the visitor to chain to
   * @param jsoDescriptors an unmodifiable set of descriptors containing
   *          <code>JavaScriptObject</code> and all subclasses
   * @param mapper maps methods to the class in which they are declared
   */
  public WriteJsoImpl(ClassVisitor cv, Set<String> jsoDescriptors,
      InstanceMethodOracle mapper) {
    super(cv);
    this.jsoDescriptors = jsoDescriptors;
    this.mapper = mapper;
  }

  @Override
  public void visit(int version, int access, String name, String signature,
      String superName, String[] interfaces) {
    originalName = name;

    // JavaScriptObject$ must implement all JSO interface types.
    if (isJavaScriptObject()) {
      ArrayList<String> jsoDescList = new ArrayList<String>();
      jsoDescList.addAll(jsoDescriptors);
      interfaces = jsoDescList.toArray(new String[jsoDescList.size()]);
    } else {
      // Reference the old superclass's implementation class.
      superName += '$';
      interfaces = null;
    }

    super.visit(version, access, name + '$', signature, superName, interfaces);

    if (isJavaScriptObject()) {
      FieldVisitor fv = visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
          HostedModeClassRewriter.REFERENCE_FIELD, "Ljava/lang/Object;", null,
          null);
      if (fv != null) {
        fv.visitEnd();
      }
    }
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
      String signature, String[] exceptions) {
    boolean isCtor = "<init>".equals(name);
    if (!isJavaScriptObject() && isCtor) {
      // Don't copy over constructors except for JavaScriptObject itself.
      return null;
    }
    if (!isCtor && !isStatic(access) && !isObjectMethod(name + desc)) {
      access |= Opcodes.ACC_STATIC;
      desc = HostedModeClassRewriter.addSyntheticThisParam(originalName, desc);
      name = name + "$";
    }
    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  private boolean isJavaScriptObject() {
    return originalName.equals(HostedModeClassRewriter.JAVASCRIPTOBJECT_DESC);
  }

  private boolean isObjectMethod(String signature) {
    return "java/lang/Object".equals(mapper.findOriginalDeclaringClass(originalName,
        signature));
  }

  private boolean isStatic(int access) {
    return (access & Opcodes.ACC_STATIC) != 0;
  }
}
