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

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.asm.ClassAdapter;
import com.google.gwt.dev.asm.ClassReader;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.ClassWriter;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.shell.JsValueGlue;
import com.google.gwt.dev.util.Name.BinaryName;
import com.google.gwt.dev.util.Name.InternalName;
import com.google.gwt.dev.util.collect.Lists;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class performs any and all byte code rewriting needed to make hosted
 * mode work. Currently, it performs the following rewrites:
 * <ol>
 * <li>Rewrites all native methods into non-native thunks to call JSNI via
 * {@link com.google.gwt.dev.shell.JavaScriptHost}.</li>
 * <li>JavaScriptObject and its subtypes gain a static {@value #REWRAP_METHOD}
 * method and new constructor methods.</li>
 * <li>All uses of <code>==</code> object comparisons gain an explicit cast to
 * Object to force canonicalization.</li>
 * <li>Casts and assignments to Object, JavaScriptObject, and interface types
 * have runtime type-fitting code added to support the not-really-a-type
 * semantic.</li>
 * <li>All interfaces that a JSO may implement have an adjunct class generated
 * that aids the SingleJsoImpl cast operations.</li>
 * </ol>
 * 
 * @see RewriteJsniMethods
 * @see WriteJsoImpl
 * @see RewriteObjectComparisons
 * @see RewriteJsoCasts
 * @see WriteSingleJsoSupportCode
 */
public class HostedModeClassRewriter {

  /**
   * An oracle to answer queries from the rewriting passes about the
   * type-system.
   */
  static class RewriterOracle {
    /**
     * Used by {@link #getConstructorDisambiguator}.
     */
    private static final String[] EMPTY_STRING = new String[0];

    private final Map<String, String[]> arrayDisambiguations = new HashMap<String, String[]>();

    private final JClassType jsoType;

    /**
     * The TypeOracle for the module being rewritten. We need the typeOracle to
     * provide a live view of the module's type-system, since Generators may add
     * new JSO types.
     */
    private final TypeOracle typeOracle;

    public RewriterOracle(TypeOracle typeOracle) {
      this.typeOracle = typeOracle;
      jsoType = typeOracle.findType(JsValueGlue.JSO_CLASS);
      assert jsoType != null : "No JavaScriptObject type";
    }

    /**
     * Returns <code>true</code> if a given type is potentially assignable from
     * a JSO subtype.
     */
    public boolean couldContainJso(String internalName) {
      return "java/lang/Object".equals(internalName)
          || JAVASCRIPTOBJECT_DESC.equals(internalName)
          || isJsoOrSubtype(internalName)
          || (!internalName.startsWith("java/") && isInterface(internalName));
    }

    public String[] getAllSuperInterfaces(String[] interfaces) {
      Set<String> toReturn = new HashSet<String>();
      Set<JClassType> seen = new HashSet<JClassType>();
      List<JClassType> queue = new LinkedList<JClassType>();

      for (String intf : interfaces) {
        JClassType type = typeOracle.findType(InternalName.toSourceName(intf));
        if (type == null) {
          throw new RuntimeException("Unknown type " + intf);
        }
        queue.add(type);
      }

      while (!queue.isEmpty()) {
        JClassType intf = queue.remove(0);
        if (seen.contains(intf)) {
          continue;
        }
        seen.add(intf);
        toReturn.add(BinaryName.toInternalName(intf.getQualifiedBinaryName()));
        queue.addAll(Arrays.asList(intf.getImplementedInterfaces()));
      }

      String[] array = toReturn.toArray(new String[toReturn.size()]);
      Arrays.sort(array);
      return array;
    }

    /**
     * In order to be able to upcast all JSO subtype arrays to JavaScriptObject
     * arrays, it is necessary to be able to maintain distinguishing method
     * descriptors. If the given descriptor will be affected by the array upcast
     * transformation, this method will return an array containing the internal
     * names of any additional types that should be appended to the descriptor.
     */
    public String[] getArrayDisambiguator(String desc) {
      String[] cached = arrayDisambiguations.get(desc);
      if (cached != null) {
        return cached;
      }

      List<String> toReturn = Lists.create();
      for (Type t : Type.getArgumentTypes(desc)) {
        if (t.getSort() == Type.ARRAY
            && t.getElementType().getSort() == Type.OBJECT) {
          String leafName = t.getElementType().getInternalName();
          if (isJsoOrSubtype(leafName)) {
            String disambiguatotr = DISAMBIGUATOR_TYPE_INTERNAL_NAME + "$"
                + InternalName.toIdentifier(leafName);
            toReturn = Lists.add(toReturn, disambiguatotr);
          }
        }
      }

      String[] array;
      if (toReturn.isEmpty()) {
        array = EMPTY_STRING;
      } else {
        array = toReturn.toArray(new String[toReturn.size()]);
      }
      arrayDisambiguations.put(desc, array);

      return array;
    }

    public boolean isInterface(String internalName) {
      String sourceName = InternalName.toSourceName(internalName);
      if (sourceName.contains("$")) {
        // Anonymous type
        return false;
      }
      JClassType type = typeOracle.findType(sourceName);
      return type != null && type.isInterface() != null;
    }

    public boolean isJsoOrSubtype(String internalName) {
      String sourceName = InternalName.toSourceName(internalName);
      if (sourceName.contains("$")) {
        // Anonymous type
        return false;
      }
      JClassType type = typeOracle.findType(sourceName);
      return type != null && jsoType.isAssignableFrom(type);
    }

    public boolean isTagInterface(String internalName) {
      String sourceName = InternalName.toSourceName(internalName);
      if (sourceName.contains("$")) {
        // Anonymous type
        return false;
      }
      JClassType type = typeOracle.findType(sourceName);
      return type != null && type.isInterface() != null
          && type.getOverridableMethods().length == 0;
    }

    /**
     * Returns <code>true</code> if a JSO assigned to a slot/field of this type
     * requires canonicalizing the JSO wrapper.
     */
    public boolean jsoAssignmentRequiresCanonicalization(String internalName) {
      return "java/lang/Object".equals(internalName);
    }
  }

  /*
   * Note: this rewriter operates on a class-by-class basis and has no global
   * view on the entire system. However, its operation requires certain global
   * state information. Therefore, all such global state must be passed into the
   * constructor.
   */

  /**
   * Used in CompilingClassLoader to trigger a call to
   * {@link #writeConstructorDisambiguationType}.
   */
  public static final String DISAMBIGUATOR_TYPE_NAME = "com.google.gwt.dev.shell.rewrite.$Disambiguator";

  /**
   * Used in CompilingClassLoader to trigger a call to
   * {@link #writeSingleJsoImplAdjunct}.
   */
  public static final String SINGLE_JSO_IMPL_ADJUNCT_SUFFIX = "$$singleJsoImpl";

  static final String CANONICAL_FIELD = "canonicalJso";

  static final String DISAMBIGUATOR_TYPE_INTERNAL_NAME = BinaryName.toInternalName(DISAMBIGUATOR_TYPE_NAME);

  static final boolean EXTRA_DEBUG_DATA = Boolean.getBoolean("gwt.dev.classDump");

  static final String JAVASCRIPTOBJECT_DESC = BinaryName.toInternalName(JsValueGlue.JSO_CLASS);

  static final String REFERENCE_FIELD = JsValueGlue.HOSTED_MODE_REFERENCE;

  static final String REWRAP_METHOD = "$rewrap";

  static final String SINGLE_JSO_IMPL_FIELD = "$singleJsoImpl";

  static final String SINGLE_JSO_IMPL_CAST_METHOD = "cast$";

  static final String SINGLE_JSO_IMPL_CAST_TO_OBJECT_METHOD = "castToObject$";

  static final String SINGLE_JSO_IMPL_INSTANCEOF_METHOD = "instanceOf$";

  static final String SINGLE_JSO_IMPL_SUPPORT_CLASS = Type.getInternalName(SingleJsoImplSupport.class);

  static final int SYSTEM_CLASS_VERSION = Double.parseDouble(System.getProperty("java.class.version")) < Opcodes.V1_6
      ? Opcodes.V1_5 : Opcodes.V1_6;

  /**
   * Passed into the rewriting visitors.
   */
  private final RewriterOracle rewriterOracle;

  /**
   * Creates a new {@link HostedModeClassRewriter}.
   * 
   * @param typeOracle The TypeOracle for the GWT module that is being rewritten
   */
  public HostedModeClassRewriter(TypeOracle typeOracle) {
    rewriterOracle = new RewriterOracle(typeOracle);
  }

  /**
   * Performs rewriting transformations on a class.
   * 
   * @param className the name of the class
   * @param classBytes the bytes of the class
   * @param anonymousClassMap a map between the anonymous class names of java
   *          compiler used to compile code and jdt. Emma-specific.
   */
  public byte[] rewrite(String className, byte[] classBytes,
      Map<String, String> anonymousClassMap) {
    classBytes = maybeUpgradeBytecode(classBytes);
    String desc = BinaryName.toInternalName(className);

    // The ASM model is to chain a bunch of visitors together.
    ClassWriter writer = new ClassWriter(0);
    ClassVisitor v = writer;

    // v = new CheckClassAdapter(v);
    // v = new TraceClassVisitor(v, new PrintWriter(System.out));

    v = new WriteSingleJsoSupportCode(v, rewriterOracle);

    v = new RewriteJsoCasts(v, rewriterOracle);

    v = new RewriteJsoArrays(v, rewriterOracle);

    v = new RewriteObjectComparisons(v, rewriterOracle);

    if (rewriterOracle.isJsoOrSubtype(desc)) {
      v = WriteJsoImpl.create(v, desc);
    }

    v = new RewriteJsniMethods(v, anonymousClassMap);

    if (SYSTEM_CLASS_VERSION < Opcodes.V1_6) {
      v = new ForceClassVersion15(v);
    }

    // We need EXPAND_FRAMES here for RewriteJsoCasts
    new ClassReader(classBytes).accept(v, ClassReader.EXPAND_FRAMES);
    return writer.toByteArray();
  }

  /**
   * Synthesize a class file that is used to disambiguate constructors that have
   * JSO subtype array parameters that have been upcast to JavaScriptObject
   * arrays.
   */
  public byte[] writeConstructorDisambiguationType(String className) {
    // Keep the synthetic class creation code next to where its consumed
    return RewriteJsoArrays.writeConstructorDisambiguationType(className);
  }

  /**
   * Synthesize a class file that implements an interface's SingleJsoImpl
   * adjunct type.
   */
  public byte[] writeSingleJsoImplAdjunct(String className) {
    // Keep the synthetic class creation code next to where its consumed
    return WriteSingleJsoSupportCode.writeSingleJsoImplAdjunct(className);
  }

  /**
   * AnalyzerAdapter, and thus RewriteJsoCasts, requires StackFrameMap data
   * which is new in version 50 (Java 1.6) bytecode. This method will only do
   * work if the input bytecode is less than version 50. This would occur when
   * running on Java 1.5 or if Emma support is enabled (since Emma is from
   * 2005).
   */
  private byte[] maybeUpgradeBytecode(byte[] classBytes) {
    // Major version is stored at offset 7 in the class file format
    if (classBytes[7] < Opcodes.V1_6) {
      // Get ASM to generate the stack frame data
      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
      ClassVisitor v2 = cw;

      // Upgrade to version 50 class format
      v2 = new ClassAdapter(v2) {
        @Override
        public void visit(int version, int access, String name,
            String signature, String superName, String[] interfaces) {
          super.visit(Opcodes.V1_6, access, name, signature, superName,
              interfaces);
        }
      };

      new ClassReader(classBytes).accept(v2, 0);
      classBytes = cw.toByteArray();
    }
    return classBytes;
  }
}
