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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class WebModeCompilerFrontEnd extends AstCompiler {

  public WebModeCompilerFrontEnd(SourceOracle sourceOracle,
      RebindPermutationOracle rebindPermOracle) {
    super(sourceOracle);
    this.rebindPermOracle = rebindPermOracle;
  }

  public CompilationUnitDeclaration[] getCompilationUnitDeclarations(
      TreeLogger logger, String[] seedTypeNames)
      throws UnableToCompleteException {

    // Build the initial set of compilation units.
    //
    ICompilationUnit[] units = new ICompilationUnit[seedTypeNames.length];
    for (int i = 0; i < seedTypeNames.length; i++) {
      String seedTypeName = seedTypeNames[i];
      units[i] = getCompilationUnitForType(logger, seedTypeName);
    }

    // Compile, which will pull in everything else via
    // doFindAdditionalTypesUsingMagic()
    //
    CompilationUnitDeclaration[] cuds = compile(logger, units);
    return cuds;
  }

  public RebindPermutationOracle getRebindPermutationOracle() {
    return rebindPermOracle;
  }

  /**
   * Pull in types referenced only via JSNI.
   */
  protected String[] doFindAdditionalTypesUsingJsni(TreeLogger logger,
      CompilationUnitDeclaration cud) throws UnableToCompleteException {
    Set dependentTypeNames = new HashSet();
    FindJsniRefVisitor v = new FindJsniRefVisitor(dependentTypeNames);
    cud.traverse(v, cud.scope);
    return (String[]) dependentTypeNames.toArray(Empty.STRINGS);
  }

  /**
   * Pull in types implicitly referenced through rebind answers.
   */
  protected String[] doFindAdditionalTypesUsingRebinds(TreeLogger logger,
      CompilationUnitDeclaration cud) throws UnableToCompleteException {
    Set dependentTypeNames = new HashSet();
    
    // Find all the deferred binding request types.
    //
    Set requestedTypes = new HashSet();
    FindDeferredBindingSitesVisitor v = new FindDeferredBindingSitesVisitor(
      requestedTypes);
    cud.traverse(v, cud.scope);
    
    // For each, ask the host for every possible deferred binding answer.
    //
    for (Iterator iter = requestedTypes.iterator(); iter.hasNext();) {
      String reqType = (String) iter.next();
      String[] resultTypes = rebindPermOracle.getAllPossibleRebindAnswers(
        getLogger(), reqType);
      
      Util.addAll(dependentTypeNames, resultTypes);
    }
    return (String[]) dependentTypeNames.toArray(Empty.STRINGS);
  }

  private final RebindPermutationOracle rebindPermOracle;
}
