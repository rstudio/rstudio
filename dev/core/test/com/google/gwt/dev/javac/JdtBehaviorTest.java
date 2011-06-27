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

import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockResource;

import junit.framework.TestCase;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validates certain JDT behaviors that the compilation process may depend on
 * for correctness. One useful aspect would be that if we upgrade JDT in the
 * future, this test could help validate our assumptions.
 */
public class JdtBehaviorTest extends TestCase {

  /**
   * Hook-point if we need to modify the compiler behavior.
   */
  private static class CompilerImpl extends Compiler {

    public CompilerImpl(INameEnvironment environment,
        ICompilerRequestor requestor) {
      super(environment, DefaultErrorHandlingPolicies.proceedWithAllProblems(),
          JdtCompiler.getCompilerOptions(), requestor,
          new DefaultProblemFactory(Locale.getDefault()));
    }
  }

  /**
   * Hook point to accept results.
   */
  private class ICompilerRequestorImpl implements ICompilerRequestor {
    public void acceptResult(CompilationResult result) {
      if (result.hasErrors()) {
        StringBuilder sb = new StringBuilder();
        for (CategorizedProblem problem : result.getErrors()) {
          sb.append(problem.toString());
          sb.append('\n');
        }
        fail(sb.toString());
      }
      for (ClassFile classFile : result.getClassFiles()) {
        char[][] classNameArray = classFile.getCompoundName();
        char[][] packageArray = CharOperation.subarray(classNameArray, 0,
            classNameArray.length - 1);
        char[] packageName = CharOperation.concatWith(packageArray, '.');
        char[] className = CharOperation.concatWith(classNameArray, '.');
        addPackages(String.valueOf(packageName));
        classFiles.put(String.valueOf(className), classFile);
      }
    }

    private void addPackages(String packageName) {
      while (true) {
        packages.add(String.valueOf(packageName));
        int pos = packageName.lastIndexOf('.');
        if (pos > 0) {
          packageName = packageName.substring(0, pos);
        } else {
          packages.add("");
          break;
        }
      }
    }
  }

  /**
   * How JDT receives files from the environment.
   */
  private class INameEnvironmentImpl implements INameEnvironment {
    public void cleanup() {
      // intentionally blank
    }

    public NameEnvironmentAnswer findType(char[] type, char[][] pkg) {
      return findType(CharOperation.arrayConcat(pkg, type));
    }

    public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
      final char[] typeChars = CharOperation.concatWith(compoundTypeName, '.');
      String typeName = String.valueOf(typeChars);
      // System.out.println("findType: " + typeName);
      ClassFile classFile = classFiles.get(typeName);
      if (classFile != null) {
        try {
          byte[] bytes = classFile.getBytes();
          char[] loc = classFile.fileName();
          ClassFileReader cfr = new ClassFileReader(bytes, loc);
          return new NameEnvironmentAnswer(cfr, null);
        } catch (ClassFormatException e) {
          throw new RuntimeException("Unexpectedly unable to parse class file",
              e);
        }
      }
      return null;
    }

    public boolean isPackage(char[][] parentPkg, char[] pkg) {
      final char[] pathChars = CharOperation.concatWith(parentPkg, pkg, '.');
      String packageName = String.valueOf(pathChars);
      // System.out.println("isPackage: " + packageName);
      return packages.contains(packageName);
    }
  }

  private static class ResourceAdapter implements ICompilationUnit {

    private final MockResource sourceFile;

    public ResourceAdapter(MockResource resource) {
      sourceFile = resource;
    }

    public char[] getContents() {
      try {
        return Shared.readSource(sourceFile).toCharArray();
      } catch (IOException ex) {
        fail("Couldn't read sourceFile: " + sourceFile + " - " + ex);
      }
      return null;
    }

    public char[] getFileName() {
      return sourceFile.getLocation().toCharArray();
    }

    public char[] getMainTypeName() {
      return Shared.getShortName(Shared.getTypeName(sourceFile)).toCharArray();
    }

    public char[][] getPackageName() {
      return CharOperation.splitOn('.', Shared.getPackageName(
          Shared.getTypeName(sourceFile)).toCharArray());
    }

    @Override
    public String toString() {
      return sourceFile.toString();
    }
  }

  protected Map<String, ClassFile> classFiles = new HashMap<String, ClassFile>();

  /**
   * New object for each test case.
   */
  protected INameEnvironmentImpl environment = new INameEnvironmentImpl();

  protected Set<String> packages = new HashSet<String>();

  /**
   * New object for each test case.
   */
  protected ICompilerRequestorImpl requestor = new ICompilerRequestorImpl();

  /**
   * New object for each test case.
   */
  CompilerImpl compiler = new CompilerImpl(environment, requestor);

  public void testIncrementalBuild() {
    List<MockResource> resources = new ArrayList<MockResource>();
    Collections.addAll(resources, JavaResourceBase.getStandardResources());
    resources.add(JavaResourceBase.FOO);
    doCompile(resources);

    // Now incremental build the bar cup.
    doCompile(Arrays.asList(JavaResourceBase.BAR));
  }

  public void testSingleBuild() {
    List<MockResource> resources = new ArrayList<MockResource>();
    Collections.addAll(resources, JavaResourceBase.getStandardResources());
    resources.add(JavaResourceBase.FOO);
    resources.add(JavaResourceBase.BAR);
    doCompile(resources);
  }

  private void doCompile(List<? extends MockResource> resources) {
    ICompilationUnit[] icus = new ICompilationUnit[resources.size()];
    for (int i = 0; i < icus.length; ++i) {
      icus[i] = new ResourceAdapter(resources.get(i));
    }
    compiler.compile(icus);
  }
}
