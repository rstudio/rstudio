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
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompiledClass;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.InstanceMethodOracle;
import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.util.tools.Utility;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceIdentityMap;
import org.apache.commons.collections.map.ReferenceMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

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
        logger.log(TreeLogger.WARN, "Malformed JSNI reference '"
            + jsniMemberRef + "'; expect subsequent failures",
            new NoSuchFieldError(jsniMemberRef));
        return -1;
      }

      // Do the lookup by class name.
      String className = parsed.className();
      DispatchClassInfo dispClassInfo = getClassInfoFromClassName(className);
      if (dispClassInfo != null) {
        String memberName = parsed.memberSignature();
        int memberId = dispClassInfo.getMemberId(memberName);
        if (memberId < 0) {
          logger.log(TreeLogger.WARN, "Member '" + memberName
              + "' in JSNI reference '" + jsniMemberRef
              + "' could not be found; expect subsequent failures",
              new NoSuchFieldError(memberName));
        }

        return synthesizeDispId(dispClassInfo.getClassId(), memberId);
      }

      logger.log(TreeLogger.WARN, "Class '" + className
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
      try {
        return Class.forName(binaryClassName, false, CompilingClassLoader.this);
      } catch (ClassNotFoundException e) {
        return null;
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
      JClassType type = typeOracle.findType(className.replace('$', '.'));
      if (type != null) {
        // Use the type oracle to compute the exact binary name
        String jniSig = type.getJNISignature();
        jniSig = jniSig.substring(1, jniSig.length() - 1);
        className = jniSig.replace('/', '.');
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
   * Implements {@link InstanceMethodOracle} on behalf of the
   * {@link HostedModeClassRewriter}. Implemented using {@link TypeOracle}.
   */
  private class MyInstanceMethodOracle implements InstanceMethodOracle {

    private final Map<String, Set<JClassType>> signatureToDeclaringClasses = new HashMap<String, Set<JClassType>>();

    public MyInstanceMethodOracle(Set<JClassType> jsoTypes,
        JClassType javaLangObject) {
      // Populate the map.
      for (JClassType type : jsoTypes) {
        for (JMethod method : type.getMethods()) {
          if (!method.isStatic()) {
            String signature = createSignature(method);
            Set<JClassType> declaringClasses = signatureToDeclaringClasses.get(signature);
            if (declaringClasses == null) {
              declaringClasses = new HashSet<JClassType>();
              signatureToDeclaringClasses.put(signature, declaringClasses);
            }
            declaringClasses.add(type);
          }
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

    public String findOriginalDeclaringClass(String desc, String signature) {
      // Lookup the method.
      Set<JClassType> declaringClasses = signatureToDeclaringClasses.get(signature);
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
   * The names of the bridge classes.
   */
  private static final Map<String, Class<?>> BRIDGE_CLASS_NAMES = new HashMap<String, Class<?>>();

  /**
   * The set of classes exposed into user space that actually live in hosted
   * space (thus, they bridge across the spaces).
   */
  private static final Class<?>[] BRIDGE_CLASSES = new Class<?>[] {
      ShellJavaScriptHost.class, GWTBridge.class};

  private static final boolean CLASS_DUMP = false;

  private static final String CLASS_DUMP_PATH = "rewritten-classes";

  /**
   * Caches the byte code for {@link JavaScriptHost}.
   */
  private static byte[] javaScriptHostBytes;

  private static EmmaStrategy emmaStrategy;

  static {
    for (Class<?> c : BRIDGE_CLASSES) {
      BRIDGE_CLASS_NAMES.put(c.getName(), c);
    }
    /*
     * Specific support for bridging to Emma since the user classloader is
     * generally completely isolated.
     */
    boolean emmaIsAvailable = false;
    try {
      Class<?> emmaBridge = Class.forName(EmmaStrategy.EMMA_RT_CLASSNAME,
          false, Thread.currentThread().getContextClassLoader());
      BRIDGE_CLASS_NAMES.put(EmmaStrategy.EMMA_RT_CLASSNAME, emmaBridge);
      emmaIsAvailable = true;
    } catch (ClassNotFoundException ignored) {
    }
    emmaStrategy = EmmaStrategy.get(emmaIsAvailable);
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
      dir.mkdirs();
    }

    File file = new File(dir, className + ".class");
    try {
      FileOutputStream fileOutput = new FileOutputStream(file);
      fileOutput.write(bytes);
      fileOutput.close();
    } catch (IOException e) {
      e.printStackTrace();
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

  private final HostedModeClassRewriter classRewriter;

  private CompilationState compilationState;

  private final DispatchClassInfoOracle dispClassInfoOracle = new DispatchClassInfoOracle();

  private Class<?> gwtClass, javaScriptHostClass;

  private final TreeLogger logger;

  private ShellJavaScriptHost shellJavaScriptHost;

  private final TypeOracle typeOracle;

  @SuppressWarnings("unchecked")
  private final Map<Object, Object> weakJavaWrapperCache = new ReferenceIdentityMap(
      AbstractReferenceMap.WEAK, AbstractReferenceMap.WEAK);

  @SuppressWarnings("unchecked")
  private final Map<Integer, Object> weakJsoCache = new ReferenceMap(
      AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK);

  /**
   * Used by {@link #findClass(String)} to prevent reentrant JSNI injection.
   */
  private boolean isInjectingClass = false;

  /**
   * Used by {@link #findClass(String)} to prevent reentrant JSNI injection.
   */
  private Stack<CompiledClass> toInject = new Stack<CompiledClass>();

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
      Map<String, String> jsoSuperTypes = new HashMap<String, String>();
      for (JClassType type : jsoTypes) {
        String binaryName = getBinaryName(type);
        jsoTypeNames.add(binaryName);
        jsoSuperTypes.put(binaryName, getBinaryName(type.getSuperclass()));
      }

      MyInstanceMethodOracle mapper = new MyInstanceMethodOracle(jsoTypes,
          typeOracle.getJavaLangObject());
      classRewriter = new HostedModeClassRewriter(jsoTypeNames, jsoSuperTypes,
          mapper);
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
  public DispatchClassInfo getClassInfoByDispId(int dispId) {
    return dispClassInfoOracle.getClassInfoByDispId(dispId);
  }

  /**
   * Returns the dispatch id for a JSNI member reference.
   * 
   * @param jsniMemberRef a JSNI member reference
   * @return dispatch id or -1 if the JSNI member reference could not be found
   */
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
  protected synchronized Class<?> findClass(String className)
      throws ClassNotFoundException {
    if (className == null) {
      throw new ClassNotFoundException("null class name",
          new NullPointerException());
    }

    // Don't mess with anything in the standard Java packages.
    //
    if (isInStandardJavaPackage(className)) {
      // make my superclass load it
      throw new ClassNotFoundException(className);
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
      CompiledClass compiledClass = compilationState.getClassFileMap().get(
          canonicalizeClassName(className));
      if (compiledClass != null) {
        toInject.push(compiledClass);
      }
    }

    /*
     * Prevent reentrant problems where classes that need to be injected have
     * circular dependencies on one another.
     */
    if (!isInjectingClass) {
      try {
        isInjectingClass = true;
        /*
         * Can't use an iterator here because calling injectJsniFor may cause
         * additional entries to be added.
         */
        while (toInject.size() > 0) {
          injectJsniFor(toInject.remove(0));
        }
      } finally {
        isInjectingClass = false;
      }
    }

    if (className.equals("com.google.gwt.core.client.GWT")) {
      gwtClass = newClass;
      updateGwtClass();
    }

    return newClass;
  }

  void clear() {
    // Release our references to the shell.
    shellJavaScriptHost = null;
    updateJavaScriptHost();
    weakJsoCache.clear();
    weakJavaWrapperCache.clear();
    dispClassInfoOracle.clear();
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

  private byte[] findClassBytes(String className) {
    if (JavaScriptHost.class.getName().equals(className)) {
      // No need to rewrite.
      return javaScriptHostBytes;
    }

    if (classRewriter != null && classRewriter.isJsoIntf(className)) {
      // Generate a synthetic JSO interface class.
      return classRewriter.writeJsoIntf(className);
    }

    // A JSO impl class needs the class bytes for the original class.
    String lookupClassName = canonicalizeClassName(className);

    CompiledClass compiledClass = compilationState.getClassFileMap().get(
        lookupClassName);
    if (compiledClass != null) {
      byte[] classBytes = compiledClass.getBytes();
      if (!compiledClass.getUnit().isSuperSource()) {
        classBytes = emmaStrategy.getEmmaClassBytes(classBytes,
            lookupClassName, compiledClass.getUnit().getLastModified());
      } else {
        logger.log(TreeLogger.SPAM, "no emma instrumentation for "
            + lookupClassName + " because it is from super-source");
      }
      if (classRewriter != null) {
        byte[] newBytes = classRewriter.rewrite(className, classBytes);
        if (CLASS_DUMP) {
          if (!Arrays.equals(classBytes, newBytes)) {
            classDump(className, newBytes);
          }
        }
        classBytes = newBytes;
      }
      return classBytes;
    }
    return null;
  }

  private String getBinaryName(JClassType type) {
    String name = type.getPackage().getName() + '.';
    name += type.getName().replace('.', '$');
    return name;
  }

  private void injectJsniFor(CompiledClass compiledClass) {
    shellJavaScriptHost.createNativeMethods(logger, compiledClass,
        compiledClass.getJsniMethods(), this);
  }

  /**
   * NOTE: This logic is also used in SerializableTypeOracleBuilder; don't
   * change this one without changing the other.
   */
  private boolean isInStandardJavaPackage(String className) {
    if (className.startsWith("java.")) {
      return true;
    }

    if (className.startsWith("javax.")) {
      return true;
    }

    return false;
  }

  /**
   * Tricky one, this. Reaches over into this modules's JavaScriptHost class and
   * sets its static 'host' field to our module space.
   * 
   * @see JavaScriptHost
   */
  private void updateGwtClass() {
    if (gwtClass == null) {
      return;
    }
    Throwable caught;
    try {
      GWTBridgeImpl bridge;
      if (shellJavaScriptHost == null) {
        bridge = null;
      } else {
        bridge = new GWTBridgeImpl(shellJavaScriptHost);
      }
      final Class<?>[] paramTypes = new Class[] {GWTBridge.class};
      Method setBridgeMethod = gwtClass.getDeclaredMethod("setBridge",
          paramTypes);
      setBridgeMethod.setAccessible(true);
      setBridgeMethod.invoke(gwtClass, new Object[] {bridge});
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
      final Class<?>[] paramTypes = new Class[] {ShellJavaScriptHost.class};
      Method setHostMethod = javaScriptHostClass.getMethod("setHost",
          paramTypes);
      setHostMethod.invoke(javaScriptHostClass,
          new Object[] {shellJavaScriptHost});
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
