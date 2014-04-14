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
package com.google.gwt.dev.shell;

import com.google.gwt.core.client.GWTBridge;
import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationProblemReporter;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.CompiledClass;
import com.google.gwt.dev.javac.JsniMethod;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.shell.rewrite.HasAnnotation;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.InstanceMethodOracle;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.SingleJsoImplData;
import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.dev.util.Name;
import com.google.gwt.dev.util.Name.InternalName;
import com.google.gwt.dev.util.Name.SourceOrBinaryName;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.log.speedtracer.DevModeEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.MapMaker;
import com.google.gwt.thirdparty.guava.common.primitives.Primitives;
import com.google.gwt.util.tools.Utility;

import java.beans.Beans;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An isolated {@link ClassLoader} for running all user code. All user files are
 * compiled from source code byte a {@link ByteCodeCompiler}. After compilation,
 * some byte code rewriting is performed to support
 * <code>JavaScriptObject</code> and its subtypes.
 *
 * TODO: we should refactor this class to move the getClassInfoByDispId,
 * getDispId, getMethodDispatch and putMethodDispatch into a separate entity
 * since they really do not interact with the CompilingClassLoader
 * functionality.
 */
public final class CompilingClassLoader extends ClassLoader implements
    DispatchIdOracle {

  /**
   * Oracle that can answer questions about {@link DispatchClassInfo
   * DispatchClassInfos}.
   */
  private final class DispatchClassInfoOracle {

    /**
     * Class identifier to DispatchClassInfo mapping.
     */
    private final ArrayList<DispatchClassInfo> classIdToClassInfo = new ArrayList<DispatchClassInfo>();

    /**
     * Binary or source class name to DispatchClassInfo map.
     */
    private final Map<String, DispatchClassInfo> classNameToClassInfo = new HashMap<String, DispatchClassInfo>();

    /**
     * Clears out the contents of this oracle.
     */
    public synchronized void clear() {
      classIdToClassInfo.clear();
      classNameToClassInfo.clear();
    }

    /**
     * Returns the {@link DispatchClassInfo} for a given dispatch id.
     *
     * @param dispId dispatch id
     * @return DispatchClassInfo for the requested dispatch id
     */
    public synchronized DispatchClassInfo getClassInfoByDispId(int dispId) {
      int classId = extractClassIdFromDispId(dispId);

      return classIdToClassInfo.get(classId);
    }

    /**
     * Returns the dispatch id for a given member reference. Member references
     * can be encoded as: "@class::field" or "@class::method(typesigs)".
     *
     * @param jsniMemberRef a string encoding a JSNI member to use
     * @return integer encoded as ((classId << 16) | memberId)
     */
    public synchronized int getDispId(String jsniMemberRef) {
      /*
       * Map JS toString() onto the Java toString() method.
       */
      if (jsniMemberRef.equals("toString")) {
        jsniMemberRef = "@java.lang.Object::toString()";
      }

      JsniRef parsed = JsniRef.parse(jsniMemberRef);
      if (parsed == null) {
        logger.log(TreeLogger.ERROR, "Malformed JSNI reference '"
            + jsniMemberRef + "'; expect subsequent failures",
            new NoSuchFieldError(jsniMemberRef));
        return -1;
      }

      // Do the lookup by class name.
      String className = parsed.className();
      DispatchClassInfo dispClassInfo = getClassInfoFromClassName(className);
      if (dispClassInfo != null) {
        String memberName = parsed.memberSignature();

        /*
         * Disallow the use of JSNI references to SingleJsoImpl interface
         * methods. This policy is due to web-mode dispatch implementation
         * details; resolving the JSNI reference wouldn't be just be a name
         * replacement, instead it would be necessary to significantly alter the
         * semantics of the hand-written JS.
         */
        if (singleJsoImplTypes.contains(canonicalizeClassName(className))) {
          logger.log(TreeLogger.ERROR,
              "Invalid JSNI reference to SingleJsoImpl interface (" + className
                  + "); consider using a trampoline. "
                  + "Expect subsequent failures.", new NoSuchFieldError(
                  jsniMemberRef));
          return -1;
        }

        int memberId = dispClassInfo.getMemberId(memberName);
        if (memberId < 0) {
          if (!className.startsWith("java.")) {
            logger.log(TreeLogger.ERROR, "Member '" + memberName
                + "' in JSNI reference '" + jsniMemberRef
                + "' could not be found; expect subsequent failures",
                new NoSuchFieldError(memberName));
          }
        }

        return synthesizeDispId(dispClassInfo.getClassId(), memberId);
      }

      logger.log(TreeLogger.ERROR, "Class '" + className
          + "' in JSNI reference '" + jsniMemberRef
          + "' could not be found; expect subsequent failures",
          new ClassNotFoundException(className));
      return -1;
    }

    /**
     * Extracts the class id from the dispatch id.
     *
     * @param dispId
     * @return the classId encoded into this dispatch id
     */
    private int extractClassIdFromDispId(int dispId) {
      return (dispId >> 16) & 0xffff;
    }

    /**
     * Returns the {@link java.lang.Class} instance for a given binary class
     * name. It is important to avoid initializing the class because this would
     * potentially cause initializers to be run in a different order than in web
     * mode. Moreover, we may not have injected all of the JSNI code required to
     * initialize the class.
     *
     * @param binaryClassName the binary name of a class
     * @return {@link java.lang.Class} instance or null if the given binary
     *         class name could not be found
     */
    private Class<?> getClassFromBinaryName(String binaryClassName) {
      int dims = 0;
      while (binaryClassName.endsWith("[]")) {
        dims++;
        binaryClassName = binaryClassName.substring(0,
            binaryClassName.length() - 2);
      }

      Class<?> clazz = primitiveTypes.get(binaryClassName);
      if (clazz == null) {
        try {
          clazz = Class.forName(binaryClassName, false,
              CompilingClassLoader.this);
        } catch (ClassNotFoundException e) {
        }
      }
      // TODO(deprecation): remove this support eventually.
      if (clazz == null && binaryClassName.length() == 1
          && "ZBCDFIJSV".indexOf(binaryClassName.charAt(0)) >= 0) {
        clazz = getDeprecatedPrimitiveType(binaryClassName.charAt(0));
        assert clazz != null;
      }
      if (dims > 0) {
        return Array.newInstance(clazz, new int[dims]).getClass();
      } else {
        return clazz;
      }
    }

    /**
     * Returns the {@link java.lang.Class} object for a class that matches the
     * source or binary name given.
     *
     * @param className binary or source name
     * @return {@link java.lang.Class} instance, if found, or null
     */
    private Class<?> getClassFromBinaryOrSourceName(String className) {
      // Try the type oracle first
      JClassType type = typeOracle.findType(SourceOrBinaryName.toSourceName(className));
      if (type != null) {
        // Use the type oracle to compute the exact binary name
        String jniSig = type.getJNISignature();
        jniSig = jniSig.substring(1, jniSig.length() - 1);
        className = InternalName.toBinaryName(jniSig);
      }
      return getClassFromBinaryName(className);
    }

    /**
     * Returns the {@link DispatchClassInfo} associated with the class name.
     * Since we allow both binary and source names to be used in JSNI class
     * references, we need to be able to deal with the fact that multiple
     * permutations of the class name with regards to source or binary forms map
     * on the same {@link DispatchClassInfo}.
     *
     * @param className binary or source name for a class
     * @return {@link DispatchClassInfo} associated with the binary or source
     *         class name; null if there is none
     */
    private DispatchClassInfo getClassInfoFromClassName(String className) {

      DispatchClassInfo dispClassInfo = classNameToClassInfo.get(className);
      if (dispClassInfo != null) {
        // return the cached value
        return dispClassInfo;
      }

      Class<?> cls = getClassFromBinaryOrSourceName(className);
      if (cls == null) {
        /*
         * default to return null; mask the specific error and let the caller
         * handle it
         */
        return null;
      }

      // Map JSO type references to the appropriate impl class.
      if (classRewriter.isJsoIntf(cls.getName())) {
        cls = getClassFromBinaryName(cls.getName() + "$");
      }

      /*
       * we need to create a new DispatchClassInfo since we have never seen this
       * class before under any source or binary class name
       */
      int classId = classIdToClassInfo.size();

      dispClassInfo = new DispatchClassInfo(cls, classId);
      classIdToClassInfo.add(dispClassInfo);

      /*
       * Whether we created a new DispatchClassInfo or not, we need to add a
       * mapping for this name
       */
      classNameToClassInfo.put(className, dispClassInfo);

      return dispClassInfo;
    }

    @Deprecated
    private Class<?> getDeprecatedPrimitiveType(char c) {
      switch (c) {
        case 'Z':
          return boolean.class;
        case 'B':
          return byte.class;
        case 'C':
          return char.class;
        case 'D':
          return double.class;
        case 'F':
          return float.class;
        case 'I':
          return int.class;
        case 'J':
          return long.class;
        case 'S':
          return short.class;
        case 'V':
          return void.class;
        default:
          return null;
      }
    }

    /**
     * Synthesizes a dispatch identifier for the given class and member ids.
     *
     * @param classId class index
     * @param memberId member index
     * @return dispatch identifier for the given class and member ids
     */
    private int synthesizeDispId(int classId, int memberId) {
      return (classId << 16) | memberId;
    }
  }

  /**
   * A ClassLoader that will delegate to a parent ClassLoader and fall back to
   * loading bytecode as resources from an alternate parent ClassLoader.
   */
  private static class MultiParentClassLoader extends ClassLoader {
    private final ClassLoader resources;

    public MultiParentClassLoader(ClassLoader parent, ClassLoader resources) {
      super(parent);
      assert parent != null;
      this.resources = resources;
    }

    @Override
    protected synchronized Class<?> findClass(String name)
        throws ClassNotFoundException {
      String resourceName = name.replace('.', '/') + ".class";
      URL url = resources.getResource(resourceName);
      if (url == null) {
        throw new ClassNotFoundException();
      }
      byte[] bytes = Util.readURLAsBytes(url);
      return defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      try {
        Class c = findLoadedClass(name);
        if (c != null) {
          if (resolve) {
            resolveClass(c);
          }
          return c;
        }
        return getParent().loadClass(name);
      } catch (Throwable t) {
        // Make a second attempt not only on ClassNotFoundExceptions, but also errors like
        // ClassCircularityError
        Class c = findClass(name);
        if (resolve) {
          resolveClass(c);
        }
        return c;
      }
    }
  }

  /**
   * Implements {@link InstanceMethodOracle} on behalf of the
   * {@link HostedModeClassRewriter}. Implemented using {@link TypeOracle}.
   */
  private class MyInstanceMethodOracle implements InstanceMethodOracle {

    private final Map<String, Set<JClassType>> signatureToDeclaringClasses = new HashMap<String, Set<JClassType>>();

    public MyInstanceMethodOracle(Set<JClassType> jsoTypes,
        JClassType javaLangObject, SingleJsoImplData jsoData) {

      // Record that the JSO implements its own methods
      for (JClassType type : jsoTypes) {
        for (JMethod method : type.getMethods()) {
          if (!method.isStatic()) {
            assert !method.isAbstract() : "Abstract method in JSO type "
                + method;
            add(type, method);
          }
        }
      }

      /*
       * Record the implementing types for methods defined in SingleJsoImpl
       * interfaces. We have to make this pass because of possible variance in
       * the return types between the abstract method declaration in the
       * interface and the concrete method.
       */
      for (String intfName : jsoData.getSingleJsoIntfTypes()) {
        // We only store the name in the data block to keep it lightweight
        JClassType intf = typeOracle.findType(Name.InternalName.toSourceName(intfName));
        JClassType jso = typeOracle.getSingleJsoImpl(intf);
        for (JMethod method : intf.getMethods()) {
          JClassType implementingJso = findImplementingTypeForMethod(jso,
              method);
          assert implementingJso != null : "Jso should contain method: "
              + method.getJsniSignature();
          add(implementingJso, method);
        }
      }

      // Object clobbers everything.
      for (JMethod method : javaLangObject.getMethods()) {
        if (!method.isStatic()) {
          String signature = createSignature(method);
          Set<JClassType> declaringClasses = new HashSet<JClassType>();
          signatureToDeclaringClasses.put(signature, declaringClasses);
          declaringClasses.add(javaLangObject);
        }
      }
    }

    @Override
    public String findOriginalDeclaringClass(String desc, String signature) {
      // Lookup the method.
      Set<JClassType> declaringClasses = signatureToDeclaringClasses.get(signature);
      assert declaringClasses != null : "No classes for " + signature;
      if (declaringClasses.size() == 1) {
        // Shortcut: if there's only one answer, it must be right.
        return createDescriptor(declaringClasses.iterator().next());
      }
      // Must check for assignability.
      String sourceName = desc.replace('/', '.');
      sourceName = sourceName.replace('$', '.');
      JClassType declaredType = typeOracle.findType(sourceName);

      // Check if I declare this directly.
      if (declaringClasses.contains(declaredType)) {
        return desc;
      }

      // Check to see what type I am assignable to.
      for (JClassType possibleSupertype : declaringClasses) {
        if (declaredType.isAssignableTo(possibleSupertype)) {
          return createDescriptor(possibleSupertype);
        }
      }
      throw new IllegalArgumentException("Could not resolve signature '"
          + signature + "' from class '" + desc + "'");
    }

    /**
     * Record that a given JSO type contains the concrete implementation of a
     * (possibly abstract) method.
     */
    private void add(JClassType type, JMethod method) {
      String signature = createSignature(method);
      Set<JClassType> declaringClasses = signatureToDeclaringClasses.get(signature);
      if (declaringClasses == null) {
        declaringClasses = new HashSet<JClassType>();
        signatureToDeclaringClasses.put(signature, declaringClasses);
      }
      declaringClasses.add(type);
    }

    private String createDescriptor(JClassType type) {
      String jniSignature = type.getJNISignature();
      return jniSignature.substring(1, jniSignature.length() - 1);
    }

    private String createSignature(JMethod method) {
      StringBuffer sb = new StringBuffer(method.getName());
      sb.append('(');
      for (JParameter param : method.getParameters()) {
        sb.append(param.getType().getJNISignature());
      }
      sb.append(')');
      sb.append(method.getReturnType().getJNISignature());
      String signature = sb.toString();
      return signature;
    }
  }

  /**
   * Cook up the data we need to support JSO subtypes that implement interfaces
   * with methods. This includes the set of SingleJsoImpl interfaces actually
   * implemented by a JSO type, the mangled method names, and the names of the
   * Methods that should actually implement the virtual functions.
   *
   * Given the current implementation of JSO$ and incremental execution of
   * rebinds, it's not possible for Generators to produce additional
   * JavaScriptObject subtypes, so this data can remain static.
   */
  private class MySingleJsoImplData implements SingleJsoImplData {
    private final SortedSet<String> mangledNames = new TreeSet<String>();
    private final Map<String, List<com.google.gwt.dev.asm.commons.Method>> mangledNamesToDeclarations = new HashMap<String, List<com.google.gwt.dev.asm.commons.Method>>();
    private final Map<String, List<com.google.gwt.dev.asm.commons.Method>> mangledNamesToImplementations = new HashMap<String, List<com.google.gwt.dev.asm.commons.Method>>();
    private final Set<String> unmodifiableIntfNames = Collections.unmodifiableSet(singleJsoImplTypes);
    private final SortedSet<String> unmodifiableNames = Collections.unmodifiableSortedSet(mangledNames);

    public MySingleJsoImplData() {
      // Loop over all interfaces with JSO implementations
      typeLoop : for (JClassType type : typeOracle.getSingleJsoImplInterfaces()) {
        assert type.isInterface() == type : "Expecting interfaces only";

        /*
         * By preemptively adding all possible mangled names by which a method
         * could be called, we greatly simplify the logic necessary to rewrite
         * the call-site.
         *
         * interface A {void m();}
         *
         * interface B extends A {void z();}
         *
         * becomes
         *
         * c_g_p_A_m() -> JsoA$.m$()
         *
         * c_g_p_B_m() -> JsoA$.m$()
         *
         * c_g_p_B_z() -> JsoB$.z$()
         */
        for (JMethod intfMethod : type.getOverridableMethods()) {
          assert intfMethod.isAbstract() : "Expecting only abstract methods";

          /*
           * It is necessary to locate the implementing type on a per-method
           * basis. Consider the case of
           *
           * @SingleJsoImpl interface C extends A, B {}
           *
           * Methods inherited from interfaces A and B must be dispatched to
           * their respective JSO implementations.
           */
          JClassType implementingType = typeOracle.getSingleJsoImpl(intfMethod.getEnclosingType());

          if (implementingType == null || implementingType.isAnnotationPresent(GwtScriptOnly.class)) {
            /*
             * This means that there is no concrete implementation of the
             * interface by a JSO. Any implementation that might be created by a
             * Generator won't be a JSO subtype, so we'll just ignore it as an
             * actionable type. Were Generators ever able to create new JSO
             * subtypes, we'd have to speculatively rewrite the callsite.
             */
            continue typeLoop;
          }

          /*
           * Record the type as being actionable.
           */
          singleJsoImplTypes.add(canonicalizeClassName(getBinaryName(type)));

          /*
           * The mangled name adds the current interface like
           *
           * com_foo_Bar_methodName
           */
          String mangledName = getBinaryName(type).replace('.', '_') + "_"
              + intfMethod.getName();
          mangledNames.add(mangledName);

          /*
           * Handle virtual overrides by finding the method that we would
           * normally invoke and using its declaring class as the dispatch
           * target.
           */
          JMethod implementingMethod;
          while ((implementingMethod = findOverloadUsingErasure(
              implementingType, intfMethod)) == null) {
            implementingType = implementingType.getSuperclass();
          }
          // implementingmethod and implementingType cannot be null here

          /*
           * Create a pseudo-method declaration for the interface method. This
           * should look something like
           *
           * ReturnType method$ (ParamType, ParamType)
           *
           * This must be kept in sync with the WriteJsoImpl class.
           */
          {
            String decl = getBinaryOrPrimitiveName(intfMethod.getReturnType().getErasedType())
                + " " + intfMethod.getName() + "(";
            for (JParameter param : intfMethod.getParameters()) {
              decl += ",";
              decl += getBinaryOrPrimitiveName(param.getType().getErasedType());
            }
            decl += ")";

            com.google.gwt.dev.asm.commons.Method declaration = com.google.gwt.dev.asm.commons.Method.getMethod(decl);
            addToMap(mangledNamesToDeclarations, mangledName, declaration);
          }

          /*
           * Cook up the a pseudo-method declaration for the concrete type. This
           * should look something like
           *
           * ReturnType method$ (JsoType, ParamType, ParamType)
           *
           * This must be kept in sync with the WriteJsoImpl class.
           */
          {
            String returnName = getBinaryOrPrimitiveName(implementingMethod.getReturnType().getErasedType());
            String jsoName = getBinaryOrPrimitiveName(implementingType);

            String decl = returnName + " " + intfMethod.getName() + "$ ("
                + jsoName;
            for (JParameter param : implementingMethod.getParameters()) {
              decl += ",";
              decl += getBinaryOrPrimitiveName(param.getType().getErasedType());
            }
            decl += ")";

            com.google.gwt.dev.asm.commons.Method toImplement = com.google.gwt.dev.asm.commons.Method.getMethod(decl);
            addToMap(mangledNamesToImplementations, mangledName, toImplement);
          }
        }
      }

      if (logger.isLoggable(TreeLogger.SPAM)) {
        TreeLogger dumpLogger = logger.branch(TreeLogger.SPAM,
            "SingleJsoImpl method mappings");
        for (Map.Entry<String, List<com.google.gwt.dev.asm.commons.Method>> entry : mangledNamesToImplementations.entrySet()) {
          dumpLogger.log(TreeLogger.SPAM, entry.getKey() + " -> " + entry.getValue());
        }
      }
    }

    @Override
    public List<com.google.gwt.dev.asm.commons.Method> getDeclarations(
        String mangledName) {
      List<com.google.gwt.dev.asm.commons.Method> toReturn = mangledNamesToDeclarations.get(mangledName);
      return toReturn == null ? null : Collections.unmodifiableList(toReturn);
    }

    @Override
    public List<com.google.gwt.dev.asm.commons.Method> getImplementations(
        String mangledName) {
      List<com.google.gwt.dev.asm.commons.Method> toReturn = mangledNamesToImplementations.get(mangledName);
      return toReturn == null ? toReturn
          : Collections.unmodifiableList(toReturn);
    }

    @Override
    public SortedSet<String> getMangledNames() {
      return unmodifiableNames;
    }

    @Override
    public Set<String> getSingleJsoIntfTypes() {
      return unmodifiableIntfNames;
    }

    /**
     * Assumes that the usual case is a 1:1 mapping.
     */
    private <K, V> void addToMap(Map<K, List<V>> map, K key, V value) {
      List<V> list = map.get(key);
      if (list == null) {
        map.put(key, Lists.create(value));
      } else {
        List<V> maybeOther = Lists.add(list, value);
        if (maybeOther != list) {
          map.put(key, maybeOther);
        }
      }
    }

    /**
     * Looks for a concrete implementation of <code>intfMethod</code> in
     * <code>implementingType</code>.
     */
    private JMethod findOverloadUsingErasure(JClassType implementingType,
        JMethod intfMethod) {

      int numParams = intfMethod.getParameters().length;
      JType[] erasedTypes = new JType[numParams];
      for (int i = 0; i < numParams; i++) {
        erasedTypes[i] = intfMethod.getParameters()[i].getType().getErasedType();
      }

      outer : for (JMethod method : implementingType.getOverloads(intfMethod.getName())) {
        JParameter[] params = method.getParameters();
        if (params.length != numParams) {
          continue;
        }
        for (int i = 0; i < numParams; i++) {
          if (params[i].getType().getErasedType() != erasedTypes[i]) {
            continue outer;
          }
        }
        return method;
      }
      return null;
    }
  }

  /**
   * Only loads bootstrap classes, specifically excluding classes from the classpath.
   */
  private static final ClassLoader bootstrapClassLoader = new ClassLoader(null) { };

  /**
   * The names of the bridge classes.
   */
  private static final Map<String, Class<?>> BRIDGE_CLASS_NAMES = new HashMap<String, Class<?>>();

  /**
   * The set of classes exposed into user space that actually live in hosted
   * space (thus, they bridge across the spaces).
   */
  private static final Class<?>[] BRIDGE_CLASSES = new Class<?>[]{
      // Have to include the shared GWTBridge class since the client one
      // inherits from it, otherwise we get verify errors
      ShellJavaScriptHost.class, GWTBridge.class, com.google.gwt.core.shared.GWTBridge.class};

  private static final boolean CLASS_DUMP = Boolean.getBoolean("gwt.dev.classDump");

  private static final String CLASS_DUMP_PATH = System.getProperty(
      "gwt.dev.classDumpPath", "rewritten-classes");

  private static boolean emmaAvailable = false;

  private static EmmaStrategy emmaStrategy;

  /**
   * Caches the byte code for {@link JavaScriptHost}.
   */
  private static byte[] javaScriptHostBytes;

  private static final Map<String, Class<?>> primitiveTypes;

  static {
    ImmutableMap.Builder<String, Class<?>> builder = ImmutableMap.builder();
    for (Class<?> klass : Primitives.allPrimitiveTypes()) {
      builder.put(klass.getSimpleName(), klass);
    }
    primitiveTypes = builder.build();
  }

  static {
    for (Class<?> c : BRIDGE_CLASSES) {
      BRIDGE_CLASS_NAMES.put(c.getName(), c);
    }
    /*
     * Specific support for bridging to Emma since the user classloader is
     * generally completely isolated.
     *
     * We are looking for a specific emma class "com.vladium.emma.rt.RT". If
     * that changes in the future, this code would need to be updated as well.
     */
    try {
      Class<?> emmaBridge = Class.forName(EmmaStrategy.EMMA_RT_CLASSNAME,
          false, Thread.currentThread().getContextClassLoader());
      BRIDGE_CLASS_NAMES.put(EmmaStrategy.EMMA_RT_CLASSNAME, emmaBridge);
      emmaAvailable = true;
    } catch (ClassNotFoundException ignored) {
    }
    emmaStrategy = EmmaStrategy.get(emmaAvailable);
  }

  private static void classDump(String name, byte[] bytes) {
    String packageName, className;
    int pos = name.lastIndexOf('.');
    if (pos < 0) {
      packageName = "";
      className = name;
    } else {
      packageName = name.substring(0, pos);
      className = name.substring(pos + 1);
    }

    File dir = new File(CLASS_DUMP_PATH + File.separator
        + packageName.replace('.', File.separatorChar));
    if (!dir.exists()) {
      // No need to check mkdirs result because an IOException will occur anyway
      dir.mkdirs();
    }

    File file = new File(dir, className + ".class");
    FileOutputStream fileOutput = null;
    try {
      fileOutput = new FileOutputStream(file);
      fileOutput.write(bytes);
      fileOutput.close();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (fileOutput != null) {
        try {
          fileOutput.close();
        } catch (IOException e) {
          // oh well, we tried
        }
      }
    }
  }

  /**
   * Magic: {@link JavaScriptHost} was never compiled because it's a part of the
   * hosted mode infrastructure. However, unlike {@link #BRIDGE_CLASSES},
   * {@code JavaScriptHost} needs a separate copy per inside the ClassLoader for
   * each module.
   */
  private static void ensureJavaScriptHostBytes(TreeLogger logger)
      throws UnableToCompleteException {

    if (javaScriptHostBytes != null) {
      return;
    }

    String className = JavaScriptHost.class.getName();
    try {
      String path = className.replace('.', '/') + ".class";
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      URL url = cl.getResource(path);
      if (url != null) {
        javaScriptHostBytes = getClassBytesFromStream(url.openStream());
      } else {
        logger.log(TreeLogger.ERROR,
            "Could not find required bootstrap class '" + className
                + "' in the classpath", null);
        throw new UnableToCompleteException();
      }
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR,
          "Error reading class bytes for " + className, e);
      throw new UnableToCompleteException();
    }
  }

  private static JClassType findImplementingTypeForMethod(JClassType type,
      JMethod method) {
    JType[] methodParamTypes = method.getErasedParameterTypes();
    while (type != null) {
      for (JMethod candidate : type.getMethods()) {
        if (hasMatchingErasedSignature(method, methodParamTypes, candidate)) {
          return type;
        }
      }
      type = type.getSuperclass();
    }
    return null;
  }

  private static byte[] getClassBytesFromStream(InputStream is)
      throws IOException {
    try {
      byte classBytes[] = new byte[is.available()];
      int read = 0;
      while (read < classBytes.length) {
        read += is.read(classBytes, read, classBytes.length - read);
      }
      return classBytes;
    } finally {
      Utility.close(is);
    }
  }

  private static boolean hasMatchingErasedSignature(JMethod a,
      JType[] aParamTypes, JMethod b) {
    if (!a.getName().equals(b.getName())) {
      return false;
    }

    JType[] bParamTypes = b.getErasedParameterTypes();
    if (aParamTypes.length != bParamTypes.length) {
      return false;
    }

    for (int i = 0; i < aParamTypes.length; ++i) {
      if (aParamTypes[i] != bParamTypes[i]) {
        return false;
      }
    }

    return true;
  }

  /**
   * The set of units whose JSNI has already been injected.
   */
  private Set<CompilationUnit> alreadyInjected = new HashSet<CompilationUnit>();

  private final HostedModeClassRewriter classRewriter;

  private CompilationState compilationState;

  private final DispatchClassInfoOracle dispClassInfoOracle = new DispatchClassInfoOracle();

  private Class<?> gwtClass, javaScriptHostClass;

  /**
   * Used by {@link #findClass(String)} to prevent reentrant JSNI injection.
   */
  private boolean isInjectingClass = false;

  private final ReentrantLock loadLock = new ReentrantLock();

  private final TreeLogger logger;

  private final Set<String> scriptOnlyClasses = new HashSet<String>();

  private ClassLoader scriptOnlyClassLoader;

  private ShellJavaScriptHost shellJavaScriptHost;

  private final Set<String> singleJsoImplTypes = new HashSet<String>();

  /**
   * Used by {@link #findClass(String)} to prevent reentrant JSNI injection.
   */
  private Stack<CompilationUnit> toInject = new Stack<CompilationUnit>();

  private final TypeOracle typeOracle;

  private final Map<Object, Object> weakJavaWrapperCache = new MapMaker().weakKeys().weakValues().makeMap();

  private final Map<Integer, Object> weakJsoCache = new MapMaker().weakValues().makeMap();

  public CompilingClassLoader(TreeLogger logger,
      CompilationState compilationState, ShellJavaScriptHost javaScriptHost)
      throws UnableToCompleteException {
    super(null);
    this.logger = logger;
    this.compilationState = compilationState;
    this.shellJavaScriptHost = javaScriptHost;
    this.typeOracle = compilationState.getTypeOracle();

    // Assertions are always on in hosted mode.
    setDefaultAssertionStatus(true);

    ensureJavaScriptHostBytes(logger);

    // Create a class rewriter based on all the subtypes of the JSO class.
    JClassType jsoType = typeOracle.findType(JsValueGlue.JSO_CLASS);
    if (jsoType != null) {

      // Create a set of binary names.
      Set<JClassType> jsoTypes = new HashSet<JClassType>();
      JClassType[] jsoSubtypes = jsoType.getSubtypes();
      Collections.addAll(jsoTypes, jsoSubtypes);
      jsoTypes.add(jsoType);

      Set<String> jsoTypeNames = new HashSet<String>();
      Map<String, List<String>> jsoSuperTypes = new HashMap<String, List<String>>();
      for (JClassType type : jsoTypes) {
        List<String> types = new ArrayList<String>();
        types.add(getBinaryName(type.getSuperclass()));
        for (JClassType impl : type.getImplementedInterfaces()) {
          types.add(getBinaryName(impl));
        }

        String binaryName = getBinaryName(type);
        jsoTypeNames.add(binaryName);
        jsoSuperTypes.put(binaryName, types);
      }

      SingleJsoImplData singleJsoImplData = new MySingleJsoImplData();

      MyInstanceMethodOracle mapper = new MyInstanceMethodOracle(jsoTypes,
          typeOracle.getJavaLangObject(), singleJsoImplData);
      classRewriter = new HostedModeClassRewriter(jsoTypeNames, jsoSuperTypes,
          singleJsoImplData, mapper);
    } else {
      // If we couldn't find the JSO class, we don't need to do any rewrites.
      classRewriter = null;
    }
  }

  /**
   * Retrieves the mapped JSO for a given unique id, provided the id was
   * previously cached and the JSO has not been garbage collected.
   *
   * @param uniqueId the previously stored unique id
   * @return the mapped JSO, or <code>null</code> if the id was not previously
   *         mapped or if the JSO has been garbage collected
   */
  public Object getCachedJso(int uniqueId) {
    return weakJsoCache.get(uniqueId);
  }

  /**
   * Returns the {@link DispatchClassInfo} for a given dispatch id.
   *
   * @param dispId dispatch identifier
   * @return {@link DispatchClassInfo} for a given dispatch id or null if one
   *         does not exist
   */
  @Override
  public DispatchClassInfo getClassInfoByDispId(int dispId) {
    return dispClassInfoOracle.getClassInfoByDispId(dispId);
  }

  /**
   * Returns the dispatch id for a JSNI member reference.
   *
   * @param jsniMemberRef a JSNI member reference
   * @return dispatch id or -1 if the JSNI member reference could not be found
   */
  @Override
  public int getDispId(String jsniMemberRef) {
    return dispClassInfoOracle.getDispId(jsniMemberRef);
  }

  /**
   * Retrieves the mapped wrapper for a given Java Object, provided the wrapper
   * was previously cached and has not been garbage collected.
   *
   * @param javaObject the Object being wrapped
   * @return the mapped wrapper, or <code>null</code> if the Java object mapped
   *         or if the wrapper has been garbage collected
   */
  public Object getWrapperForObject(Object javaObject) {
    return weakJavaWrapperCache.get(javaObject);
  }

  /**
   * Weakly caches a given JSO by unique id. A cached JSO can be looked up by
   * unique id until it is garbage collected.
   *
   * @param uniqueId a unique id associated with the JSO
   * @param jso the value to cache
   */
  public void putCachedJso(int uniqueId, Object jso) {
    weakJsoCache.put(uniqueId, jso);
  }

  /**
   * Weakly caches a wrapper for a given Java Object.
   *
   * @param javaObject the Object being wrapped
   * @param wrapper the mapped wrapper
   */
  public void putWrapperForObject(Object javaObject, Object wrapper) {
    weakJavaWrapperCache.put(javaObject, wrapper);
  }

  @Override
  protected Class<?> findClass(String className) throws ClassNotFoundException {
    if (className == null) {
      throw new ClassNotFoundException("null class name",
          new NullPointerException());
    }

    if (className.equals("com.google.gwt.core.ext.debug.JsoEval")) {
      // In addition to the system ClassLoader, we let JsoEval be available
      // from this CompilingClassLoader in case that's where the debugger
      // happens to look.
      return ClassLoader.getSystemClassLoader().loadClass(className);
    }

    loadLock.lock();
    try {

      if (scriptOnlyClasses.contains(className)) {
        // Allow the child ClassLoader to handle this
        throw new ClassNotFoundException();
      }

      // Check for a bridge class that spans hosted and user space.
      if (BRIDGE_CLASS_NAMES.containsKey(className)) {
        return BRIDGE_CLASS_NAMES.get(className);
      }

      // Get the bytes, compiling if necessary.
      byte[] classBytes = findClassBytes(className);
      if (classBytes == null) {
        throw new ClassNotFoundException(className);
      }

      if (HasAnnotation.hasAnnotation(classBytes, GwtScriptOnly.class)) {
        scriptOnlyClasses.add(className);
        maybeInitializeScriptOnlyClassLoader();

        /*
         * Release the lock before side-loading from scriptOnlyClassLoader. This prevents deadlock
         * conditions when a class from scriptOnlyClassLoader ends up trying to call back into this
         * classloader from another thread.
         */
        loadLock.unlock();

        // Also don't run the static initializer to lower the risk of deadlock.
        return Class.forName(className, false, scriptOnlyClassLoader);
      }

     /*
      * Prevent reentrant problems where classes that need to be injected have
      * circular dependencies on one another via JSNI and inheritance. This check
      * ensures that a class's supertype can refer to the subtype (static
      * members, etc) via JSNI references by ensuring that the Class for the
      * subtype will have been defined before injecting the JSNI for the
      * supertype.
      */
      boolean localInjection;
      if (!isInjectingClass) {
        localInjection = isInjectingClass = true;
      } else {
        localInjection = false;
      }

      Class<?> newClass = defineClass(className, classBytes, 0, classBytes.length);
      if (className.equals(JavaScriptHost.class.getName())) {
        javaScriptHostClass = newClass;
        updateJavaScriptHost();
      }

      /*
      * We have to inject the JSNI code after defining the class, since dispId
      * assignment is based around reflection on Class objects. Don't inject JSNI
      * when loading a JSO interface class; just wait until the implementation
      * class is loaded.
      */
      if (!classRewriter.isJsoIntf(className)) {
        CompilationUnit unit = getUnitForClassName(canonicalizeClassName(className));
        if (unit != null) {
          toInject.push(unit);
        }
      }

      if (localInjection) {
        try {
          /*
          * Can't use an iterator here because calling injectJsniFor may cause
          * additional entries to be added.
          */
          while (toInject.size() > 0) {
            CompilationUnit unit = toInject.remove(0);
            if (!alreadyInjected.contains(unit)) {
              injectJsniMethods(unit);
              alreadyInjected.add(unit);
            }
          }
        } finally {
          isInjectingClass = false;
        }
      }

      if (className.equals("com.google.gwt.core.client.GWT")) {
        gwtClass = newClass;
        setGwtBridge(makeGwtBridge());
      }

      return newClass;
    } finally {
      if (loadLock.isLocked()) {
        loadLock.unlock();
      }
    }
  }

  /**
   * Remove some of the excess locking that we'd normally inherit from loadClass.
   */
  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    // at design time we want to provide parent ClassLoader, so keep default implementation
    if (Beans.isDesignTime()) {
      return super.loadClass(name, resolve);
    }

    Class c = findLoadedClass(name);
    if (c != null) {
      if (resolve) {
        resolveClass(c);
      }
      return c;
    }

    assert getParent() == null;

    try {
      c = bootstrapClassLoader.loadClass(name);
    } catch (ClassNotFoundException e) {
      c = findClass(name);
    }

    if (resolve) {
      resolveClass(c);
    }

    return c;
  }

  void clear() {
    // Release our references to the shell.
    shellJavaScriptHost = null;
    scriptOnlyClasses.clear();
    scriptOnlyClassLoader = null;
    updateJavaScriptHost();
    weakJsoCache.clear();
    weakJavaWrapperCache.clear();
    dispClassInfoOracle.clear();
    setGwtBridge(null);
  }

  /**
   * Convert a binary class name into a resource-like name.
   */
  private String canonicalizeClassName(String className) {
    String lookupClassName = className.replace('.', '/');
    // A JSO impl class ends with $, strip it
    if (classRewriter != null && classRewriter.isJsoImpl(className)) {
      lookupClassName = lookupClassName.substring(0,
          lookupClassName.length() - 1);
    }
    return lookupClassName;
  }

  @SuppressWarnings("deprecation")
  private byte[] findClassBytes(String className) {
    if (JavaScriptHost.class.getName().equals(className)) {
      // No need to rewrite.
      return javaScriptHostBytes;
    }


    // A JSO impl class needs the class bytes for the original class.
    String lookupClassName = canonicalizeClassName(className);

    CompiledClass compiledClass = compilationState.getClassFileMap().get(
        lookupClassName);

    if (classRewriter != null && classRewriter.isJsoIntf(className)) {
      // Generate a synthetic JSO interface class.
      byte[] newBytes = classRewriter.writeJsoIntf(className, compiledClass != null ?
        compiledClass.getBytes() : null);
      if (CLASS_DUMP) {
        classDump(className, newBytes);
      }
      return newBytes;
    }

    CompilationUnit unit = (compiledClass == null)
        ? getUnitForClassName(lookupClassName) : compiledClass.getUnit();
    if (emmaAvailable) {
      /*
       * build the map for anonymous classes. Do so only if unit has anonymous
       * classes, jsni methods, is not super-source and the map has not been
       * built before.
       */
      List<JsniMethod> jsniMethods = (unit == null) ? null
          : unit.getJsniMethods();
      if (unit != null && !unit.isSuperSource() && !unit.isGenerated()
          && unit.hasAnonymousClasses() && jsniMethods != null
          && jsniMethods.size() > 0 && !unit.createdClassMapping()) {
        if (!unit.constructAnonymousClassMappings(logger)) {
          logger.log(TreeLogger.ERROR,
              "Our heuristic for mapping anonymous classes between compilers "
                  + "failed. Unsafe to continue because the wrong jsni code "
                  + "could end up running. className = " + className);
          return null;
        }
      }
    }

    byte classBytes[] = null;
    if (compiledClass != null) {
      classBytes = compiledClass.getBytes();
      if (!compiledClass.getUnit().isSuperSource()) {
        classBytes = emmaStrategy.getEmmaClassBytes(classBytes,
            lookupClassName, compiledClass.getUnit().getLastModified());
      } else {
        if (logger.isLoggable(TreeLogger.SPAM)) {
          logger.log(TreeLogger.SPAM, "no emma instrumentation for "
              + lookupClassName + " because it is from super-source");
        }
      }
    } else if (emmaAvailable) {
      /*
       * TypeOracle does not know about this class. Most probably, this class
       * was referenced in one of the classes loaded from disk. Check if we can
       * find it on disk. Typically this is a synthetic class added by the
       * compiler.
       */
      if (typeHasCompilationUnit(lookupClassName)
          && CompilationUnit.isClassnameGenerated(className)) {
        /*
         * modification time = 0 ensures that whatever is on the disk is always
         * loaded.
         */
        if (logger.isLoggable(TreeLogger.DEBUG)) {
          logger.log(TreeLogger.DEBUG, "EmmaStrategy: loading " + lookupClassName
              + " from disk even though TypeOracle does not know about it");
        }
        classBytes = emmaStrategy.getEmmaClassBytes(null, lookupClassName, 0);
      }
    }
    if (classBytes != null && classRewriter != null) {
      Map<String, String> anonymousClassMap = Collections.emptyMap();
      if (unit != null) {
        anonymousClassMap = unit.getAnonymousClassMap();
      }
      byte[] newBytes = classRewriter.rewrite(typeOracle, className,
          classBytes, anonymousClassMap);
      if (CLASS_DUMP) {
        if (!Arrays.equals(classBytes, newBytes)) {
          classDump(className, newBytes);
        }
      }
      classBytes = newBytes;
    }

    if (unit != null && unit.isError()) {
      // Compile worked, but the unit had some kind of error (JSNI?)
      CompilationProblemReporter.reportErrors(logger, unit, false);
    }

    return classBytes;
  }

  private String getBinaryName(JClassType type) {
    String name = type.getPackage().getName() + '.';
    name += type.getName().replace('.', '$');
    return name;
  }

  private String getBinaryOrPrimitiveName(JType type) {
    JArrayType asArray = type.isArray();
    JClassType asClass = type.isClassOrInterface();
    JPrimitiveType asPrimitive = type.isPrimitive();
    if (asClass != null) {
      return getBinaryName(asClass);
    } else if (asPrimitive != null) {
      return asPrimitive.getQualifiedSourceName();
    } else if (asArray != null) {
      JType componentType = asArray.getComponentType();
      return getBinaryOrPrimitiveName(componentType) + "[]";
    } else {
      throw new InternalCompilerException("Cannot create binary name for "
          + type.getQualifiedSourceName());
    }
  }

  /**
   * Returns the compilationUnit corresponding to the className. For nested
   * classes, the unit corresponding to the top level type is returned.
   *
   * Since a file might have several top-level types, search using classFileMap.
   */
  private CompilationUnit getUnitForClassName(String className) {
    String mainTypeName = className;
    int index = mainTypeName.length();
    CompiledClass cc = null;
    while (cc == null && index != -1) {
      mainTypeName = mainTypeName.substring(0, index);
      cc = compilationState.getClassFileMap().get(mainTypeName);
      index = mainTypeName.lastIndexOf('$');
    }
    return cc == null ? null : cc.getUnit();
  }

  private void injectJsniMethods(CompilationUnit unit) {
    if (unit == null || unit.getJsniMethods() == null) {
      return;
    }
    Event event = SpeedTracerLogger.start(DevModeEventType.LOAD_JSNI, "unit",
        unit.getTypeName());
    try {
      shellJavaScriptHost.createNativeMethods(logger, unit.getJsniMethods(),
          this);
    } finally {
      event.end();
    }
  }

  private void maybeInitializeScriptOnlyClassLoader() {
    if (scriptOnlyClassLoader == null) {
      scriptOnlyClassLoader = new MultiParentClassLoader(this,
          Thread.currentThread().getContextClassLoader());
    }
  }

  private boolean typeHasCompilationUnit(String className) {
    return getUnitForClassName(className) != null;
  }

  /**
   * Calls setBridge method on the GWT class inside this classloader, if possible.
   */
  private void setGwtBridge(GWTBridgeImpl bridge) {
    if (gwtClass == null) {
      return;
    }
    Throwable caught;
    try {
      final Class<?>[] paramTypes = new Class[]{GWTBridge.class};
      Method setBridgeMethod = gwtClass.getDeclaredMethod("setBridge",
          paramTypes);
      setBridgeMethod.setAccessible(true);
      setBridgeMethod.invoke(gwtClass, new Object[]{bridge});
      return;
    } catch (SecurityException e) {
      caught = e;
    } catch (NoSuchMethodException e) {
      caught = e;
    } catch (IllegalArgumentException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    } catch (InvocationTargetException e) {
      caught = e.getTargetException();
    }
    throw new RuntimeException("Error initializing GWT bridge", caught);
  }

  /**
   * Returns a new bridge or null.
   */
  private GWTBridgeImpl makeGwtBridge() {
    if (shellJavaScriptHost == null) {
      return null;
    }
    return new GWTBridgeImpl(shellJavaScriptHost);
  }

  /**
   * Tricky one, this. Reaches over into this modules's JavaScriptHost class and
   * sets its static 'host' field to our module space.
   *
   * @see JavaScriptHost
   */
  private void updateJavaScriptHost() {
    if (javaScriptHostClass == null) {
      return;
    }
    Throwable caught;
    try {
      final Class<?>[] paramTypes = new Class[]{ShellJavaScriptHost.class};
      Method setHostMethod = javaScriptHostClass.getMethod("setHost",
          paramTypes);
      setHostMethod.invoke(javaScriptHostClass,
          new Object[]{shellJavaScriptHost});
      return;
    } catch (SecurityException e) {
      caught = e;
    } catch (NoSuchMethodException e) {
      caught = e;
    } catch (IllegalArgumentException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    } catch (InvocationTargetException e) {
      caught = e.getTargetException();
    }
    throw new RuntimeException("Error initializing JavaScriptHost", caught);
  }
}
