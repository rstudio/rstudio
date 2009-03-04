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
import com.google.gwt.dev.javac.JdtCompiler.CompilationUnitAdapter;
import com.google.gwt.dev.jdt.FindDeferredBindingSitesVisitor.MessageSendSite;
import com.google.gwt.dev.jjs.impl.FragmentLoaderCreator;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.JsniRef;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides a reusable front-end based on the JDT compiler that incorporates
 * GWT-specific concepts such as JSNI and deferred binding.
 */
public class WebModeCompilerFrontEnd extends AbstractCompiler {

  private final FragmentLoaderCreator fragmentLoaderCreator;
  private final RebindPermutationOracle rebindPermOracle;

  /**
   * Construct a WebModeCompilerFrontEnd. The reason a
   * {@link FragmentLoaderCreator} needs to be passed in is that it uses
   * generator infrastructure, and therefore needs access to more parts of the
   * compiler than WebModeCompilerFrontEnd currently has.
   */
  public WebModeCompilerFrontEnd(CompilationState compilationState,
      RebindPermutationOracle rebindPermOracle,
      FragmentLoaderCreator fragmentLoaderCreator) {
    super(compilationState, false);
    this.rebindPermOracle = rebindPermOracle;
    this.fragmentLoaderCreator = fragmentLoaderCreator;
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
     * Compile, which will pull in everything else via
     * doFindAdditionalTypesUsingMagic()
     */
    CompilationUnitDeclaration[] cuds = compile(logger,
        icus.toArray(new ICompilationUnit[icus.size()]));
    return cuds;
  }

  public RebindPermutationOracle getRebindPermutationOracle() {
    return rebindPermOracle;
  }

  @Override
  protected void doCompilationUnitDeclarationValidation(
      CompilationUnitDeclaration cud, TreeLogger logger) {
    /*
     * Anything that makes it here was already checked by AstCompiler while
     * building TypeOracle; no need to rerun checks.
     */
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
   * Pull in types implicitly referenced through rebind answers.
   */
  @Override
  protected String[] doFindAdditionalTypesUsingRebinds(TreeLogger logger,
      CompilationUnitDeclaration cud) {
    Set<String> dependentTypeNames = new HashSet<String>();

    // Find all the deferred binding request types.
    FindDeferredBindingSitesVisitor v = new FindDeferredBindingSitesVisitor();
    cud.traverse(v, cud.scope);
    Map<String, MessageSendSite> requestedTypes = v.getSites();

    // For each, ask the host for every possible deferred binding answer.
    for (String reqType : requestedTypes.keySet()) {
      MessageSendSite site = requestedTypes.get(reqType);

      try {
        String[] resultTypes = rebindPermOracle.getAllPossibleRebindAnswers(
            logger, reqType);
        // Check that each result is instantiable.
        for (int i = 0; i < resultTypes.length; ++i) {
          String typeName = resultTypes[i];

          // This causes the compiler to find the additional type, possibly
          // winding its back to ask for the compilation unit from the source
          // oracle.
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
          MethodBinding noArgCtor = type.getExactConstructor(TypeBinding.NO_PARAMETERS);
          if (noArgCtor == null) {
            FindDeferredBindingSitesVisitor.reportRebindProblem(site,
                "Rebind result '" + typeName
                    + "' has no default (zero argument) constructors");
            continue;
          }
          dependentTypeNames.add(typeName);
        }
        Collections.addAll(dependentTypeNames, resultTypes);
      } catch (UnableToCompleteException e) {
        FindDeferredBindingSitesVisitor.reportRebindProblem(site,
            "Failed to resolve '" + reqType + "' via deferred binding");
      }
    }

    /*
     * Create a a fragment loader for each GWT.runAsync call. They must be
     * created now, rather than in ReplaceRunAsyncs, because all generated
     * classes need to be created before GenerateJavaAST. Note that the loaders
     * created are not yet associated with the specific sites. The present task
     * is only to make sure that enough loaders exist. The real association
     * between loaders and runAsync sites will be made in ReplaceRunAsyncs.
     */
    for (MessageSendSite site : v.getRunAsyncSites()) {
      FragmentLoaderCreator loaderCreator = fragmentLoaderCreator;
      String resultType;
      try {
        resultType = loaderCreator.create(logger);
        dependentTypeNames.add(resultType);
      } catch (UnableToCompleteException e) {
        FindDeferredBindingSitesVisitor.reportRebindProblem(site,
            "Failed to create a runAsync fragment loader");
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
