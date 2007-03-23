/*
 * Copyright 2006 Google Inc.
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
import com.google.gwt.dev.jdt.ByteCodeCompiler;
import com.google.gwt.dev.jdt.CacheManager;
import com.google.gwt.util.tools.Utility;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * TODO : we should refactor this class to move the getClassInfoByDispId,
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
    private final ArrayList classIdToClassInfo = new ArrayList();

    /**
     * Binary or source class name to DispatchClassInfo map.
     */
    private final Map classNameToClassInfo = new HashMap();

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

      return (DispatchClassInfo) classIdToClassInfo.get(classId);
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
       * MAGIC: toString is id 0
       * 
       * We need to check for toString early on to handle the JavaScript
       * toString method. We do a case insensitive check to make sure that
       * tostring in JavaScript would work.
       * 
       * TODO : is it true that tostring is valid in JavaScript? JavaScript is
       * case sensitive.
       */
      if (jsniMemberRef.equalsIgnoreCase("toString")) {
        return 0;
      }

      // References are of the form "@class::field" or
      // "@class::method(typesigs)".
      int endClassName = jsniMemberRef.indexOf("::");
      if (endClassName == -1 || jsniMemberRef.length() < 1
          || jsniMemberRef.charAt(0) != '@') {
        return -1;
      }

      String className = jsniMemberRef.substring(1, endClassName);

      // Do the lookup by class name.
      DispatchClassInfo dispClassInfo = getClassInfoFromClassName(className);
      if (dispClassInfo != null) {
        String memberName = jsniMemberRef.substring(endClassName + 2);
        int memberId = dispClassInfo.getMemberId(memberName);

        return synthesizeDispId(dispClassInfo.getClassId(), memberId);
      }

      // Mask the specific error and let the caller handle it.
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
    private Class getClassFromBinaryName(String binaryClassName) {
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
    private Class getClassFromBinaryOrSourceName(String className) {
      /*
       * Process the class name from right to left.
       */
      int fromIndex = className.length();
      while (fromIndex > 0) {
        String enclosingClassName = className.substring(0, fromIndex);

        Class cls = getClassFromBinaryName(enclosingClassName);
        if (cls != null) {
          if (fromIndex < className.length()) {
            String binaryClassName = enclosingClassName
                + className.substring(fromIndex).replace('.', '$');
            return getClassFromBinaryName(binaryClassName);
          } else {
            return cls;
          }
        } else {
          fromIndex = enclosingClassName.lastIndexOf('.', fromIndex);
        }
      }

      return null;
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

      DispatchClassInfo dispClassInfo = (DispatchClassInfo) classNameToClassInfo.get(className);
      if (dispClassInfo != null) {
        // return the cached value
        return dispClassInfo;
      }

      Class cls = getClassFromBinaryOrSourceName(className);
      if (cls == null) {
        /*
         * default to return null; mask the specific error and let the caller
         * handle it
         */
        return null;
      }

     if (dispClassInfo == null) {
        /*
         * we need to create a new DispatchClassInfo since we have never seen
         * this class before under any source or binary class name
         */
        int classId = classIdToClassInfo.size();

        dispClassInfo = new DispatchClassInfo(cls, classId);
        classIdToClassInfo.add(dispClassInfo);
      }

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

  private final ByteCodeCompiler compiler;

  private final DispatchClassInfoOracle dispClassInfoOracle = new DispatchClassInfoOracle();

  private final TreeLogger logger;

  private final Map methodToDispatch = new HashMap();

  public CompilingClassLoader(TreeLogger logger, ByteCodeCompiler compiler)
      throws UnableToCompleteException {
    super(null);
    this.logger = logger;
    this.compiler = compiler;

    // SPECIAL MAGIC: Prevents the compile process from ever trying to compile
    // these guys from source, which is what we want, since they are special and
    // neither of them would compile correctly from source.
    // 
    // JavaScriptHost is special because its type cannot be known to the user.
    // It is referenced only from generated code and GWT.create.
    //
    for (int i = 0; i < CacheManager.BOOTSTRAP_CLASSES.length; i++) {
      Class clazz = CacheManager.BOOTSTRAP_CLASSES[i];
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

  public Object getMethodDispatch(Method method) {
    synchronized (methodToDispatch) {
      return methodToDispatch.get(method);
    }
  }

  public void putMethodDispatch(Method method, Object methodDispatch) {
    synchronized (methodToDispatch) {
      methodToDispatch.put(method, methodDispatch);
    }
  }

  protected synchronized Class findClass(String className)
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
    // Define the class from bytes.
    //
    try {
      byte[] classBytes = compiler.getClassBytes(logger, className);
      return defineClass(className, classBytes, 0, classBytes.length);
    } catch (UnableToCompleteException e) {
      throw new ClassNotFoundException(className);
    }
  }

  void clear() {
    dispClassInfoOracle.clear();

    synchronized (methodToDispatch) {
      methodToDispatch.clear();
    }
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
