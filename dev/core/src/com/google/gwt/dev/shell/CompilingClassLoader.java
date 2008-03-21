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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jdt.ByteCodeCompiler;
import com.google.gwt.dev.jdt.CacheManager;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.InstanceMethodMapper;
import com.google.gwt.util.tools.Utility;

import org.apache.commons.collections.map.ReferenceMap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * An isolated {@link ClassLoader} for running all user code. All user files are
 * compiled from source code byte a {@link ByteCodeCompiler}. After
 * compilation, some byte code rewriting is performed to support
 * <code>JavaScriptObject</code> and its subtypes.
 * 
 * TODO: we should refactor this class to move the getClassInfoByDispId,
 * getDispId, getMethodDispatch and putMethodDispatch into a separate entity
 * since they really do not interact with the CompilingClassLoader
 * functionality.
 */
public final class CompilingClassLoader extends ClassLoader {

  /**
   * Oracle that can answer questions about
   * {@link DispatchClassInfo DispatchClassInfos}.
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

      // References are of the form "@class::field" or
      // "@class::method(typesigs)".
      int endClassName = jsniMemberRef.indexOf("::");
      if (endClassName == -1 || jsniMemberRef.length() < 1
          || jsniMemberRef.charAt(0) != '@') {
        logger.log(TreeLogger.WARN, "Malformed JSNI reference '"
            + jsniMemberRef + "'; expect subsequent failures",
            new NoSuchFieldError(jsniMemberRef));
        return -1;
      }

      String className = jsniMemberRef.substring(1, endClassName);

      // Do the lookup by class name.
      DispatchClassInfo dispClassInfo = getClassInfoFromClassName(className);
      if (dispClassInfo != null) {
        String memberName = jsniMemberRef.substring(endClassName + 2);
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
     * name.
     * 
     * @param binaryClassName the binary name of a class
     * @return {@link java.lang.Class} instance or null if the given binary
     *         class name could not be found
     */
    private Class<?> getClassFromBinaryName(String binaryClassName) {
      try {
        return Class.forName(binaryClassName, true, CompilingClassLoader.this);
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

  private class MyMethodDeclarationMapper implements InstanceMethodMapper {

    private final Map<String, Set<JClassType>> signatureToDeclaringClasses = new HashMap<String, Set<JClassType>>();

    public MyMethodDeclarationMapper(Set<JClassType> jsoTypes,
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

    public String findDeclaringClass(String desc, String signature) {
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

  private final HostedModeClassRewriter classRewriter;

  private final ByteCodeCompiler compiler;

  private final DispatchClassInfoOracle dispClassInfoOracle = new DispatchClassInfoOracle();

  private final TreeLogger logger;

  private final Map<MethodAdaptor, Object> methodToDispatch = new HashMap<MethodAdaptor, Object>();

  private final TypeOracle typeOracle;

  @SuppressWarnings("unchecked")
  private final Map<Integer, Object> weakJsoCache = new ReferenceMap(
      ReferenceMap.HARD, ReferenceMap.WEAK);

  public CompilingClassLoader(TreeLogger logger, ByteCodeCompiler compiler,
      TypeOracle typeOracle) throws UnableToCompleteException {
    super(null);
    this.logger = logger;
    this.compiler = compiler;
    this.typeOracle = typeOracle;

    // Assertions are always on in hosted mode.
    setDefaultAssertionStatus(true);

    // SPECIAL MAGIC: Prevents the compile process from ever trying to compile
    // these guys from source, which is what we want, since they are special and
    // neither of them would compile correctly from source.
    // 
    // JavaScriptHost is special because its type cannot be known to the user.
    // It is referenced only from generated code and GWT.create.
    //
    for (int i = 0; i < CacheManager.BOOTSTRAP_CLASSES.length; i++) {
      Class<?> clazz = CacheManager.BOOTSTRAP_CLASSES[i];
      String className = clazz.getName();
      try {
        String path = clazz.getName().replace('.', '/').concat(".class");
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL url = cl.getResource(path);
        if (url != null) {
          byte classBytes[] = getClassBytesFromStream(url.openStream());
          String loc = url.toExternalForm();
          compiler.putClassBytes(logger, className, classBytes, loc);
        } else {
          logger.log(TreeLogger.ERROR,
              "Could not find required bootstrap class '" + className
                  + "' in the classpath", null);
          throw new UnableToCompleteException();
        }
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Error reading class bytes for "
            + className, e);
        throw new UnableToCompleteException();
      }
    }
    compiler.removeStaleByteCode(logger);

    // Create a class rewriter based on all the subtypes of the JSO class.
    JClassType jsoType = typeOracle.findType(JsValueGlue.JSO_CLASS);
    if (jsoType != null) {

      // Create a set of binary names.
      Set<JClassType> jsoTypes = new HashSet<JClassType>();
      JClassType[] jsoSubtypes = jsoType.getSubtypes();
      Collections.addAll(jsoTypes, jsoSubtypes);
      jsoTypes.add(jsoType);

      Set<String> jsoTypeNames = new HashSet<String>();
      for (JClassType type : jsoTypes) {
        jsoTypeNames.add(getBinaryName(type));
      }

      MyMethodDeclarationMapper mapper = new MyMethodDeclarationMapper(
          jsoTypes, typeOracle.getJavaLangObject());
      classRewriter = new HostedModeClassRewriter(jsoTypeNames, mapper);
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

  public Object getMethodDispatch(MethodAdaptor method) {
    synchronized (methodToDispatch) {
      return methodToDispatch.get(method);
    }
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

  public void putMethodDispatch(MethodAdaptor method, Object methodDispatch) {
    synchronized (methodToDispatch) {
      methodToDispatch.put(method, methodDispatch);
    }
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

    // MAGIC: this allows JavaScriptHost (in user space) to bridge to the real
    // class in host space.
    //
    if (className.equals(ShellJavaScriptHost.class.getName())) {
      return ShellJavaScriptHost.class;
    }

    // Get the bytes, compiling if necessary.
    byte[] classBytes;
    try {
      // A JSO impl class needs the class bytes for the original class.
      String lookupClassName = className;
      if (classRewriter != null && classRewriter.isJsoImpl(className)) {
        lookupClassName = className.substring(0, className.length() - 1);
      }
      classBytes = compiler.getClassBytes(logger, lookupClassName);
      if (classRewriter != null) {
        classBytes = classRewriter.rewrite(className, classBytes);
      }
      return defineClass(className, classBytes, 0, classBytes.length);
    } catch (UnableToCompleteException e) {
      throw new ClassNotFoundException(className);
    }
  }

  void clear() {
    weakJsoCache.clear();
    dispClassInfoOracle.clear();

    synchronized (methodToDispatch) {
      methodToDispatch.clear();
    }
  }

  private String getBinaryName(JClassType type) {
    String name = type.getPackage().getName() + '.';
    name += type.getName().replace('.', '$');
    return name;
  }

  private byte[] getClassBytesFromStream(InputStream is) throws IOException {
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

  private boolean isInStandardJavaPackage(String className) {
    if (className.startsWith("java.")) {
      return true;
    }

    if (className.startsWith("javax.")) {
      return true;
    }

    return false;
  }
}
