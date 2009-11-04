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
import com.google.gwt.dev.jdt.TypeRefVisitor;
import com.google.gwt.dev.util.DiskCache;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.dev.util.collect.IdentityHashMap;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.collect.Sets;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Encapsulates the state of a single active compilation unit in a particular
 * module. State is accumulated throughout the life cycle of the containing
 * module and may be invalidated at certain times and recomputed.
 */
public abstract class CompilationUnit {

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
      public void visitInnerClass(String name, String outerName,
          String innerName, int access) {
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
        if (isClassnameGenerated(lookupName)
            && !allGeneratedClasses.contains(lookupName)) {
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
      URL url = Thread.currentThread().getContextClassLoader().getResource(
          slashedName + ".class");
      if (url == null) {
        logger.log(TreeLogger.DEBUG, "Unable to find " + slashedName
            + " on the classPath");
        return null;
      }
      String urlStr = url.toExternalForm();
      if (slashedName.equals(mainClass)) {
        // initialize the mainUrlBase for later use.
        mainUrlBase = urlStr.substring(0, urlStr.lastIndexOf('/'));
      } else {
        assert mainUrlBase != null;
        if (!mainUrlBase.equals(urlStr.substring(0, urlStr.lastIndexOf('/')))) {
          logger.log(TreeLogger.DEBUG, "Found " + slashedName + " at " + urlStr
              + " The base location is different from  that of " + mainUrlBase
              + " Not loading");
          return null;
        }
      }

      // url != null, we found it on the class path.
      try {
        URLConnection conn = url.openConnection();
        return Util.readURLConnectionAsBytes(conn);
      } catch (IOException ignored) {
        logger.log(TreeLogger.DEBUG, "Unable to load " + urlStr
            + ", in trying to load " + slashedName);
        // Fall through.
      }
      return null;
    }
  }
  /**
   * Tracks the state of a compilation unit through the compile and recompile
   * process.
   */
  enum State {
    /**
     * All internal state is cleared; the unit's source has not yet been
     * compiled by JDT.
     */
    FRESH,
    /**
     * In this intermediate state, the unit's source has been compiled by JDT.
     * The unit will contain a set of CompiledClasses.
     */
    COMPILED,
    /**
     * In this final state, the unit was compiled, but contained one or more
     * errors. Those errors are cached inside the unit, but all other internal
     * state is cleared.
     */
    ERROR,
    /**
     * In this final state, the unit has been compiled and is error free.
     * Additionally, all other units this unit depends on (transitively) are
     * also error free. The unit contains a set of checked CompiledClasses. The
     * unit and each contained CompiledClass releases all references to the JDT
     * AST. Each class contains a reference to a valid JRealClassType, which has
     * been added to the module's TypeOracle, as well as byte code, JSNI
     * methods, and all other final state.
     */
    CHECKED,
    /**
     * A CHECKED generated unit enters this state at the start of a refresh. If
     * a generator generates the same unit with identical source, the unit is
     * immediately promoted to CHECKED, bypassing costly compilation,
     * validation, and TypeOracle building.
     */
    GRAVEYARD,
  }

  private class FindTypesInCud extends ASTVisitor {
    Map<SourceTypeBinding, CompiledClass> map = new IdentityHashMap<SourceTypeBinding, CompiledClass>();

    public Set<CompiledClass> getClasses() {
      return new HashSet<CompiledClass>(map.values());
    }

    @Override
    public boolean visit(TypeDeclaration typeDecl, BlockScope scope) {
      CompiledClass enclosingClass = map.get(typeDecl.binding.enclosingType());
      assert (enclosingClass != null);
      /*
       * Weird case: if JDT determines that this local class is totally
       * uninstantiable, it won't bother allocating a local name.
       */
      if (typeDecl.binding.constantPoolName() != null) {
        CompiledClass newClass = new CompiledClass(CompilationUnit.this,
            typeDecl, enclosingClass);
        map.put(typeDecl.binding, newClass);
      }
      return true;
    }

    @Override
    public boolean visit(TypeDeclaration typeDecl, ClassScope scope) {
      CompiledClass enclosingClass = map.get(typeDecl.binding.enclosingType());
      assert (enclosingClass != null);
      CompiledClass newClass = new CompiledClass(CompilationUnit.this,
          typeDecl, enclosingClass);
      map.put(typeDecl.binding, newClass);
      return true;
    }

    @Override
    public boolean visit(TypeDeclaration typeDecl, CompilationUnitScope scope) {
      assert (typeDecl.binding.enclosingType() == null);
      CompiledClass newClass = new CompiledClass(CompilationUnit.this,
          typeDecl, null);
      map.put(typeDecl.binding, newClass);
      return true;
    }
  }

  protected static final DiskCache diskCache = new DiskCache();

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
  public static boolean isClassnameGenerated(String className) {
    return GENERATED_CLASSNAME_PATTERN.matcher(className).matches();
  }

  private static Set<String> computeFileNameRefs(
      CompilationUnitDeclaration cud,
      final Map<String, String> binaryTypeToSourceFileMap) {
    final Set<String> result = new HashSet<String>();
    cud.traverse(new TypeRefVisitor(cud) {
      @Override
      protected void onBinaryTypeRef(BinaryTypeBinding referencedType,
          CompilationUnitDeclaration unitOfReferrer, Expression expression) {
        String fileName = binaryTypeToSourceFileMap.get(String.valueOf(referencedType.getFileName()));
        if (fileName != null) {
          result.add(fileName);
        }
      }

      @Override
      protected void onTypeRef(SourceTypeBinding referencedType,
          CompilationUnitDeclaration unitOfReferrer) {
        // Map the referenced type to the target compilation unit file.
        result.add(String.valueOf(referencedType.getFileName()));
      }
    }, cud.scope);
    return Sets.normalize(result);
  }

  /**
   * Map from the className in javac to the className in jdt. String represents
   * the part of className after the compilation unit name. Emma-specific.
   */
  private Map<String, String> anonymousClassMap = null;
  private CompilationUnitDeclaration cud;
  private CategorizedProblem[] errors;
  private Set<CompiledClass> exposedCompiledClasses;
  private Set<String> fileNameRefs;
  private List<JsniMethod> jsniMethods = null;
  private State state = State.FRESH;

  public boolean constructAnonymousClassMappings(TreeLogger logger) {
    /*
     * Check if the unit has one or more classes with generated names. 'javac'
     * below refers to the compiler that was used to compile the java files on
     * disk. Returns true if our heuristic for constructing the anonymous class
     * mappings worked.
     */
    anonymousClassMap = new HashMap<String, String>();
    for (String topLevelClass : getTopLevelClasses()) {
      // Generate a mapping for each top-level class separately
      List<String> javacClasses = new GeneratedClassnameFinder(logger,
          topLevelClass).getClassNames();
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

  public boolean createdClassMapping() {
    return anonymousClassMap != null;
  }

  /**
   * Overridden to finalize; always returns object identity.
   */
  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  public Map<String, String> getAnonymousClassMap() {
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
   * Returns the user-relevant location of the source file. No programmatic
   * assumptions should be made about the return value.
   */
  public abstract String getDisplayLocation();

  public boolean getJsniInjected() {
    return jsniMethods != null;
  }

  public List<JsniMethod> getJsniMethods() {
    return jsniMethods;
  }

  /**
   * Returns the last modified time of the compilation unit.
   */
  public abstract long getLastModified();

  /**
   * Returns the source code for this unit.
   */
  public abstract String getSource();

  /**
   * Returns the fully-qualified name of the top level public type.
   */
  public abstract String getTypeName();

  public boolean hasAnonymousClasses() {
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
   * Returns <code>true</code> if this unit is compiled and valid.
   */
  public boolean isCompiled() {
    return state == State.COMPILED || state == State.CHECKED
        || state == State.GRAVEYARD;
  }

  public boolean isError() {
    return state == State.ERROR;
  }

  /**
   * Returns <code>true</code> if this unit was generated by a
   * {@link com.google.gwt.core.ext.Generator}.
   */
  public abstract boolean isGenerated();

  /**
   * 
   * @return true if the Compilation Unit is from a super-source.
   */
  public abstract boolean isSuperSource();

  /**
   * Overridden to finalize; always returns {@link #getDisplayLocation()}.
   */
  @Override
  public final String toString() {
    return getDisplayLocation();
  }

  /*
   * A unit that will not be resurrected without being re-compiled.
   */
  public boolean willNotBeResurrected() {
    return state == State.FRESH || state == State.ERROR;
  }

  /**
   * If compiled, returns all contained classes; otherwise returns
   * <code>null</code>.
   */
  Set<CompiledClass> getCompiledClasses() {
    if (!isCompiled()) {
      return null;
    }
    if (exposedCompiledClasses == null) {
      FindTypesInCud typeFinder = new FindTypesInCud();
      cud.traverse(typeFinder, cud.scope);
      Set<CompiledClass> compiledClasses = typeFinder.getClasses();
      exposedCompiledClasses = Sets.normalizeUnmodifiable(compiledClasses);
    }
    return exposedCompiledClasses;
  }

  CategorizedProblem[] getErrors() {
    return errors;
  }

  Set<String> getFileNameRefs() {
    return fileNameRefs;
  }

  /**
   * If compiled, returns the JDT compilation unit declaration; otherwise
   * <code>null</code>.
   */
  CompilationUnitDeclaration getJdtCud() {
    return cud;
  }

  State getState() {
    return state;
  }

  void setChecked() {
    assert cud != null || state == State.GRAVEYARD;
    for (CompiledClass compiledClass : getCompiledClasses()) {
      compiledClass.checked();
    }
    cud = null;
    state = State.CHECKED;
  }

  /**
   * Sets the compiled JDT AST for this unit.
   */
  void setCompiled(CompilationUnitDeclaration cud,
      Map<String, String> binaryTypeToSourceFileMap) {
    assert (state == State.FRESH || state == State.ERROR);
    this.cud = cud;
    fileNameRefs = computeFileNameRefs(cud, binaryTypeToSourceFileMap);
    state = State.COMPILED;
  }

  void setError() {
    this.errors = cud.compilationResult().getErrors();
    invalidate();
    state = State.ERROR;
  }

  void setFresh() {
    this.errors = null;
    invalidate();
    state = State.FRESH;
  }

  void setGraveyard() {
    assert (state == State.CHECKED);
    if (exposedCompiledClasses != null) {
      for (CompiledClass compiledClass : exposedCompiledClasses) {
        compiledClass.graveyard();
      }
    }
    state = State.GRAVEYARD;
  }

  void setJsniMethods(List<JsniMethod> jsniMethods) {
    this.jsniMethods = Lists.normalizeUnmodifiable(jsniMethods);
  }

  private List<String> getJdtClassNames(String topLevelClass) {
    List<String> classNames = new ArrayList<String>();
    for (CompiledClass cc : getCompiledClasses()) {
      if (isAnonymousClass(cc)
          && cc.getInternalName().startsWith(topLevelClass + "$")) {
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
   * Removes all accumulated state associated with compilation.
   */
  private void invalidate() {
    cud = null;
    fileNameRefs = null;
    jsniMethods = null;
    if (exposedCompiledClasses != null) {
      for (CompiledClass compiledClass : exposedCompiledClasses) {
        compiledClass.invalidate();
      }
      exposedCompiledClasses = null;
    }
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
