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

import com.google.gwt.dev.asm.ClassReader;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.ClassWriter;
import com.google.gwt.dev.shell.JsValueGlue;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class performs any and all byte code rewriting needed to make hosted
 * mode work. Currently, it performs the following rewrites:
 * <ol>
 * <li>Rewrites all JSO types into an interface type (which retains the
 * original name) and an implementation type (which has a $ appended).</li>
 * <li>All JSO interface types are empty and mirror the original type
 * hierarchy.</li>
 * <li>All JSO impl types contain the guts of the original type, except that
 * all instance methods are reimplemented as statics.</li>
 * <li>Calls sites to JSO types rewritten to dispatch to impl types. Any
 * virtual calls are also made static. Static field references to JSO types
 * reference static fields in the the impl class.</li>
 * <li>JavaScriptObject$ implements all the interface types and is the only
 * instantiable type.</li>
 * </ol>
 * 
 * @see RewriteRefsToJsoClasses
 * @see WriteJsoInterface
 * @see WriteJsoImpl
 */
public class HostedModeClassRewriter {

  /**
   * Maps instance methods to the class in which they are declared.
   */
  public interface InstanceMethodMapper {

    /**
     * For a given instance method and declared qualifying class, find the class
     * in which that method was first declared. Methods declared on Object will
     * return "java/lang/Object". Static methods will always return
     * <code>declaredClass</code>.
     * 
     * @param desc a descriptor of the static type of the qualifier
     * @param signature the binary signature of the method
     * @return the descriptor of the class in which that method was declared
     * @throws IllegalArgumentException if the method could not be found
     */
    String findDeclaringClass(String desc, String signature);
  }

  static final String REFERENCE_FIELD = JsValueGlue.HOSTED_MODE_REFERENCE;

  static final String JAVASCRIPTOBJECT_DESC = JsValueGlue.JSO_CLASS.replace(
      '.', '/');

  static final String JAVASCRIPTOBJECT_IMPL_DESC = JsValueGlue.JSO_IMPL_CLASS.replace(
      '.', '/');;

  static String addSyntheticThisParam(String owner, String methodDescriptor) {
    return "(L" + owner + ";" + methodDescriptor.substring(1);
  }

  private static String toDescriptor(String jsoSubtype) {
    return jsoSubtype.replace('.', '/');
  }

  /**
   * An unmodifiable set of descriptors containing the implementation form of
   * <code>JavaScriptObject</code> and all subclasses.
   */
  private final Set<String> jsoImplDescriptors;

  /**
   * An unmodifiable set of descriptors containing the interface form of
   * <code>JavaScriptObject</code> and all subclasses.
   */
  private final Set<String> jsoIntfDescriptors;

  /**
   * Maps methods to the class in which they are declared.
   */
  private InstanceMethodMapper mapper;

  /**
   * Creates a new {@link HostedModeClassRewriter} for a specified set of
   * subclasses of JavaScriptObject.
   * 
   * @param jsoSubtypes a set of binary type names representing JavaScriptObject
   *          and all of its subtypes of
   * @param mapper maps methods to the class in which they are declared
   */
  public HostedModeClassRewriter(Set<String> jsoSubtypes,
      InstanceMethodMapper mapper) {
    Set<String> buildJsoIntfDescriptors = new HashSet<String>();
    Set<String> buildJsoImplDescriptors = new HashSet<String>();
    for (String jsoSubtype : jsoSubtypes) {
      String desc = toDescriptor(jsoSubtype);
      buildJsoIntfDescriptors.add(desc);
      buildJsoImplDescriptors.add(desc + "$");
    }
    this.jsoIntfDescriptors = Collections.unmodifiableSet(buildJsoIntfDescriptors);
    this.jsoImplDescriptors = Collections.unmodifiableSet(buildJsoImplDescriptors);
    this.mapper = mapper;
  }

  /**
   * Returns <code>true</code> if the class is the implementation class for a
   * JSO subtype.
   */
  public boolean isJsoImpl(String className) {
    return jsoImplDescriptors.contains(toDescriptor(className));
  }

  /**
   * Returns <code>true</code> if the class is the interface class for a JSO
   * subtype.
   */
  public boolean isJsoIntf(String className) {
    return jsoIntfDescriptors.contains(toDescriptor(className));
  }

  /**
   * Performs rewriting transformations on a class.
   * 
   * @param className the name of the class
   * @param classBytes the bytes of the class
   */
  public byte[] rewrite(String className, byte[] classBytes) {
    String desc = toDescriptor(className);

    // The ASM model is to chain a bunch of visitors together.
    ClassWriter writer = new ClassWriter(0);
    ClassVisitor v = writer;

    // v = new CheckClassAdapter(v);
    // v = new TraceClassVisitor(v, new PrintWriter(System.out));

    v = new RewriteRefsToJsoClasses(v, jsoIntfDescriptors, mapper);

    if (jsoImplDescriptors.contains(desc)) {
      v = new WriteJsoImpl(v, jsoIntfDescriptors, mapper);
    } else if (jsoIntfDescriptors.contains(desc)) {
      v = new WriteJsoInterface(v);
    }

    new ClassReader(classBytes).accept(v, 0);
    return writer.toByteArray();
  }
}
