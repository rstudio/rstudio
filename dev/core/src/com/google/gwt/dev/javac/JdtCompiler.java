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

import com.google.gwt.dev.javac.impl.Shared;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.util.tools.Utility;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
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
   * Adapts a {@link CompilationUnit} for a JDT compile.
   */
  public static class CompilationUnitAdapter implements ICompilationUnit {

    private final CompilationUnit unit;

    public CompilationUnitAdapter(CompilationUnit unit) {
      this.unit = unit;
    }

    public char[] getContents() {
      return unit.getSource().toCharArray();
    }

    public char[] getFileName() {
      return unit.getDisplayLocation().toCharArray();
    }

    public char[] getMainTypeName() {
      return Shared.getShortName(unit.getTypeName()).toCharArray();
    }

    public char[][] getPackageName() {
      String packageName = Shared.getPackageName(unit.getTypeName());
      return CharOperation.splitOn('.', packageName.toCharArray());
    }

    public CompilationUnit getUnit() {
      return unit;
    }

    @Override
    public String toString() {
      return unit.toString();
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
      ICompilationUnit icu = cud.compilationResult().compilationUnit;
      CompilationUnitAdapter adapter = (CompilationUnitAdapter) icu;
      CompilationUnit unit = adapter.getUnit();
      unit.setCompiled(cud, binaryTypesRefs);
      if (!cud.compilationResult().hasErrors()) {
        recordBinaryTypes(unit.getCompiledClasses());
      }
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
  public static boolean compile(Collection<CompilationUnit> units) {
    return new JdtCompiler().doCompile(units);
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

  /**
   * Maps dotted binary names to compiled classes.
   */
  private final Map<String, CompiledClass> binaryTypes = new HashMap<String, CompiledClass>();

  /**
   * Maps dotted binary names the containing unit location.
   */
  private final Map<String, String> binaryTypesRefs = new HashMap<String, String>();

  private final Set<String> notPackages = new HashSet<String>();

  private final Set<String> packages = new HashSet<String>();

  /**
   * Not externally instantiable.
   */
  public JdtCompiler() {
  }

  public boolean doCompile(Collection<CompilationUnit> units) {
    List<ICompilationUnit> icus = new ArrayList<ICompilationUnit>();
    for (CompilationUnit unit : units) {
      String packageName = Shared.getPackageName(unit.getTypeName()).replace(
          '.', '/');
      addPackages(packageName);
      Set<CompiledClass> compiledClasses = unit.getCompiledClasses();
      if (compiledClasses == null) {
        icus.add(new CompilationUnitAdapter(unit));
      } else {
        recordBinaryTypes(compiledClasses);
      }
    }
    if (icus.isEmpty()) {
      return false;
    }

    PerfLogger.start("JdtCompiler.compile");
    new CompilerImpl().compile(icus.toArray(new ICompilationUnit[icus.size()]));
    PerfLogger.end();
    return true;
  }

  public Set<String> getBinaryTypeNames() {
    return binaryTypes.keySet();
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

  private void recordBinaryTypes(Set<CompiledClass> compiledClasses) {
    for (CompiledClass compiledClass : compiledClasses) {
      binaryTypes.put(compiledClass.getInternalName(), compiledClass);
      binaryTypesRefs.put(compiledClass.getInternalName(),
          compiledClass.getUnit().getDisplayLocation());
    }
  }

}
