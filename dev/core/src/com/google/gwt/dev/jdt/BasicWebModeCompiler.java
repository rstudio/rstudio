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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.CompiledClass;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.dev.util.Memory;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides a basic front-end based on the JDT compiler that incorporates
 * GWT-specific concepts such as JSNI.
 */
public class BasicWebModeCompiler extends AbstractCompiler {

  public static CompilationUnitDeclaration[] getCompilationUnitDeclarations(
      TreeLogger logger, CompilationState state, String... seedTypeNames)
      throws UnableToCompleteException {
    return new BasicWebModeCompiler(state).getCompilationUnitDeclarations(
        logger, seedTypeNames);
  }

  /**
   * Construct a BasicWebModeCompiler.
   */
  public BasicWebModeCompiler(CompilationState compilationState) {
    super(compilationState, false);
  }

  /**
   * Build the initial set of compilation units.
   */
  public CompilationUnitDeclaration[] getCompilationUnitDeclarations(
      TreeLogger logger, String[] seedTypeNames)
      throws UnableToCompleteException {

    TypeOracle oracle = compilationState.getTypeOracle();
    Set<JClassType> intfTypes = oracle.getSingleJsoImplInterfaces();
    Map<String, CompiledClass> classMapBySource = compilationState.getClassFileMapBySource();

    /*
     * The alreadyAdded set prevents duplicate CompilationUnits from being added
     * to the icu list in the case of multiple JSO implementations as inner
     * classes in the same top-level class or seed classes as SingleJsoImpls
     * (e.g. JSO itself as the SingleImpl for all tag interfaces).
     */
    Set<CompilationUnit> alreadyAdded = new HashSet<CompilationUnit>();

    List<ICompilationUnit> icus = new ArrayList<ICompilationUnit>(
        seedTypeNames.length + intfTypes.size());

    for (String seedTypeName : seedTypeNames) {
      CompilationUnit unit = getUnitForType(logger, classMapBySource,
          seedTypeName);

      if (alreadyAdded.add(unit)) {
        icus.add(new CompilationUnitAdapter(unit));
      } else {
        logger.log(TreeLogger.WARN, "Duplicate compilation unit '"
            + unit.getDisplayLocation() + "'in seed types");
      }
    }

    /*
     * Add all SingleJsoImpl types that we know about. It's likely that the
     * concrete types are never explicitly referenced from the seed types.
     */
    for (JClassType intf : intfTypes) {
      String implName = oracle.getSingleJsoImpl(intf).getQualifiedSourceName();
      CompilationUnit unit = getUnitForType(logger, classMapBySource, implName);

      if (alreadyAdded.add(unit)) {
        icus.add(new CompilationUnitAdapter(unit));
        logger.log(TreeLogger.SPAM, "Forced compilation of unit '"
            + unit.getDisplayLocation()
            + "' becasue it contains a SingleJsoImpl type");
      }
    }

    /*
     * Compile, which will pull in everything else via the
     * doFindAdditionalTypesUsingFoo() methods.
     */
    CompilationUnitDeclaration[] cuds = compile(logger,
        icus.toArray(new ICompilationUnit[icus.size()]));
    Memory.maybeDumpMemory("WebModeCompiler");
    return cuds;
  }

  /**
   * Pull in types referenced only via JSNI.
   */
  @Override
  protected String[] doFindAdditionalTypesUsingJsni(TreeLogger logger,
      CompilationUnitDeclaration cud) {
    FindJsniRefVisitor v = new FindJsniRefVisitor();
    cud.traverse(v, cud.scope);
    Set<String> jsniRefs = v.getJsniRefs();
    Set<String> dependentTypeNames = new HashSet<String>();
    for (String jsniRef : jsniRefs) {
      JsniRef parsed = JsniRef.parse(jsniRef);
      if (parsed != null) {
        // If we fail to parse, don't add a class reference.
        dependentTypeNames.add(parsed.className());
      }
    }
    return dependentTypeNames.toArray(Empty.STRINGS);
  }

  /**
   * Get the CompilationUnit for a named type or throw an
   * UnableToCompleteException.
   */
  private CompilationUnit getUnitForType(TreeLogger logger,
      Map<String, CompiledClass> classMapBySource, String typeName)
      throws UnableToCompleteException {

    CompiledClass compiledClass = classMapBySource.get(typeName);
    if (compiledClass == null) {
      logger.log(TreeLogger.ERROR, "Unable to find compilation unit for type '"
          + typeName + "'");
      throw new UnableToCompleteException();
    }

    assert compiledClass.getUnit() != null;
    return compiledClass.getUnit();
  }
}
