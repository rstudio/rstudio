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

import com.google.gwt.dev.jdt.TypeRefVisitor;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.util.Name.BinaryName;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.util.tools.Utility;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.Clinit;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.NestedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.UnresolvedReferenceBinding;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Manages the process of compiling {@link CompilationUnit}s.
 */
public class JdtCompiler {
  /**
   * Provides hooks for changing the behavior of the JdtCompiler when unknown
   * types are encountered during compilation. Currently used for allowing
   * external tools to provide source lazily when undefined references appear.
   */
  public static interface AdditionalTypeProviderDelegate {
    /**
     * Checks for additional packages which may contain additional compilation
     * units.
     * 
     * @param slashedPackageName the '/' separated name of the package to find
     * @return <code>true</code> if such a package exists
     */
    boolean doFindAdditionalPackage(String slashedPackageName);

    /**
     * Finds a new compilation unit on-the-fly for the requested type, if there
     * is an alternate mechanism for doing so.
     * 
     * @param binaryName the binary name of the requested type
     * @return a unit answering the name, or <code>null</code> if no such unit
     *         can be created
     */
    GeneratedUnit doFindAdditionalType(String binaryName);
  }

  /**
   * A default processor that simply collects build units.
   */
  public static final class DefaultUnitProcessor implements UnitProcessor {
    private final List<CompilationUnit> results = new ArrayList<CompilationUnit>();

    public DefaultUnitProcessor() {
    }

    public List<CompilationUnit> getResults() {
      return Lists.normalizeUnmodifiable(results);
    }

    public void process(CompilationUnitBuilder builder, CompilationUnitDeclaration cud,
        List<CompiledClass> compiledClasses) {
      builder.setClasses(compiledClasses).setTypes(Collections.<JDeclaredType> emptyList())
          .setDependencies(new Dependencies()).setJsniMethods(Collections.<JsniMethod> emptyList())
          .setMethodArgs(new MethodArgNamesLookup())
          .setProblems(cud.compilationResult().getProblems());
      results.add(builder.build());
    }
  }
  /**
   * Static cache of all the JRE package names.
   */
  public static class JreIndex {
    private static Set<String> packages = readPackages();

    public static boolean contains(String name) {
      return packages.contains(name);
    }

    private static void addPackageRecursively(Set<String> packages, String pkg) {
      if (!packages.add(pkg)) {
        return;
      }

      int i = pkg.lastIndexOf('/');
      if (i != -1) {
        addPackageRecursively(packages, pkg.substring(0, i));
      }
    }

    private static Set<String> readPackages() {
      HashSet<String> pkgs = new HashSet<String>();
      String klass = "java/lang/Object.class";
      URL url = ClassLoader.getSystemClassLoader().getResource(klass);
      try {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        JarFile f = connection.getJarFile();
        Enumeration<JarEntry> entries = f.entries();
        while (entries.hasMoreElements()) {
          JarEntry e = entries.nextElement();
          String name = e.getName();
          if (name.endsWith(".class")) {
            String pkg = Shared.getSlashedPackageFrom(name);
            addPackageRecursively(pkgs, pkg);
          }
        }
        return pkgs;
      } catch (IOException e) {
        throw new InternalCompilerException("Unable to find JRE", e);
      }
    }
  }

  /**
   * Interface for processing units on the fly during compilation.
   */
  public interface UnitProcessor {
    void process(CompilationUnitBuilder builder, CompilationUnitDeclaration cud,
        List<CompiledClass> compiledClasses);
  }

  /**
   * Adapts a {@link CompilationUnit} for a JDT compile.
   */
  private static class Adapter implements ICompilationUnit {

    private final CompilationUnitBuilder builder;

    public Adapter(CompilationUnitBuilder builder) {
      this.builder = builder;
    }

    public CompilationUnitBuilder getBuilder() {
      return builder;
    }

    public char[] getContents() {
      return builder.getSource().toCharArray();
    }

    public char[] getFileName() {
      return builder.getLocation().toCharArray();
    }

    public char[] getMainTypeName() {
      return Shared.getShortName(builder.getTypeName()).toCharArray();
    }

    public char[][] getPackageName() {
      String packageName = Shared.getPackageName(builder.getTypeName());
      return CharOperation.splitOn('.', packageName.toCharArray());
    }

    @Override
    public String toString() {
      return builder.toString();
    }
  }

  private class CompilerImpl extends Compiler {

    public CompilerImpl() {
      super(new INameEnvironmentImpl(), DefaultErrorHandlingPolicies.proceedWithAllProblems(),
          getCompilerOptions(), new ICompilerRequestorImpl(), new DefaultProblemFactory(
              Locale.getDefault()));
    }

    @Override
    public void process(CompilationUnitDeclaration cud, int i) {
      super.process(cud, i);
      ClassFile[] classFiles = cud.compilationResult().getClassFiles();
      Map<ClassFile, CompiledClass> results = new LinkedHashMap<ClassFile, CompiledClass>();
      for (ClassFile classFile : classFiles) {
        createCompiledClass(classFile, results);
      }
      List<CompiledClass> compiledClasses = new ArrayList<CompiledClass>(results.values());
      addBinaryTypes(compiledClasses);

      ICompilationUnit icu = cud.compilationResult().compilationUnit;
      Adapter adapter = (Adapter) icu;
      CompilationUnitBuilder builder = adapter.getBuilder();
      processor.process(builder, cud, compiledClasses);
    }

    /**
     * Recursively creates enclosing types first.
     */
    private void createCompiledClass(ClassFile classFile, Map<ClassFile, CompiledClass> results) {
      if (results.containsKey(classFile)) {
        // Already created.
        return;
      }
      CompiledClass enclosingClass = null;
      if (classFile.enclosingClassFile != null) {
        ClassFile enclosingClassFile = classFile.enclosingClassFile;
        createCompiledClass(enclosingClassFile, results);
        enclosingClass = results.get(enclosingClassFile);
        assert enclosingClass != null;
      }
      String internalName = CharOperation.charToString(classFile.fileName());
      CompiledClass result =
          new CompiledClass(classFile.getBytes(), enclosingClass, isLocalType(classFile),
              internalName);
      results.put(classFile, result);
    }
  }

  /**
   * Hook point to accept results.
   */
  private static class ICompilerRequestorImpl implements ICompilerRequestor {
    public void acceptResult(CompilationResult result) {
    }
  }

  /**
   * How JDT receives files from the environment.
   */
  private class INameEnvironmentImpl implements INameEnvironment {
    public void cleanup() {
    }

    public NameEnvironmentAnswer findType(char[] type, char[][] pkg) {
      return findType(CharOperation.arrayConcat(pkg, type));
    }

    public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
      char[] binaryNameChars = CharOperation.concatWith(compoundTypeName, '/');
      String binaryName = String.valueOf(binaryNameChars);
      CompiledClass compiledClass = binaryTypes.get(binaryName);
      try {
        if (compiledClass != null) {
          return compiledClass.getNameEnvironmentAnswer();
        }
      } catch (ClassFormatException ex) {
        // fall back to binary class
      }
      if (isPackage(binaryName)) {
        return null;
      }
      if (additionalTypeProviderDelegate != null) {
        GeneratedUnit unit = additionalTypeProviderDelegate.doFindAdditionalType(binaryName);
        if (unit != null) {
          CompilationUnitBuilder b = CompilationUnitBuilder.create(unit);
          Adapter a = new Adapter(b);
          return new NameEnvironmentAnswer(a, null);
        }
      }
      try {
        URL resource = getClassLoader().getResource(binaryName + ".class");
        if (resource != null) {
          InputStream openStream = resource.openStream();
          try {
            ClassFileReader cfr = ClassFileReader.read(openStream, resource.toExternalForm(), true);
            return new NameEnvironmentAnswer(cfr, null);
          } finally {
            Utility.close(openStream);
          }
        }
      } catch (ClassFormatException e) {
      } catch (IOException e) {
      }
      return null;
    }

    public boolean isPackage(char[][] parentPkg, char[] pkg) {
      char[] pathChars = CharOperation.concatWith(parentPkg, pkg, '/');
      String packageName = String.valueOf(pathChars);
      return isPackage(packageName);
    }

    private ClassLoader getClassLoader() {
      return Thread.currentThread().getContextClassLoader();
    }

    private boolean isPackage(String slashedPackageName) {
      // Test the JRE explicitly, because the classloader trick doesn't work.
      if (JreIndex.contains(slashedPackageName)) {
        return true;
      }
      /*
       * TODO(zundel): When cached CompiledClass instances are used, 'packages'
       * does not contain all packages in the compile and this test fails the
       * test on some packages.
       * 
       * This is supposed to work via the call chain:
       * 
       * CSB.doBuildFrom -> CompileMoreLater.addValidUnit 
       *    -> JdtCompiler.addCompiledUnit
       *    -> addPackages()
       */
      if (packages.contains(slashedPackageName)) {
        return true;
      }
      if (notPackages.contains(slashedPackageName)) {
        return false;
      }
      String resourceName = slashedPackageName + '/';
      if ((additionalTypeProviderDelegate != null && additionalTypeProviderDelegate
          .doFindAdditionalPackage(slashedPackageName))) {
        addPackages(slashedPackageName);
        return true;
      }
      // Include class loader check for binary-only annotations.
      if (getClassLoader().getResource(resourceName) != null) {
        addPackages(slashedPackageName);
        return true;
      } else {
        notPackages.add(slashedPackageName);
        return false;
      }
    }
  }

  /**
   * Compiles the given set of units. The units will be internally modified to
   * reflect the results of compilation.
   */
  public static List<CompilationUnit> compile(Collection<CompilationUnitBuilder> builders) {
    Event jdtCompilerEvent = SpeedTracerLogger.start(CompilerEventType.JDT_COMPILER);

    try {
      DefaultUnitProcessor processor = new DefaultUnitProcessor();
      JdtCompiler compiler = new JdtCompiler(processor);
      compiler.doCompile(builders);
      return processor.getResults();
    } finally {
      jdtCompilerEvent.end();
    }
  }

  public static CompilerOptions getCompilerOptions() {
    CompilerOptions options = new CompilerOptions();
    options.complianceLevel = options.sourceLevel = options.targetJDK = ClassFileConstants.JDK1_6;

    // Generate debug info for debugging the output.
    options.produceDebugAttributes =
        ClassFileConstants.ATTR_VARS | ClassFileConstants.ATTR_LINES
            | ClassFileConstants.ATTR_SOURCE;
    // Tricks like "boolean stopHere = true;" depend on this setting.
    options.preserveAllLocalVariables = true;
    // Let the JDT collect compilation unit dependencies
    options.produceReferenceInfo = true;

    // Turn off all warnings, saves some memory / speed.
    options.reportUnusedDeclaredThrownExceptionIncludeDocCommentReference = false;
    options.reportUnusedDeclaredThrownExceptionExemptExceptionAndThrowable = false;
    options.warningThreshold = 0;
    options.inlineJsrBytecode = true;
    return options;
  }

  public static ReferenceBinding resolveType(LookupEnvironment lookupEnvironment, String typeName) {
    ReferenceBinding type = null;

    int p = typeName.indexOf('$');
    if (p > 0) {
      // resolve an outer type before trying to get the cached inner
      String cupName = typeName.substring(0, p);
      char[][] chars = CharOperation.splitOn('.', cupName.toCharArray());
      ReferenceBinding outerType = lookupEnvironment.getType(chars);
      if (outerType != null) {
        // outer class was found
        resolveRecursive(outerType);
        chars = CharOperation.splitOn('.', typeName.toCharArray());
        type = lookupEnvironment.getCachedType(chars);
        if (type == null) {
          // no inner type; this is a pure failure
          return null;
        }
      }
    } else {
      // just resolve the type straight out
      char[][] chars = CharOperation.splitOn('.', typeName.toCharArray());
      type = lookupEnvironment.getType(chars);
    }

    if (type != null) {
      if (type instanceof UnresolvedReferenceBinding) {
        /*
         * Since type is an instance of UnresolvedReferenceBinding, we know that
         * the return value BinaryTypeBinding.resolveType will be of type
         * ReferenceBinding
         */
        type = (ReferenceBinding) BinaryTypeBinding.resolveType(type, lookupEnvironment, true);
      }
      // found it
      return type;
    }

    // Assume that the last '.' should be '$' and try again.
    //
    p = typeName.lastIndexOf('.');
    if (p >= 0) {
      typeName = typeName.substring(0, p) + "$" + typeName.substring(p + 1);
      return resolveType(lookupEnvironment, typeName);
    }

    return null;
  }

  /**
   * Returns <code>true</code> if this is a local type, or if this type is
   * nested inside of any local type.
   */
  private static boolean isLocalType(ClassFile classFile) {
    SourceTypeBinding b = classFile.referenceBinding;
    while (!b.isStatic()) {
      if (b instanceof LocalTypeBinding) {
        return true;
      }
      b = ((NestedTypeBinding) b).enclosingType;
    }
    return false;
  }

  /**
   * Recursively invoking {@link ReferenceBinding#memberTypes()} causes JDT to
   * resolve and cache all nested types at arbitrary depth.
   */
  private static void resolveRecursive(ReferenceBinding outerType) {
    for (ReferenceBinding memberType : outerType.memberTypes()) {
      resolveRecursive(memberType);
    }
  }

  private AdditionalTypeProviderDelegate additionalTypeProviderDelegate;

  /**
   * Maps dotted binary names to compiled classes.
   */
  private final Map<String, CompiledClass> binaryTypes = new HashMap<String, CompiledClass>();

  /**
   * Only active during a compile.
   */
  private transient CompilerImpl compilerImpl;

  private final Set<String> notPackages = new HashSet<String>();

  private final Set<String> packages = new HashSet<String>();

  private final UnitProcessor processor;

  public JdtCompiler(UnitProcessor processor) {
    this.processor = processor;
  }

  public void addCompiledUnit(CompilationUnit unit) {
    addPackages(Shared.getPackageName(unit.getTypeName()).replace('.', '/'));
    addBinaryTypes(unit.getCompiledClasses());
  }

  public ArrayList<String> collectApiRefs(final CompilationUnitDeclaration cud) {
    final Set<String> apiRefs = new HashSet<String>();
    class DependencyVisitor extends TypeRefVisitor {
      public DependencyVisitor() {
        super(cud);
      }

      @Override
      public boolean visit(Argument arg, BlockScope scope) {
        // Adapted from {@link Argument#traverse}.
        // Don't visit annotations.
        if (arg.type != null) {
          arg.type.traverse(this, scope);
        }
        return false;
      }

      @Override
      public boolean visit(Argument arg, ClassScope scope) {
        // Adapted from {@link Argument#traverse}.
        // Don't visit annotations.
        if (arg.type != null) {
          arg.type.traverse(this, scope);
        }
        return false;
      }

      @Override
      public boolean visit(Block block, BlockScope scope) {
        assert false : "Error in DepedencyVisitor; should never visit a block";
        return false;
      }

      @Override
      public boolean visit(Clinit clinit, ClassScope scope) {
        return false;
      }

      @Override
      public boolean visit(ConstructorDeclaration ctor, ClassScope scope) {
        if (ctor.typeParameters != null) {
          int typeParametersLength = ctor.typeParameters.length;
          for (int i = 0; i < typeParametersLength; i++) {
            ctor.typeParameters[i].traverse(this, ctor.scope);
          }
        }
        traverse(ctor);
        return false;
      }

      @Override
      public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
        // Don't visit javadoc.
        // Don't visit annotations.
        if (fieldDeclaration.type != null) {
          fieldDeclaration.type.traverse(this, scope);
        }
        // Don't visit initialization.
        return false;
      }

      @Override
      public boolean visit(Initializer initializer, MethodScope scope) {
        return false;
      }

      @Override
      public boolean visit(MethodDeclaration meth, ClassScope scope) {
        if (meth.typeParameters != null) {
          int typeParametersLength = meth.typeParameters.length;
          for (int i = 0; i < typeParametersLength; i++) {
            meth.typeParameters[i].traverse(this, meth.scope);
          }
        }
        if (meth.returnType != null) {
          meth.returnType.traverse(this, meth.scope);
        }
        traverse(meth);
        return false;
      }

      @Override
      public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
        traverse(typeDeclaration);
        return false;
      }

      @Override
      public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
        traverse(typeDeclaration);
        return false;
      }

      @Override
      protected void onBinaryTypeRef(BinaryTypeBinding referencedType,
          CompilationUnitDeclaration unitOfReferrer, Expression expression) {
        if (!String.valueOf(referencedType.getFileName()).endsWith(".java")) {
          // ignore binary-only annotations
          return;
        }
        addReference(referencedType);
      }

      @Override
      protected void onTypeRef(SourceTypeBinding referencedType,
          CompilationUnitDeclaration unitOfReferrer) {
        addReference(referencedType);
      }

      private void addReference(ReferenceBinding referencedType) {
        String binaryName = CharOperation.toString(referencedType.compoundName);
        apiRefs.add(BinaryName.toSourceName(binaryName));
      }

      /**
       * Adapted from {@link MethodDeclaration#traverse}.
       */
      private void traverse(AbstractMethodDeclaration meth) {
        // Don't visit javadoc.
        // Don't visit annotations.
        if (meth.arguments != null) {
          int argumentLength = meth.arguments.length;
          for (int i = 0; i < argumentLength; i++) {
            meth.arguments[i].traverse(this, meth.scope);
          }
        }
        if (meth.thrownExceptions != null) {
          int thrownExceptionsLength = meth.thrownExceptions.length;
          for (int i = 0; i < thrownExceptionsLength; i++) {
            meth.thrownExceptions[i].traverse(this, meth.scope);
          }
        }
        // Don't visit method bodies.
      }

      /**
       * Adapted from {@link TypeDeclaration#traverse}.
       */
      private void traverse(TypeDeclaration type) {
        // Don't visit javadoc.
        // Don't visit annotations.
        if (type.superclass != null) {
          type.superclass.traverse(this, type.scope);
        }
        if (type.superInterfaces != null) {
          int length = type.superInterfaces.length;
          for (int i = 0; i < length; i++) {
            type.superInterfaces[i].traverse(this, type.scope);
          }
        }
        if (type.typeParameters != null) {
          int length = type.typeParameters.length;
          for (int i = 0; i < length; i++) {
            type.typeParameters[i].traverse(this, type.scope);
          }
        }
        if (type.memberTypes != null) {
          int length = type.memberTypes.length;
          for (int i = 0; i < length; i++) {
            type.memberTypes[i].traverse(this, type.scope);
          }
        }
        if (type.fields != null) {
          int length = type.fields.length;
          for (int i = 0; i < length; i++) {
            FieldDeclaration field;
            if ((field = type.fields[i]).isStatic()) {
              field.traverse(this, type.staticInitializerScope);
            } else {
              field.traverse(this, type.initializerScope);
            }
          }
        }
        if (type.methods != null) {
          int length = type.methods.length;
          for (int i = 0; i < length; i++) {
            type.methods[i].traverse(this, type.scope);
          }
        }
      }
    }
    DependencyVisitor visitor = new DependencyVisitor();
    cud.traverse(visitor, cud.scope);
    ArrayList<String> result = new ArrayList<String>(apiRefs);
    Collections.sort(result);
    return result;
  }

  public boolean doCompile(Collection<CompilationUnitBuilder> builders) {
    List<ICompilationUnit> icus = new ArrayList<ICompilationUnit>();
    for (CompilationUnitBuilder builder : builders) {
      addPackages(Shared.getPackageName(builder.getTypeName()).replace('.', '/'));
      icus.add(new Adapter(builder));
    }
    if (icus.isEmpty()) {
      return false;
    }

    compilerImpl = new CompilerImpl();
    compilerImpl.compile(icus.toArray(new ICompilationUnit[icus.size()]));
    compilerImpl = null;
    return true;
  }

  public ReferenceBinding resolveType(String typeName) {
    return resolveType(compilerImpl.lookupEnvironment, typeName);
  }

  public void setAdditionalTypeProviderDelegate(AdditionalTypeProviderDelegate newDelegate) {
    additionalTypeProviderDelegate = newDelegate;
  }

  private void addBinaryTypes(Collection<CompiledClass> compiledClasses) {
    for (CompiledClass cc : compiledClasses) {
      binaryTypes.put(cc.getInternalName(), cc);
    }
  }

  private void addPackages(String slashedPackageName) {
    while (packages.add(slashedPackageName)) {
      int pos = slashedPackageName.lastIndexOf('/');
      if (pos > 0) {
        slashedPackageName = slashedPackageName.substring(0, pos);
      } else {
        packages.add("");
        break;
      }
    }
  }

}
