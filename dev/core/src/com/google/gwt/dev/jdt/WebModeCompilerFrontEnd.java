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
import com.google.gwt.dev.jdt.FindDeferredBindingSitesVisitor.DeferredBindingSite;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Provides a reusable front-end based on the JDT compiler that incorporates
 * GWT-specific concepts such as JSNI and deferred binding.
 */
public class WebModeCompilerFrontEnd extends AstCompiler {

  private final RebindPermutationOracle rebindPermOracle;

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
      CompilationUnitDeclaration cud) {
    Set dependentTypeNames = new HashSet();
    FindJsniRefVisitor v = new FindJsniRefVisitor(dependentTypeNames);
    cud.traverse(v, cud.scope);
    return (String[]) dependentTypeNames.toArray(Empty.STRINGS);
  }

  /**
   * Pull in types implicitly referenced through rebind answers.
   */
  protected String[] doFindAdditionalTypesUsingRebinds(TreeLogger logger,
      CompilationUnitDeclaration cud) {
    Set dependentTypeNames = new HashSet();

    // Find all the deferred binding request types.
    //
    Map requestedTypes = new HashMap();
    FindDeferredBindingSitesVisitor v = new FindDeferredBindingSitesVisitor(
        requestedTypes);
    cud.traverse(v, cud.scope);

    // For each, ask the host for every possible deferred binding answer.
    //
    for (Iterator iter = requestedTypes.keySet().iterator(); iter.hasNext();) {
      String reqType = (String) iter.next();
      DeferredBindingSite site = (DeferredBindingSite) requestedTypes.get(reqType);

      try {
        String[] resultTypes = rebindPermOracle.getAllPossibleRebindAnswers(
            getLogger(), reqType);
        // Check that each result is instantiable.
        for (int i = 0; i < resultTypes.length; ++i) {
          String typeName = resultTypes[i];

          // This causes the compiler to find the additional type, possibly
          // winding its back to ask for the compilation unit from the source
          // oracle.
          //
          ReferenceBinding type = resolvePossiblyNestedType(typeName);

          // Sanity check rebind results.
          if (type == null) {
            FindDeferredBindingSitesVisitor.reportRebindProblem(site,
                "Rebind result '" + typeName + "' could not be found");
            continue;
          }
          if (!type.isClass()) {
            FindDeferredBindingSitesVisitor.reportRebindProblem(site,
                "Rebind result '" + typeName + "' must be a class");
            continue;
          }
          if (type.isAbstract()) {
            FindDeferredBindingSitesVisitor.reportRebindProblem(site,
                "Rebind result '" + typeName + "' cannot be abstract");
            continue;
          }
          if (type.isNestedType() && !type.isStatic()) {
            FindDeferredBindingSitesVisitor.reportRebindProblem(site,
                "Rebind result '" + typeName
                    + "' cannot be a non-static nested class");
            continue;
          }
          if (type.isLocalType()) {
            FindDeferredBindingSitesVisitor.reportRebindProblem(site,
                "Rebind result '" + typeName + "' cannot be a local class");
            continue;
          }
          // Look for a noArg ctor.
          MethodBinding noArgCtor = type.getExactMethod("<init>".toCharArray(),
              TypeBinding.NoParameters, cud.scope);

          if (noArgCtor == null) {
            FindDeferredBindingSitesVisitor.reportRebindProblem(site,
                "Rebind result '" + typeName
                    + "' has no default (zero argument) constructors.");
            continue;
          }
          dependentTypeNames.add(typeName);
        }
        Util.addAll(dependentTypeNames, resultTypes);
      } catch (UnableToCompleteException e) {
        FindDeferredBindingSitesVisitor.reportRebindProblem(site,
            "Failed to resolve '" + reqType + "' via deferred binding.");
      }
    }
    return (String[]) dependentTypeNames.toArray(Empty.STRINGS);
  }
}
