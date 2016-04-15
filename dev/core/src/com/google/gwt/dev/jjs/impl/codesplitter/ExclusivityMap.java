/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.jjs.impl.codesplitter;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * A map from program atoms to fragments; each fragment may contain more than one runAsync.
 * Maps atom to the fragments, if any, that they are exclusive to. Atoms not
 * exclusive to any fragment are either mapped to NOT_EXCLUSIVE, or left out of the map entirely.
 * Note that the map is incomplete; any entry not included has not been proven to be exclusive.
 * Also, note that the initial load sequence is assumed to already be loaded.
 */
class ExclusivityMap {
  /**
   * A liveness predicate that is based on an exclusivity map.
   */
  private class ExclusivityMapLivenessPredicate implements LivenessPredicate {
    private final Fragment fragment;

    public ExclusivityMapLivenessPredicate(Fragment fragment) {
      this.fragment = fragment;
    }

    @Override
    public boolean isLive(JDeclaredType type) {
      return isLiveInFragment(fragment, type);
    }

    @Override
    public boolean isLive(JField field) {
      return isLiveInFragment(fragment, field);
    }

    @Override
    public boolean isLive(JMethod method) {
      return isLiveInFragment(fragment, method);
    }

    @Override
    public boolean miscellaneousStatementsAreLive() {
      return true;
    }
  }

  /**
   * A dummy fragment that represents atoms that are not in the map.
   */
  public static final Fragment NOT_EXCLUSIVE = new Fragment(Fragment.Type.NOT_EXCLUSIVE) {
    @Override
    public int getFragmentId() {
      throw makeUnsupportedException("getFragmentId");
    }

    @Override
    public List<JsStatement> getStatements() {
      throw makeUnsupportedException("getStatements");
    }

    @Override
    public void setStatements(List<JsStatement> statements) {
      throw makeUnsupportedException("setStatements");
    }

    @Override
    public void addStatements(List<JsStatement> statements) {
      throw makeUnsupportedException("addStatements");
    }

    @Override
    public Set<JRunAsync> getRunAsyncs() {
      throw makeUnsupportedException("getRunAsyncs");
    }

    @Override
    public void addRunAsync(JRunAsync runAsync) {
      throw makeUnsupportedException("addSplitPoint");
    }

    @Override
    public void setFragmentId(int fragmentId) {
      throw makeUnsupportedException("setFragmentId");
    }

    private UnsupportedOperationException makeUnsupportedException(String methodName) {
      return new UnsupportedOperationException(methodName + " is not supported in the "
          + "dummy NOT_EXCLUSIVE fragment");
    }
  };

  /**
   * Gets the liveness predicate for fragment.
   */
  LivenessPredicate getLivenessPredicate(Fragment fragment) {
    return new ExclusivityMapLivenessPredicate(fragment);
  }

  /**
   * Determine whether a field is live in a fragment.
   */
  public boolean isLiveInFragment(Fragment fragment, JField field) {
    return isLiveInFragment(fragmentForField, field, fragment);
  }

  /**
   * Determine whether a method is live in a fragment.
   */
  public boolean isLiveInFragment(Fragment fragment, JMethod method) {
    return isLiveInFragment(fragmentForMethod, method, fragment);
  }

  /**
   * Determine whether a type is live in a fragment.
   */
  public boolean isLiveInFragment(Fragment fragment, JDeclaredType type) {
    return isLiveInFragment(fragmentForType, type, fragment);
  }

  private Map<JField, Fragment> fragmentForField = Maps.newHashMap();
  private Map<JMethod, Fragment> fragmentForMethod = Maps.newHashMap();
  private Map<JDeclaredType, Fragment> fragmentForType = Maps.newHashMap();

  /**
   * Traverse {@code exp} and find all referenced class literals.
   */
  private static Set<JClassLiteral> classLiteralsIn(JExpression expression) {
    final Set<JClassLiteral> literals = Sets.newHashSet();
    new JVisitor() {
      @Override
      public void endVisit(JClassLiteral classLiteral, Context ctx) {
        literals.add(classLiteral);
      }
    }.accept(expression);
    return literals;
  }

  /**
   * Traverse {@code exp} and find all referenced JMethods.
   */
  private static Set<JMethod> methodsReferencesIn(JExpression expression) {
    final Set<JMethod> methods = Sets.newHashSet();
    new JVisitor() {
      @Override
      public void endVisit(JsniMethodRef jsniMethodRef, Context ctx) {
        methods.add(jsniMethodRef.getTarget());
      }
    }.accept(expression);
    return methods;
  }

  /**
   * Map atoms to exclusive fragments. Do this by trying to find code atoms that
   * are only needed by a single split point. Such code can be moved to the
   * exclusively live fragment associated with that split point.
   */
  public static ExclusivityMap computeExclusivityMap(Collection<Fragment> exclusiveFragments,
      ControlFlowAnalyzer completeCfa,
      Map<Fragment, ControlFlowAnalyzer> notExclusiveCfaByFragment) {
    ExclusivityMap exclusivityMap = new ExclusivityMap();
    exclusivityMap.compute(exclusiveFragments, completeCfa, notExclusiveCfaByFragment);
    return exclusivityMap;
  }

  /**
   * <p>
   * Patch up the fragment map to satisfy load-order dependencies, as described
   * in the comment of {@link LivenessPredicate}.
   * Load-order dependencies can be
   * violated when an atom is mapped to 0 as a leftover, but it has some
   * load-order dependency on an atom that was put in an exclusive fragment.
   * </p>
   *
   * <p>
   * In general, it might be possible to split things better by considering load
   * order dependencies when building the fragment map. However, fixing them
   * after the fact makes CodeSplitter simpler. In practice, for programs tried
   * so far, there are very few load order dependency fixups that actually
   * happen, so it seems better to keep the compiler simpler.
   * </p>
   *
   * <p>
   * It would be safer and more robust to include the load order dependencies
   * in the general scheme and uniformly use control flow analysis to determine
   * dependencies instead of hand picking atoms to check and fix. Also note that
   * some of the control flow and load dependencies are introduced as the Java
   * AST is translated into JavaScript and hence not visible by ControlFlowAnalyzer.
   * </p>
   *
   * <p>
   * Furthermore, in some cases actual dependencies <i>differ</i> between Java AST and the
   * final JavaScript output. For example whether a field initialization is done at declaration
   * or during instance creation decided by
   * {@link GenerateJavaScriptAST.GenerateJavaScriptVisitor#initializeAtTopScope}. Mismatches
   * like these are handled explicitly by these fixup passes.
   * </p>
   */
  public void fixUpLoadOrderDependencies(TreeLogger logger, JProgram jprogram,
      Set<JMethod> methodsStillInJavaScript) {
    fixUpLoadOrderDependenciesForMethods(logger, jprogram, methodsStillInJavaScript);
    fixUpLoadOrderDependenciesForTypes(logger, jprogram);
    fixUpLoadOrderDependenciesForClassLiterals(logger, jprogram, methodsStillInJavaScript);
  }

  /**
   * Map atoms to exclusive fragments. Do this by trying to find code atoms that
   * are only needed by a single split point. Such code can be moved to the
   * exclusively live fragment associated with that split point.
   */
  private void compute(Collection<Fragment> exclusiveFragments, ControlFlowAnalyzer completeCfa,
      Map<Fragment, ControlFlowAnalyzer> notExclusiveCfaByFragment) {

    Set<JField> allLiveFields = filter(Sets.union(completeCfa.getLiveFieldsAndMethods(),
        completeCfa.getFieldsWritten()), JField.class);
    Set<JMethod> allLiveMethods = filter(completeCfa.getLiveFieldsAndMethods(), JMethod.class);
    Set<JDeclaredType> allLiveTypes =
        filter(completeCfa.getInstantiatedTypes(), JDeclaredType.class);

    for (Fragment fragment : exclusiveFragments) {
      assert fragment.isExclusive();
      ControlFlowAnalyzer complementCfa = notExclusiveCfaByFragment.get(fragment);
      Set<JNode> nodesNotExclusiveToFragment = Sets.union(complementCfa.getLiveFieldsAndMethods(),
          complementCfa.getFieldsWritten());

      putIfAbsent(fragmentForField, fragment,
          Sets.difference(allLiveFields, nodesNotExclusiveToFragment));
      putIfAbsent(fragmentForMethod, fragment,
          Sets.difference(allLiveMethods, complementCfa.getLiveFieldsAndMethods()));
      putIfAbsent(fragmentForType, fragment,
          Sets.difference(allLiveTypes,
              filter(complementCfa.getInstantiatedTypes(), JDeclaredType.class)));
    }

    // Assign all living atoms to left overs.
    putIfAbsent(fragmentForField, NOT_EXCLUSIVE, allLiveFields);
    putIfAbsent(fragmentForMethod, NOT_EXCLUSIVE, allLiveMethods);
    putIfAbsent(fragmentForType, NOT_EXCLUSIVE, allLiveTypes);
  }

  /**
   * A class literal cannot be loaded until all the parameters to its createFor... class are.
   * Make sure that the strings are available for all class literals at the time they are
   * loaded and make sure that superclass class literals are loaded before.
   */
  private void fixUpLoadOrderDependenciesForClassLiterals(
      TreeLogger logger, JProgram jprogram, Set<JMethod> methodsStillInJavaScript) {
    int numFixups = 0;
    /**
     * Consider all static fields of ClassLiteralHolder; the majority if not all its static
     * fields are class literal fields. It is safe to fix up extra fields.
     */
    Queue<JField> potentialClassLiteralFields = new ArrayDeque<JField>(
        jprogram.getTypeClassLiteralHolder().getFields());
    int numClassLiterals = potentialClassLiteralFields.size();

    while (!potentialClassLiteralFields.isEmpty()) {
      JField field = potentialClassLiteralFields.remove();
      if (!field.isStatic()) {
        continue;
      }

      Fragment classLiteralFragment = fragmentForField.get(field);

      // In -XenableClosureFormat creation of class literals needs to happen before or with class
      // definition. This fixup takes care when it is not the case.
      JType type = jprogram.getTypeByClassLiteralField(field);
      Fragment classLiteralTypeFragment = fragmentForType.get(type);
      if (!canReferenceAtomsFrom(classLiteralTypeFragment, classLiteralFragment)) {
        numFixups++;
        fragmentForField.put(field, NOT_EXCLUSIVE);
        classLiteralFragment = NOT_EXCLUSIVE;
      }

      JExpression initializer = field.getInitializer();

      // Fixup the superclass class literals.
      for (JClassLiteral superclassClassLiteral : classLiteralsIn(initializer)) {
        JField superclassClassLiteralField = superclassClassLiteral.getField();
        // Fix the super class literal and add it to the reexamined.
        Fragment superclassClassLiteralFragment = fragmentForField.get(superclassClassLiteralField);
        if (!canReferenceAtomsFrom(classLiteralFragment, superclassClassLiteralFragment)) {
          numFixups++;
          fragmentForField.put(superclassClassLiteralField, NOT_EXCLUSIVE);
          // Add the field back so that its superclass class literal gets fixed if necessary.
          potentialClassLiteralFields.add(superclassClassLiteralField);
        }
      }

      // If there are references to methods move those as well. In particular the enum class
      // literals reference the static methods values() and valueOf() for the particular enum type
      // those methods need to be defined before the class literal.
      for (JMethod referencedMethod : methodsReferencesIn(initializer)) {
        // Move the referenced methods if necessary.
        Fragment referencedMethodFragment = fragmentForMethod.get(referencedMethod);
        if (methodsStillInJavaScript.contains(referencedMethod)
            && !canReferenceAtomsFrom(classLiteralFragment, referencedMethodFragment)) {
          assert referencedMethod.isStatic();
          numFixups++;
          fragmentForMethod.put(referencedMethod, NOT_EXCLUSIVE);
        }
      }
    }

    logger.log(TreeLogger.DEBUG, "Fixed up load-order dependencies by moving " +
        numFixups + " fields in class literal constructors to fragment 0, out of " +
        numClassLiterals);
  }

  /**
   * Fixes up the load-order dependencies from instance methods to their enclosing types, in some
   * cases there is some freedom to place instance methods in one of two or more exclusive
   * fragment. That scenario arises when an instance method is only accessible after two or
   * more exclusive fragments have been loaded. In such scenario this fixup will move the method
   * to the fragment where the type is instantiated.
   */
  private void fixUpLoadOrderDependenciesForMethods(TreeLogger logger, JProgram jprogram,
      Set<JMethod> methodsStillInJavaScript) {
    int numFixups = 0;

    for (JDeclaredType type : jprogram.getDeclaredTypes()) {
      Fragment typeFrag = fragmentForType.get(type);
      if (typeFrag == null || !typeFrag.isExclusive()) {
        continue;
      }
      /*
      * If the type is in an exclusive fragment, all its instance methods must be in the same one;
      * if this is not the case move the type to the NOT_EXCLUSIVE fragment.
      */
      for (JMethod method : type.getMethods()) {
        if (method.needsDynamicDispatch() && methodsStillInJavaScript.contains(method)
            && typeFrag != fragmentForMethod.get(method)) {
          fragmentForType.put(type, NOT_EXCLUSIVE);
          numFixups++;
          break;
        }
      }
    }

    logger.log(TreeLogger.DEBUG,
        "Fixed up load-order dependencies for instance methods by moving " + numFixups
            + " types to fragment 0, out of " + jprogram.getDeclaredTypes().size());
  }

  /**
   * Fixes up load order dependencies from types to their supertypes.
   */
  private void fixUpLoadOrderDependenciesForTypes(TreeLogger logger, JProgram jprogram) {
    int numFixups = 0;
    Queue<JDeclaredType> typesToCheck =
        new ArrayDeque<JDeclaredType>(jprogram.getDeclaredTypes().size());
    typesToCheck.addAll(jprogram.getDeclaredTypes());

    while (!typesToCheck.isEmpty()) {
      JDeclaredType type = typesToCheck.remove();
      if (type.getSuperClass() != null) {
        Fragment typeFrag = fragmentForType.get(type);
        Fragment supertypeFrag = fragmentForType.get(type.getSuperClass());
        if (!canReferenceAtomsFrom(typeFrag, supertypeFrag)) {
          numFixups++;
          fragmentForType.put(type.getSuperClass(), NOT_EXCLUSIVE);
          typesToCheck.add(type.getSuperClass());
        }
      }
    }

    logger.log(TreeLogger.DEBUG, "Fixed up load-order dependencies on supertypes by moving "
        + numFixups + " types to fragment 0, out of " + jprogram.getDeclaredTypes().size());
  }

  private static <T> Set<T> filter(Set<?> types, Class<T> clazz) {
    return (Set) Sets.filter(types, Predicates.instanceOf(clazz));
  }

  /**
   * Returns true if atoms in thatFragment are visible from thisFragment.
   */
  private static boolean canReferenceAtomsFrom(Fragment thisFragment, Fragment thatFragment) {
    return thisFragment == null || thisFragment == thatFragment || !thatFragment.isExclusive();
  }

  /**
   * An atom is live in a fragment if either it is exclusive to that fragment or not exclusive
   * to any fragment.
   */
  private static <T> boolean isLiveInFragment(Map<T, Fragment> map, T atom,
      Fragment expectedFragment) {
    Fragment actualFragment = map.get(atom);
    return actualFragment != null &&
        (expectedFragment == actualFragment || !actualFragment.isExclusive());
  }

  private <T> void putIfAbsent(Map<T, Fragment> map, Fragment fragment, Iterable<T> atoms) {
    for (T atom : atoms) {
      if (!map.containsKey(atom)) {
        // Some atoms might atoms might be dead until both split points i and j are reached, and
        // thus they could be assigned to either.
        // We choose here to assign to the first fragment, so that we could use this method
        // to assign leftovers.
        map.put(atom, fragment);
      }
    }
  }
}
