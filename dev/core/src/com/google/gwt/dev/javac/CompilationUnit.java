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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.asm.ClassReader;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.commons.EmptyVisitor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.util.DiskCache;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.collect.HashMap;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Encapsulates the state of a single active compilation unit in a particular
 * module. State is accumulated throughout the life cycle of the containing
 * module and may be invalidated at certain times and recomputed.
 */
public abstract class CompilationUnit implements Serializable {

  /**
   * Encapsulates the functionality to find all nested classes of this class
   * that have compiler-generated names. All class bytes are loaded from the
   * disk and then analyzed using ASM.
   */
  static class GeneratedClassnameFinder {
    private static class AnonymousClassVisitor extends EmptyVisitor {
      /*
       * array of classNames of inner clases that aren't synthetic classes.
       */
      List<String> classNames = new ArrayList<String>();

      public List<String> getInnerClassNames() {
        return classNames;
      }

      @Override
      public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if ((access & Opcodes.ACC_SYNTHETIC) == 0) {
          classNames.add(name);
        }
      }
    }

    private final List<String> classesToScan;
    private final TreeLogger logger;
    private final String mainClass;
    private String mainUrlBase = null;

    GeneratedClassnameFinder(TreeLogger logger, String mainClass) {
      assert mainClass != null;
      this.mainClass = mainClass;
      classesToScan = new ArrayList<String>();
      classesToScan.add(mainClass);
      this.logger = logger;
    }

    List<String> getClassNames() {
      // using a list because presumably there will not be many generated
      // classes
      List<String> allGeneratedClasses = new ArrayList<String>();
      for (int i = 0; i < classesToScan.size(); i++) {
        String lookupName = classesToScan.get(i);
        byte classBytes[] = getClassBytes(lookupName);
        if (classBytes == null) {
          /*
           * Weird case: javac might generate a name and reference the class in
           * the bytecode but decide later that the class is unnecessary. In the
           * bytecode, a null is passed for the class.
           */
          continue;
        }

        /*
         * Add the class to the list only if it can be loaded to get around the
         * javac weirdness issue where javac refers a class but does not
         * generate it.
         */
        if (isClassnameGenerated(lookupName) && !allGeneratedClasses.contains(lookupName)) {
          allGeneratedClasses.add(lookupName);
        }
        AnonymousClassVisitor cv = new AnonymousClassVisitor();
        new ClassReader(classBytes).accept(cv, 0);
        List<String> innerClasses = cv.getInnerClassNames();
        for (String innerClass : innerClasses) {
          // The innerClass has to be an inner class of the lookupName
          if (!innerClass.startsWith(mainClass + "$")) {
            continue;
          }
          /*
           * TODO (amitmanjhi): consider making this a Set if necessary for
           * performance
           */
          // add the class to classes
          if (!classesToScan.contains(innerClass)) {
            classesToScan.add(innerClass);
          }
        }
      }
      Collections.sort(allGeneratedClasses, new GeneratedClassnameComparator());
      return allGeneratedClasses;
    }

    /*
     * Load classBytes from disk. Check if the classBytes are loaded from the
     * same location as the location of the mainClass.
     */
    private byte[] getClassBytes(String slashedName) {
      URL url = Thread.currentThread().getContextClassLoader().getResource(slashedName + ".class");
      if (url == null) {
        if (logger.isLoggable(TreeLogger.DEBUG)) {
          logger.log(TreeLogger.DEBUG, "Unable to find " + slashedName + " on the classPath");
        }
        return null;
      }
      String urlStr = url.toExternalForm();
      if (slashedName.equals(mainClass)) {
        // initialize the mainUrlBase for later use.
        mainUrlBase = urlStr.substring(0, urlStr.lastIndexOf('/'));
      } else {
        assert mainUrlBase != null;
        if (!mainUrlBase.equals(urlStr.substring(0, urlStr.lastIndexOf('/')))) {
          if (logger.isLoggable(TreeLogger.DEBUG)) {
            logger.log(TreeLogger.DEBUG, "Found " + slashedName + " at " + urlStr
                + " The base location is different from  that of " + mainUrlBase + " Not loading");
          }
          return null;
        }
      }

      // url != null, we found it on the class path.
      try {
        URLConnection conn = url.openConnection();
        return Util.readURLConnectionAsBytes(conn);
      } catch (IOException ignored) {
        if (logger.isLoggable(TreeLogger.DEBUG)) {
          logger.log(TreeLogger.DEBUG, "Unable to load " + urlStr + ", in trying to load "
              + slashedName);
        }
        // Fall through.
      }
      return null;
    }
  }

  protected static final DiskCache diskCache = DiskCache.INSTANCE;

  public static final Comparator<CompilationUnit> COMPARATOR = new Comparator<CompilationUnit>() {
    @Override
    public int compare(CompilationUnit o1, CompilationUnit o2) {
      return o1.getResourcePath().compareTo(o2.getResourcePath());
    }
  };

  private static final Pattern GENERATED_CLASSNAME_PATTERN = Pattern.compile(".+\\$\\d.*");

  /**
   * Checks if the class names is generated. Accepts any classes whose names
   * match .+$\d.* (handling named classes within anonymous classes and multiple
   * named classes of the same name in a class, but in different methods).
   * Checks if the class or any of its enclosing classes are anonymous or
   * synthetic.
   * <p>
   * If new compilers have different conventions for anonymous and synthetic
   * classes, this code needs to be updated.
   * </p>
   * 
   * @param className name of the class to be checked.
   * @return true iff class or any of its enclosing classes are anonymous or
   *         synthetic.
   */
  @Deprecated
  public static boolean isClassnameGenerated(String className) {
    return GENERATED_CLASSNAME_PATTERN.matcher(className).matches();
  }

  /**
   * Map from the className in javac to the className in jdt. String represents
   * the part of className after the compilation unit name. Emma-specific.
   */
  private transient Map<String, String> anonymousClassMap = null;

  /**
   * Returns the unit as an instance of {@link CachedCompilationUnit}, making a
   * copy if necessary.
   */
  public abstract CachedCompilationUnit asCachedCompilationUnit();

  @Deprecated
  public final boolean constructAnonymousClassMappings(TreeLogger logger) {
    /*
     * Check if the unit has one or more classes with generated names. 'javac'
     * below refers to the compiler that was used to compile the java files on
     * disk. Returns true if our heuristic for constructing the anonymous class
     * mappings worked.
     */
    anonymousClassMap = new HashMap<String, String>();
    for (String topLevelClass : getTopLevelClasses()) {
      // Generate a mapping for each top-level class separately
      List<String> javacClasses =
          new GeneratedClassnameFinder(logger, topLevelClass).getClassNames();
      List<String> jdtClasses = getJdtClassNames(topLevelClass);
      if (javacClasses.size() != jdtClasses.size()) {
        anonymousClassMap = Collections.emptyMap();
        return false;
      }
      int size = javacClasses.size();
      for (int i = 0; i < size; i++) {
        if (!javacClasses.get(i).equals(jdtClasses.get(i))) {
          anonymousClassMap.put(javacClasses.get(i), jdtClasses.get(i));
        }
      }
    }
    return true;
  }

  @Deprecated
  public final boolean createdClassMapping() {
    return anonymousClassMap != null;
  }

  /**
   * Overridden to finalize; always returns object identity.
   */
  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Deprecated
  public final Map<String, String> getAnonymousClassMap() {
    /*
     * Return an empty map so that class-rewriter does not need to check for
     * null. A null value indicates that anonymousClassMap was never created
     * which is the case for many units. An example is a class containing jsni
     * units but no inner classes.
     */
    if (anonymousClassMap == null) {
      return Collections.emptyMap();
    }
    return anonymousClassMap;
  }

  /**
   * Returns all contained classes.
   */
  public abstract Collection<CompiledClass> getCompiledClasses();

  public abstract List<JsniMethod> getJsniMethods();

  /**
   * Returns the last modified time of the compilation unit.
   */
  public abstract long getLastModified();

  /**
   * @return a way to lookup method argument names for this compilation unit.
   */
  public abstract MethodArgNamesLookup getMethodArgs();

  /**
   * This is the resource location from the classpath or some deterministic
   * virtual location (in the case of generators or mock data) where the source
   * for this unit originated. This should be unique for each unit compiled to
   * create a module.
   * 
   * @see com.google.gwt.dev.resource.Resource#getLocation()
   */
  public abstract String getResourceLocation();

  /**
   * Returns the full abstract path of the resource. If a resource has been
   * re-rooted, this path should include any path prefix that was stripped.
   * 
   * @see com.google.gwt.dev.resource.Resource#getPath() 
   * @see com.google.gwt.dev.resource.Resource#getPathPrefix()
   */
  public abstract String getResourcePath();

  /**
   * Returns the fully-qualified name of the top level public type.
   */
  public abstract String getTypeName();

  /**
   * Returns the GWT AST types in this unit.
   */
  public List<JDeclaredType> getTypes() {
    try {
      byte[] bytes = getTypesSerialized();
      ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
      return JProgram.deserializeTypes(ois);
    } catch (IOException e) {
      throw new RuntimeException("Unexpected IOException on in-memory stream", e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Unexpected error deserializing AST for '" + getTypeName() + "'",
          e);
    }
  }

  /**
   * Returns the GWT AST types in this unit in serialized form.
   */
  public abstract byte[] getTypesSerialized();

  @Deprecated
  public final boolean hasAnonymousClasses() {
    for (CompiledClass cc : getCompiledClasses()) {
      if (isAnonymousClass(cc)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Overridden to finalize; always returns identity hash code.
   */
  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  /**
   * Returns <code>true</code> if this unit had errors.
   */
  public abstract boolean isError();

  /**
   * Returns <code>true</code> if this unit was generated by a
   * {@link com.google.gwt.core.ext.Generator}.
   */
  @Deprecated
  public abstract boolean isGenerated();

  /**
   * 
   * @return true if the Compilation Unit is from a super-source.
   */
  @Deprecated
  public abstract boolean isSuperSource();

  /**
   * Overridden to finalize; always returns {@link #getResourceLocation()}.
   */
  @Override
  public final String toString() {
    return getResourceLocation();
  }

  /**
   * The canonical serialized form of a CompilatinUnit is
   * {@link CachedCompilationUnit}.
   */
  protected final Object writeReplace() {
    return asCachedCompilationUnit();
  }

  /**
   * Returns the content ID for the source with which this unit was compiled.
   */
  abstract ContentId getContentId();

  /**
   * The set of dependencies on other classes.
   */
  abstract Dependencies getDependencies();

  abstract CategorizedProblem[] getProblems();

  private List<String> getJdtClassNames(String topLevelClass) {
    List<String> classNames = new ArrayList<String>();
    for (CompiledClass cc : getCompiledClasses()) {
      if (isAnonymousClass(cc) && cc.getInternalName().startsWith(topLevelClass + "$")) {
        classNames.add(cc.getInternalName());
      }
    }
    Collections.sort(classNames, new GeneratedClassnameComparator());
    return classNames;
  }

  private List<String> getTopLevelClasses() {
    List<String> topLevelClasses = new ArrayList<String>();
    for (CompiledClass cc : getCompiledClasses()) {
      if (cc.getEnclosingClass() == null) {
        topLevelClasses.add(cc.getInternalName());
      }
    }
    return topLevelClasses;
  }

  /**
   * TODO(amitmanjhi): what is the difference between an anonymous and local
   * class for our purposes? All our unit tests pass whether or not we do the
   * additional {@link #isClassnameGenerated} check. We either need to find the
   * real difference and add a unit test, or else simply this.
   */
  private boolean isAnonymousClass(CompiledClass cc) {
    return cc.isLocal() && isClassnameGenerated(cc.getInternalName());
  }
}
