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

import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.asm.ClassReader;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.ClassWriter;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.commons.Method;
import com.google.gwt.dev.shell.JsValueGlue;
import com.google.gwt.dev.util.log.speedtracer.DevModeEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * This class performs any and all byte code rewriting needed to make hosted
 * mode work. Currently, it performs the following rewrites:
 * <ol>
 * <li>Rewrites all native methods into non-native thunks to call JSNI via
 * {@link com.google.gwt.dev.shell.JavaScriptHost}.</li>
 * <li>Rewrites all JSO types into an interface type (which retains the original
 * name) and an implementation type (which has a $ appended).</li>
 * <li>All JSO interface types are empty and mirror the original type hierarchy.
 * </li>
 * <li>All JSO impl types contain the guts of the original type, except that all
 * instance methods are reimplemented as statics.</li>
 * <li>Calls sites to JSO types rewritten to dispatch to impl types. Any virtual
 * calls are also made static. Static field references to JSO types reference
 * static fields in the the impl class.</li>
 * <li>JavaScriptObject$ implements all the interface types and is the only
 * instantiable type.</li>
 * </ol>
 * 
 * @see RewriteJsniMethods
 * @see RewriteRefsToJsoClasses
 * @see WriteJsoInterface
 * @see WriteJsoImpl
 */
public class HostedModeClassRewriter {
  /*
   * Note: this rewriter operates on a class-by-class basis and has no global
   * view on the entire system. However, its operation requires certain global
   * state information. Therefore, all such global state must be passed into the
   * constructor.
   */

  /**
   * Maps instance methods to the class in which they are declared. This must be
   * provided by the caller since it requires global program state.
   */
  public interface InstanceMethodOracle {

    /**
     * For a given instance method and declared enclosing class (which must be a
     * JSO subtype), find the class in which that method was originally
     * declared. Methods declared on Object will return "java/lang/Object".
     * Static methods will always return <code>declaredClass</code>.
     * 
     * @param declaredClass a descriptor of the static type of the qualifier
     * @param signature the binary signature of the method
     * @return the descriptor of the class in which that method was declared,
     *         which will either be <code>declaredClass</code> or some
     *         superclass
     * @throws IllegalArgumentException if the method could not be found
     */
    String findOriginalDeclaringClass(String declaredClass, String signature);
  }

  /**
   * Contains data about how SingleJsoImpl methods are to be dispatched.
   */
  public interface SingleJsoImplData {
    /**
     * Returns the method declarations that should be generated for a given
     * mangled method name. {@link #getDeclarations} and
     * {@link #getImplementations} maintain a pairwise mapping.
     */
    List<Method> getDeclarations(String mangledName);

    /**
     * Returns the implementations that the method declarations above should
     * delegate to.{@link #getDeclarations} and {@link #getImplementations}
     * maintain a pairwise mapping.
     */
    List<Method> getImplementations(String mangledName);

    /**
     * Returns all of the mangled method names for SingleJsoImpl methods.
     */
    SortedSet<String> getMangledNames();

    /**
     * Returns the internal names of all interface types implemented by JSOs.
     */
    Set<String> getSingleJsoIntfTypes();
  }

  static final String JAVASCRIPTOBJECT_DESC = JsValueGlue.JSO_CLASS.replace(
      '.', '/');

  static final String JAVASCRIPTOBJECT_IMPL_DESC = JsValueGlue.JSO_IMPL_CLASS.replace(
      '.', '/');

  static final String REFERENCE_FIELD = JsValueGlue.HOSTED_MODE_REFERENCE;

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
  private final Set<String> jsoImplDescs;

  /**
   * An unmodifiable set of descriptors containing the interface form of
   * <code>JavaScriptObject</code> and all subclasses.
   */
  private final Set<String> jsoIntfDescs;

  private final SingleJsoImplData jsoData;

  /**
   * Records the superclass of every JSO for generating empty JSO interfaces.
   */
  private final Map<String, List<String>> jsoSuperDescs;

  /**
   * Maps methods to the class in which they are declared.
   */
  private InstanceMethodOracle mapper;

  /**
   * Creates a new {@link HostedModeClassRewriter} for a specified set of
   * subclasses of JavaScriptObject.
   * 
   * @param jsoSubtypes a set of binary type names representing JavaScriptObject
   *          and all of its subtypes of
   * @param mapper maps methods to the class in which they are declared
   */
  public HostedModeClassRewriter(Set<String> jsoSubtypes,
      Map<String, List<String>> jsoSuperTypes, SingleJsoImplData jsoData,
      InstanceMethodOracle mapper) {
    Set<String> buildJsoIntfDescs = new HashSet<String>();
    Set<String> buildJsoImplDescs = new HashSet<String>();
    Map<String, List<String>> buildJsoSuperDescs = new HashMap<String, List<String>>();
    for (String jsoSubtype : jsoSubtypes) {
      String desc = toDescriptor(jsoSubtype);
      buildJsoIntfDescs.add(desc);
      buildJsoImplDescs.add(desc + "$");

      List<String> superTypes = jsoSuperTypes.get(jsoSubtype);
      assert (superTypes != null);
      assert (superTypes.size() > 0);
      for (ListIterator<String> i = superTypes.listIterator(); i.hasNext();) {
        i.set(toDescriptor(i.next()));
      }
      buildJsoSuperDescs.put(desc, Collections.unmodifiableList(superTypes));
    }

    this.jsoIntfDescs = Collections.unmodifiableSet(buildJsoIntfDescs);
    this.jsoImplDescs = Collections.unmodifiableSet(buildJsoImplDescs);
    this.jsoSuperDescs = Collections.unmodifiableMap(buildJsoSuperDescs);
    this.jsoData = jsoData;
    this.mapper = mapper;
  }

  /**
   * Returns <code>true</code> if the class is the implementation class for a
   * JSO subtype.
   */
  public boolean isJsoImpl(String className) {
    return jsoImplDescs.contains(toDescriptor(className));
  }

  /**
   * Returns <code>true</code> if the class is the interface class for a JSO
   * subtype.
   */
  public boolean isJsoIntf(String className) {
    return jsoIntfDescs.contains(toDescriptor(className));
  }

  /**
   * Performs rewriting transformations on a class.
   * 
   * @param typeOracle a typeOracle modeling the user classes
   * @param className the name of the class
   * @param classBytes the bytes of the class
   * @param anonymousClassMap a map between the anonymous class names of java
   *          compiler used to compile code and jdt. Emma-specific.
   */
  public byte[] rewrite(TypeOracle typeOracle, String className,
      byte[] classBytes, Map<String, String> anonymousClassMap) {
    Event classBytesRewriteEvent =
        SpeedTracerLogger.start(DevModeEventType.CLASS_BYTES_REWRITE, "Class Name", className);
    String desc = toDescriptor(className);
    assert (!jsoIntfDescs.contains(desc));

    // The ASM model is to chain a bunch of visitors together.
    ClassWriter writer = new ClassWriter(0);
    ClassVisitor v = writer;

    // v = new CheckClassAdapter(v);
    // v = new TraceClassVisitor(v, new PrintWriter(System.out));

    v = new UseMirroredClasses(v, className);
    
    v = new RewriteSingleJsoImplDispatches(v, typeOracle, jsoData);

    v = new RewriteRefsToJsoClasses(v, jsoIntfDescs, mapper);

    if (jsoImplDescs.contains(desc)) {
      v = WriteJsoImpl.create(v, desc, jsoIntfDescs, mapper, jsoData);
    }

    v = new RewriteJsniMethods(v, anonymousClassMap);

    if (Double.parseDouble(System.getProperty("java.class.version")) < Opcodes.V1_6) {
      v = new ForceClassVersion15(v);
    }

    new ClassReader(classBytes).accept(v, 0);
    classBytesRewriteEvent.end();
    return writer.toByteArray();
  }

  public byte[] writeJsoIntf(String className) {
    String desc = toDescriptor(className);
    assert (jsoIntfDescs.contains(desc));
    assert (jsoSuperDescs.containsKey(desc));
    List<String> superDescs = jsoSuperDescs.get(desc);
    assert (superDescs != null);
    assert (superDescs.size() > 0);

    // The ASM model is to chain a bunch of visitors together.
    ClassWriter writer = new ClassWriter(0);
    ClassVisitor v = writer;

    // v = new CheckClassAdapter(v);
    // v = new TraceClassVisitor(v, new PrintWriter(System.out));

    String[] interfaces;
    // TODO(bov): something better than linear?
    if (superDescs.contains("java/lang/Object")) {
      interfaces = null;
    } else {
      interfaces = superDescs.toArray(new String[superDescs.size()]);
    }
    v.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE, desc,
        null, "java/lang/Object", interfaces);
    v.visitEnd();
    return writer.toByteArray();
  }
}
