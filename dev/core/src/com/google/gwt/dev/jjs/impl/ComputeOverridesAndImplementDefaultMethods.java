/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.collect.FluentIterable;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.LinkedHashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes the transitive override relationships between methods and stored them in
 * {@link JMethod}, accessible through {@link JMethod#getOverriddenMethods()} and
 * {@link JMethod#getOverridingMethods()}.
 * <p>
 * In the process of computing overrides, some classes require the addition of stubs or forwarding
 * methods in order to implement default interface methods and to account for
 * accidental overrides
 * <p>
 *  NOTE: Accidental overrides arise when a class {@code A} defines an instance method {@code m()},
 *  an unrelated interface {@code I} also declares a method with the same signature and a second
 *  class {@code B} extends {@code A} and implements {@code I}. In this case {@code m()} at
 *  {@code B} is an override of {@code I.m()} but is inherited from {@code A} making {@code A.m()}
 *  override a method on an unrelated override.
 *  <p>
 *  Modeling accidental overrides in the naive way, i.e. marking the superclass method as overriding
 *  a method from an unrelated interface, has many undesirable consequences, the worst of which is
 *  that it makes the override relation non-local (only correct with global information.
 *  <p>
 *  A simple solution is to explicitly add a (forwarding) stub to the class that inherits the
 *  method and implements the interface.
 */
public class ComputeOverridesAndImplementDefaultMethods {
  // Maps signatures to methods at each type, package private methods have the package prepended
  // to the signature to represent classes that have multiple package private methods with the
  // same signatures declared at classes in different packages.
  private final Map<JType, Map<String, JMethod>> polymorphicMethodsByExtendedSignatureByType =
      Maps.newLinkedHashMap();
  private final  Map<JMethod, JMethod> defaultMethodsByForwardingMethod = Maps.newHashMap();
  // List of (forwarding) stub methods created by this pass.
  private final List<JMethod> newStubMethods = Lists.newArrayList();

  /**
   * Returns the methods created by this pass. These methods are created due to default
   * declarations or due to accidental overrides.
   */
  public List<JMethod> exec(JProgram program) {
    for (JDeclaredType type : program.getDeclaredTypes()) {
      computeOverrides(type);
    }
    return newStubMethods;
  }

  /**
   * Compute all overrides and accumulate newly created methods.
   * <p>
   * Every method that is dispatchable at type {@code type} will be recorded in
   * {@code polymorphicMethodsByExtendedSignatureByType}. Package private method will have the
   * package qualified name prepended to the signature.
   * <p>
   * NOTE: {@code polymorphicMethodsByExtendedSignatureByType} is Map and not a multimap to
   * distinguish between the absence of a type and a type with no methods.
   * The absence of a type means that the type has not been processed yet.
   */
  private void computeOverrides(JDeclaredType type) {
    if (type == null || polymorphicMethodsByExtendedSignatureByType.containsKey(type)) {
      // Already computed.
      return;
    }

    // Compute overrides of all superclasses recursively.
    JClassType superClass = type.getSuperClass();
    computeOverrides(superClass);
    for (JInterfaceType implementedInterface : type.getImplements()) {
      computeOverrides(implementedInterface);
    }

    // At this point we can assume that the override computation for superclasses and
    // superinterfaces is correct and all their synthetic virtual override forwarding stubs are
    // in place

    // Initialize the entries for the current types with its super and declared polymorphic
    // methods.
    Map<String, JMethod> polymorphicMethodsByExtendedSignature = Maps.newLinkedHashMap();
    polymorphicMethodsByExtendedSignatureByType.put(type, polymorphicMethodsByExtendedSignature);
    if (polymorphicMethodsByExtendedSignatureByType.containsKey(type.getSuperClass())) {
      polymorphicMethodsByExtendedSignature
          .putAll(polymorphicMethodsByExtendedSignatureByType.get(type.getSuperClass()));
    }

    // Compute the override relationships among just the methods defined in classes
    for (JMethod method : type.getMethods()) {
      String extendedSignature = computeExtendedSignature(method);
      if (extendedSignature != null) {
        JMethod overriddenMethod = polymorphicMethodsByExtendedSignature.get(extendedSignature);
        if (overriddenMethod == null) {
          maybeAddPublicOverrideToPackagePrivateMethod(method);
        } else {
          addOverridingMethod(overriddenMethod, method);
        }
        polymorphicMethodsByExtendedSignature.put(extendedSignature, method);
      }
    }

    // Find all interface methods, if there is a default implementations it will be first.
    Multimap<String, JMethod> interfaceMethodsBySignature
        = collectMostSpecificSuperInterfaceMethodsBySignature(type);

    // Compute interface overrides, fix accidental overrides and implement default methods.
    for (String signature : interfaceMethodsBySignature.keySet()) {
      Collection<JMethod> interfaceMethods = interfaceMethodsBySignature.get(signature);
      JMethod implementingMethod = polymorphicMethodsByExtendedSignature.get(signature);
      if (implementingMethod == null) {
        // See if there is a package private method whose visibility is made public by the
        // override (can actually only happen for abstract methods, as it is a compiler error
        // otherwise.
        implementingMethod = polymorphicMethodsByExtendedSignature.get(
            computePackagePrivateSignature(type.getPackageName(), signature));
      }
      if (implementingMethod == null || implementingMethod.getEnclosingType() != type) {
        implementingMethod = maybeAddSyntheticOverride(type, implementingMethod, interfaceMethods);
        if (implementingMethod == null) {
          assert type instanceof JInterfaceType;
          assert interfaceMethods.size() == 1;
          polymorphicMethodsByExtendedSignature.put(signature, interfaceMethods.iterator().next());
          continue;
        }
        newStubMethods.add(implementingMethod);
      }
      if (implementingMethod.getEnclosingType() == type) {
        for (JMethod interfaceMethod : interfaceMethods) {
          addOverridingMethod(interfaceMethod, implementingMethod);
        }
      }
    }
  }

  /**
   * If {@code method} overrides a package private method and increases its visibility to public
   * mark the override and add the package private dispatch signature for this method.
   * @param method
   */
  private void maybeAddPublicOverrideToPackagePrivateMethod(JMethod method) {
    if (method.isPackagePrivate()) {
      return;
    }
    Map<String, JMethod> polymorphicMethodsByExtendedSignature =
        polymorphicMethodsByExtendedSignatureByType.get(method.getEnclosingType());
    // if the method is not package private, check whether it overrides a package private
    // method.
    String packagePrivateSignature = computePackagePrivateSignature(method);
    JMethod packagePrivateOverriddenMethod =
        polymorphicMethodsByExtendedSignature.get(packagePrivateSignature);
    if (packagePrivateOverriddenMethod != null) {
      // Overrides a package private method and makes it public.
      addOverridingMethod(packagePrivateOverriddenMethod, method);
      polymorphicMethodsByExtendedSignature.put(packagePrivateSignature, method);
    }
  }

  /**
   * Adds overridden/overriding information to the corresponding JMethods.
   */
  private static void addOverridingMethod(JMethod overriddenMethod, JMethod overridingMethod) {
    assert overriddenMethod != overridingMethod : overriddenMethod + " can not override itself";
    overridingMethod.addOverriddenMethod(overriddenMethod);
    overriddenMethod.addOverridingMethod(overridingMethod);
    for (JMethod transitivelyOverriddenMethod : overriddenMethod.getOverriddenMethods()) {
      overridingMethod.addOverriddenMethod(transitivelyOverriddenMethod);
      transitivelyOverriddenMethod.addOverridingMethod(overridingMethod);
    }
  }

  /**
   * Returns a unique extended signature for the method. An extended signature is the method
   * signature if the method is public; otherwise if the method is package private the extended
   * signature is the method signature prepended the package.
   * <p>
   * Allows to represent package private dispatch when unrelated package private methods have the
   * same signature.
   */
  private static String computeExtendedSignature(JMethod method) {
    if (!method.canBePolymorphic()) {
      return null;
    }
    if (method.isPackagePrivate()) {
      return computePackagePrivateSignature(method);
    }
    return method.getSignature();
  }

  /**
   * Returns the signature of {@code method} as if {@code method} was package private.
   */
  private static String computePackagePrivateSignature(JMethod method) {
    String packageName = method.getEnclosingType().getPackageName();
    return computePackagePrivateSignature(packageName, method.getSignature());
  }

  private static String computePackagePrivateSignature(String packageName, String publicSignature) {
    return StringInterner.get().intern(packageName + "." + publicSignature);
  }

  /**
   * Adds a synthetic override if needed.
   * <p>
   * This is used for two main reasons:
   * <ul>
   *   <li>1. to add a concrete implementation for a default method</li>
   *   <li>2. to add a virtual override to account more precisely for accidental overrides</li>
   *   <li>3. to add declaration stub in the case where an interface inherits a method from more
   *          more than one super class</li>
   * </ul>
   * <p>
   * Returns {@code null} if there is there is no need to add a stub; this is only the case for
   * interfaces when is only one declaration of the method in the super interface hierarchy.
   */
  private JMethod maybeAddSyntheticOverride(
      JDeclaredType type, JMethod superMethod, Collection<JMethod> interfaceMethods) {

    // If there is a default implementation it will be first and the only default in the collection
    // (as multiple "active" defaults are a compiler error).
    JMethod interfaceMethod = interfaceMethods.iterator().next();
    assert !interfaceMethod.isStatic();

    JMethod implementingMethod = superMethod;

    // Only populate classes with stubs, forwarding methods or default implementations.
    if (needsDefaultImplementationStubMethod(type, superMethod, interfaceMethod)) {

      assert FluentIterable.from(interfaceMethods).filter(new Predicate<JMethod>() {
        @Override
        public boolean apply(JMethod jMethod) {
          return jMethod.isDefaultMethod();
        }
      }).size() == 1 : "Ambiguous default method resolution for class " + type.getName() +
          " conflicting methods " +
          Iterables.toString(FluentIterable.from(interfaceMethods).filter(
              new Predicate<JMethod>() {
                @Override
                public boolean apply(JMethod jMethod) {
                  return jMethod.isDefaultMethod();
                }
              }));

      // Create a forwarding method to the correct default method.
      implementingMethod = JjsUtils.createForwardingMethod(type, interfaceMethod);

      defaultMethodsByForwardingMethod.put(implementingMethod, interfaceMethod);
    } else if (superMethod == null && interfaceMethod.isAbstract() &&
        (type instanceof JClassType || interfaceMethods.size() > 1)) {
      // It is an abstract stub
      implementingMethod = JjsUtils.createSyntheticAbstractStub(type, interfaceMethod);
    } else if (type instanceof JClassType && superMethod.getEnclosingType() != type &&
        !FluentIterable.from(interfaceMethods)
            .allMatch(Predicates.in(superMethod.getOverriddenMethods()))) {
        // the implementing method does not override all interface declared methods with the same
        // signature.
      if (superMethod.isAbstract()) {
        implementingMethod = JjsUtils.createSyntheticAbstractStub(type, interfaceMethod);
      } else {
        // Creates a forwarding method to act as the place holder for this accidental override.
        implementingMethod = JjsUtils.createForwardingMethod(type, superMethod);
        implementingMethod.setSyntheticAccidentalOverride();

        if (superMethod.isFinal()) {
          // To keep consistency we reset the final mark
          superMethod.setFinal(false);
        }
      }
    }

    if (implementingMethod != null) {
      polymorphicMethodsByExtendedSignatureByType.get(type)
          .put(implementingMethod.getSignature(), implementingMethod);

      if (superMethod != null && superMethod != implementingMethod) {
        addOverridingMethod(superMethod, implementingMethod);
      }
    }
    return implementingMethod;
  }

  /**
   * Return true if the type {@code type} need to replace {@code superMethod} (possibly {@code null})
   * with a (forwarding) stub due to default {@code interfaceMethod}.
   */
  private boolean needsDefaultImplementationStubMethod(
      JDeclaredType type, JMethod superMethod, JMethod interfaceMethod) {
    if (!interfaceMethod.isDefaultMethod() || type instanceof JInterfaceType) {
      // Only implement default methods in classes.
      return false;
    }
    if (superMethod == null || (superMethod.isAbstract() && superMethod.isSynthetic())) {
      // The interface method is not implemented or an abstract stub was synthesized as the super
      // (not necessarily direct) implementation.
      return true;
    }
    JMethod superForwardingMethod = defaultMethodsByForwardingMethod.get(superMethod);
    // A default superMethod stub is in place in the supertype, and needs to be replaced if it does
    // not forward to the required default implementation.
    return superForwardingMethod != null
        && superForwardingMethod.isDefaultMethod()
        && superForwardingMethod != interfaceMethod;
  }

  /**
   * Collects all interface methods by signature so that (1) methods in the final set do not
   * have overrides in the set, and (2) if there is a default implementation for a signature, it
   * appears first.
   * <p>
   * NOTE: There should not be any ambiguity (e.g. to conflicting defaults), those cases should
   * have been a compilation error in JDT.
   */
  private Multimap<String, JMethod> collectMostSpecificSuperInterfaceMethodsBySignature(
      JDeclaredType type) {
    Multimap<String, JMethod> interfaceMethodsBySignature = LinkedHashMultimap.create();
    collectAllSuperInterfaceMethodsBySignature(type, interfaceMethodsBySignature);

    List<String> signatures = Lists.newArrayList(interfaceMethodsBySignature.keySet());
    for (String signature : signatures) {
      Collection<JMethod> allMethods = interfaceMethodsBySignature.get(signature);
      Set<JMethod> notOverriddenMethods  = Sets.newLinkedHashSet(allMethods);
      for (JMethod method : allMethods) {
        notOverriddenMethods =
            Sets.difference(notOverriddenMethods, method.getOverriddenMethods());
      }
      Set<JMethod> defaultMethods = FluentIterable.from(notOverriddenMethods).filter(
          new Predicate<JMethod>() {
            @Override
            public boolean apply(JMethod method) {
              return method.isDefaultMethod();
            }
          }).toSet();
      Set<JMethod> leafMethods = Sets.newLinkedHashSet(defaultMethods);
      leafMethods.addAll(notOverriddenMethods);
      interfaceMethodsBySignature.replaceValues(signature, leafMethods);
    }

    return interfaceMethodsBySignature;
  }

  private void collectAllSuperInterfaceMethodsBySignature(JDeclaredType type,
      Multimap<String, JMethod> methodsBySignature) {
    for (JDeclaredType superType: type.getImplements()) {
      for (JMethod method : polymorphicMethodsByExtendedSignatureByType.get(superType).values()) {
        methodsBySignature.put(computeExtendedSignature(method), method);
      }
    }
  }
}
