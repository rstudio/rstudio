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
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.collect.IdentityHashMap;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.collect.Sets;
import com.google.gwt.util.tools.Utility;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
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
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.UnresolvedReferenceBinding;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Manages the process of compiling {@link CompilationUnit}s.
 */
public class JdtCompiler {

  /**
   * A default processor that simply collects build units.
   */
  public static final class DefaultUnitProcessor implements UnitProcessor {
    private JdtCompiler compiler;
    private final List<CompilationUnit> results = new ArrayList<CompilationUnit>();

    public DefaultUnitProcessor() {
    }

    public List<CompilationUnit> getResults() {
      return Lists.normalizeUnmodifiable(results);
    }

    public void process(CompilationUnitBuilder builder,
        CompilationUnitDeclaration cud, List<CompiledClass> compiledClasses) {
      CompilationUnit unit = builder.build(compiledClasses,
          compiler.computeDependencies(cud),
          Collections.<JsniMethod> emptyList(),
          cud.compilationResult().getProblems());
      if (cud.compilationResult().hasErrors()) {
        unit = new ErrorCompilationUnit(unit);
      }
      results.add(unit);
    }

    public void setCompiler(JdtCompiler compiler) {
      this.compiler = compiler;
    }
  }
  /**
   * Interface for processing units on the fly during compilation.
   */
  public interface UnitProcessor {
    void process(CompilationUnitBuilder builder,
        CompilationUnitDeclaration cud, List<CompiledClass> compiledClasses);
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
      super(new INameEnvironmentImpl(),
          DefaultErrorHandlingPolicies.proceedWithAllProblems(),
          getCompilerOptions(), new ICompilerRequestorImpl(),
          new DefaultProblemFactory(Locale.getDefault()));
    }

    @Override
    public void process(CompilationUnitDeclaration cud, int i) {
      super.process(cud, i);
      FindTypesInCud typeFinder = new FindTypesInCud();
      cud.traverse(typeFinder, cud.scope);
      List<CompiledClass> compiledClasses = typeFinder.getClasses();
      addBinaryTypes(compiledClasses);

      ICompilationUnit icu = cud.compilationResult().compilationUnit;
      Adapter adapter = (Adapter) icu;
      CompilationUnitBuilder builder = adapter.getBuilder();
      contentIdMap.put(builder.getLocation(), builder.getContentId());
      processor.process(builder, cud, compiledClasses);
    }
  }

  private class FindTypesInCud extends ASTVisitor {
    Map<SourceTypeBinding, CompiledClass> map = new IdentityHashMap<SourceTypeBinding, CompiledClass>();

    public List<CompiledClass> getClasses() {
      return new ArrayList<CompiledClass>(map.values());
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
        CompiledClass newClass = new CompiledClass(typeDecl, enclosingClass);
        map.put(typeDecl.binding, newClass);
      }
      return true;
    }

    @Override
    public boolean visit(TypeDeclaration typeDecl, ClassScope scope) {
      CompiledClass enclosingClass = map.get(typeDecl.binding.enclosingType());
      assert (enclosingClass != null);
      CompiledClass newClass = new CompiledClass(typeDecl, enclosingClass);
      map.put(typeDecl.binding, newClass);
      return true;
    }

    @Override
    public boolean visit(TypeDeclaration typeDecl, CompilationUnitScope scope) {
      assert (typeDecl.binding.enclosingType() == null);
      CompiledClass newClass = new CompiledClass(typeDecl, null);
      map.put(typeDecl.binding, newClass);
      return true;
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
      if (compiledClass != null) {
        return compiledClass.getNameEnvironmentAnswer();
      }
      if (isPackage(binaryName)) {
        return null;
      }
      try {
        URL resource = getClassLoader().getResource(binaryName + ".class");
        if (resource != null) {
          InputStream openStream = resource.openStream();
          try {
            ClassFileReader cfr = ClassFileReader.read(openStream,
                resource.toExternalForm(), true);
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
      // Include class loader check for binary-only annotations.
      if (packages.contains(slashedPackageName)) {
        return true;
      }
      if (notPackages.contains(slashedPackageName)) {
        return false;
      }
      String resourceName = slashedPackageName + '/';
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
  public static List<CompilationUnit> compile(
      Collection<CompilationUnitBuilder> builders) {
    DefaultUnitProcessor processor = new DefaultUnitProcessor();
    JdtCompiler compiler = new JdtCompiler(processor);
    processor.setCompiler(compiler);
    compiler.doCompile(builders);
    return processor.getResults();
  }

  public static CompilerOptions getCompilerOptions() {
    CompilerOptions options = new CompilerOptions();
    options.complianceLevel = options.sourceLevel = options.targetJDK = ClassFileConstants.JDK1_6;

    // Generate debug info for debugging the output.
    options.produceDebugAttributes = ClassFileConstants.ATTR_VARS
        | ClassFileConstants.ATTR_LINES | ClassFileConstants.ATTR_SOURCE;
    // Tricks like "boolean stopHere = true;" depend on this setting.
    options.preserveAllLocalVariables = true;

    // Turn off all warnings, saves some memory / speed.
    options.reportUnusedDeclaredThrownExceptionIncludeDocCommentReference = false;
    options.reportUnusedDeclaredThrownExceptionExemptExceptionAndThrowable = false;
    options.warningThreshold = 0;
    options.inlineJsrBytecode = true;
    return options;
  }

  public static ReferenceBinding resolveType(
      LookupEnvironment lookupEnvironment, String typeName) {
    ReferenceBinding type = null;

    int p = typeName.indexOf('$');
    if (p > 0) {
      // resolve an outer type before trying to get the cached inner
      String cupName = typeName.substring(0, p);
      char[][] chars = CharOperation.splitOn('.', cupName.toCharArray());
      if (lookupEnvironment.getType(chars) != null) {
        // outer class was found
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
        type = BinaryTypeBinding.resolveType(type, lookupEnvironment, true);
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
   * Maps dotted binary names to compiled classes.
   */
  private final Map<String, CompiledClass> binaryTypes = new HashMap<String, CompiledClass>();

  /**
   * Only active during a compile.
   */
  private transient CompilerImpl compilerImpl;

  /**
   * Maps resource path names to contentId to resolve dependencies.
   */
  private final Map<String, ContentId> contentIdMap = new HashMap<String, ContentId>();

  /**
   * Builders don't compute their contentId until their source is read;
   * therefore we cannot eagerly lookup their contentId up front. Keep the set
   * of currently compiling units in a map and lazily fetch their id only when a
   * dependency is encountered.
   */
  private transient Map<String, CompilationUnitBuilder> lazyContentIdMap;

  private final Set<String> notPackages = new HashSet<String>();

  private final Set<String> packages = new HashSet<String>();

  private final UnitProcessor processor;

  public JdtCompiler(UnitProcessor processor) {
    this.processor = processor;
  }

  public void addCompiledUnit(CompilationUnit unit) {
    assert unit.isCompiled();
    addPackages(Shared.getPackageName(unit.getTypeName()).replace('.', '/'));
    addBinaryTypes(unit.getCompiledClasses());
    contentIdMap.put(unit.getDisplayLocation(), unit.getContentId());
  }

  public Set<ContentId> computeDependencies(CompilationUnitDeclaration cud) {
    return computeDependencies(cud, Collections.<String> emptySet());
  }

  public Set<ContentId> computeDependencies(
      final CompilationUnitDeclaration cud, Set<String> additionalDependencies) {
    final Set<ContentId> result = new HashSet<ContentId>();
    class DependencyVisitor extends TypeRefVisitor {
      public DependencyVisitor() {
        super(cud);
      }

      @Override
      protected void onBinaryTypeRef(BinaryTypeBinding referencedType,
          CompilationUnitDeclaration unitOfReferrer, Expression expression) {
        String fileName = String.valueOf(referencedType.getFileName());
        addFileReference(fileName);
      }

      @Override
      protected void onTypeRef(SourceTypeBinding referencedType,
          CompilationUnitDeclaration unitOfReferrer) {
        // Map the referenced type to the target compilation unit file.
        String fileName = String.valueOf(referencedType.getFileName());
        addFileReference(fileName);
      }

      private void addFileReference(String fileName) {
        if (!fileName.endsWith(".java")) {
          // Binary-only reference, cannot compute dependency.
          return;
        }
        ContentId contentId = contentIdMap.get(fileName);
        if (contentId == null) {
          // This may be a reference to a currently-compiling unit.
          CompilationUnitBuilder builder = lazyContentIdMap.get(fileName);
          assert builder != null : "Unexpected source reference ('" + fileName
              + "') could not find builder";
          contentId = builder.getContentId();
        }
        assert contentId != null;
        result.add(contentId);
      }
    }
    DependencyVisitor visitor = new DependencyVisitor();
    cud.traverse(visitor, cud.scope);

    for (String dependency : additionalDependencies) {
      visitor.addFileReference(dependency);
    }
    return Sets.normalize(result);
  }

  public boolean doCompile(Collection<CompilationUnitBuilder> builders) {
    lazyContentIdMap = new HashMap<String, CompilationUnitBuilder>();
    List<ICompilationUnit> icus = new ArrayList<ICompilationUnit>();
    for (CompilationUnitBuilder builder : builders) {
      addPackages(Shared.getPackageName(builder.getTypeName()).replace('.', '/'));
      icus.add(new Adapter(builder));
      lazyContentIdMap.put(builder.getLocation(), builder);
    }
    if (icus.isEmpty()) {
      return false;
    }

    PerfLogger.start("JdtCompiler.compile");
    compilerImpl = new CompilerImpl();
    compilerImpl.compile(icus.toArray(new ICompilationUnit[icus.size()]));
    compilerImpl = null;
    PerfLogger.end();
    lazyContentIdMap = null;
    return true;
  }

  public ReferenceBinding resolveType(String typeName) {
    return resolveType(compilerImpl.lookupEnvironment, typeName);
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
