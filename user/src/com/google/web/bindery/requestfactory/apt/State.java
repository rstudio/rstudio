/*
 * Copyright 2011 Google Inc.
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
package com.google.web.bindery.requestfactory.apt;

import com.google.web.bindery.requestfactory.shared.SkipInterfaceValidation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

class State {
  /**
   * Slightly tweaked implementation used when running tests.
   */
  static class ForTesting extends State {
    public ForTesting(ProcessingEnvironment processingEnv) {
      super(processingEnv);
    }

    @Override
    boolean respectAnnotations() {
      return false;
    }
  }

  private static class Job {
    public final TypeElement element;
    public final ScannerBase<?> scanner;

    public Job(TypeElement element, ScannerBase<?> scanner) {
      this.element = element;
      this.scanner = scanner;
    }
  }

  /**
   * Used to take a {@code FooRequest extends Request<Foo>} and find the
   * {@code Request<Foo>} type.
   */
  static TypeMirror viewAs(DeclaredType desiredType, TypeMirror searchFrom, State state) {
    if (!desiredType.getTypeArguments().isEmpty()) {
      throw new IllegalArgumentException("Expecting raw type, received " + desiredType.toString());
    }
    Element searchElement = state.types.asElement(searchFrom);
    switch (searchElement.getKind()) {
      case CLASS:
      case INTERFACE:
      case ENUM: {
        TypeMirror rawSearchFrom = state.types.getDeclaredType((TypeElement) searchElement);
        if (state.types.isSameType(desiredType, rawSearchFrom)) {
          return searchFrom;
        }
        for (TypeMirror s : state.types.directSupertypes(searchFrom)) {
          TypeMirror maybe = viewAs(desiredType, s, state);
          if (maybe != null) {
            return maybe;
          }
        }
        break;
      }
      case TYPE_PARAMETER: {
        // Search <T extends Foo> as Foo
        return viewAs(desiredType, ((TypeVariable) searchElement).getUpperBound(), state);
      }
    }
    return null;
  }

  final Elements elements;
  final DeclaredType entityProxyIdType;
  final DeclaredType entityProxyType;
  final DeclaredType extraTypesAnnotation;
  final DeclaredType instanceRequestType;
  final DeclaredType locatorType;
  final DeclaredType objectType;
  final DeclaredType requestContextType;
  final DeclaredType requestFactoryType;
  final DeclaredType requestType;
  final DeclaredType serviceLocatorType;
  final Set<TypeElement> seen;
  final Types types;
  final DeclaredType valueProxyType;
  private final Map<TypeElement, TypeElement> clientToDomainMain;
  private final List<Job> jobs = new LinkedList<Job>();
  private final Messager messager;
  private boolean poisoned;
  private final boolean suppressErrors;
  private final boolean suppressWarnings;
  private final boolean verbose;
  /**
   * Prevents duplicate messages from being emitted.
   */
  private final Map<Element, Set<String>> previousMessages = new HashMap<Element, Set<String>>();

  private final Set<TypeElement> proxiesRequiringMapping = new LinkedHashSet<TypeElement>();

  public State(ProcessingEnvironment processingEnv) {
    clientToDomainMain = new HashMap<TypeElement, TypeElement>();
    elements = processingEnv.getElementUtils();
    messager = processingEnv.getMessager();
    types = processingEnv.getTypeUtils();
    suppressErrors = Boolean.parseBoolean(processingEnv.getOptions().get("suppressErrors"));
    suppressWarnings = Boolean.parseBoolean(processingEnv.getOptions().get("suppressWarnings"));
    verbose = Boolean.parseBoolean(processingEnv.getOptions().get("verbose"));

    entityProxyType = findType("EntityProxy");
    entityProxyIdType = findType("EntityProxyId");
    extraTypesAnnotation = findType("ExtraTypes");
    instanceRequestType = findType("InstanceRequest");
    locatorType = findType("Locator");
    objectType = findType(Object.class);
    requestType = findType("Request");
    requestContextType = findType("RequestContext");
    requestFactoryType = findType("RequestFactory");
    seen = new HashSet<TypeElement>();
    serviceLocatorType = findType("ServiceLocator");
    valueProxyType = findType("ValueProxy");
  }

  /**
   * Add a mapping from a client type to a domain type.
   */
  public void addMapping(TypeElement clientType, TypeElement domainType) {
    clientToDomainMain.put(clientType, domainType);
  }

  /**
   * Check an element, and, for types, its supertype hierarchy, for an
   * {@code ExtraTypes} annotation.
   */
  public void checkExtraTypes(Element x) {
    (new ScannerBase<Void>() {
      @Override
      public Void visitExecutable(ExecutableElement x, State state) {
        // Check method declaration
        checkForAnnotation(x);
        return null;
      }

      @Override
      public Void visitType(TypeElement x, State state) {
        // Check type's declaration
        checkForAnnotation(x);
        // Look at superclass, if it exists
        if (!x.getSuperclass().getKind().equals(TypeKind.NONE)) {
          scan(state.types.asElement(x.getSuperclass()), state);
        }
        // Check super-interfaces
        for (TypeMirror intf : x.getInterfaces()) {
          scan(state.types.asElement(intf), state);
        }
        return null;
      }

      private void checkForAnnotation(Element x) {
        // Bug similar to Eclipse 261969 makes ExtraTypes.value() unreliable.
        for (AnnotationMirror mirror : x.getAnnotationMirrors()) {
          if (!types.isSameType(mirror.getAnnotationType(), extraTypesAnnotation)) {
            continue;
          }
          // The return of the Class[] value() method
          AnnotationValue value = mirror.getElementValues().values().iterator().next();
          // which is represented by a list
          @SuppressWarnings("unchecked")
          List<? extends AnnotationValue> valueList =
              (List<? extends AnnotationValue>) value.getValue();
          for (AnnotationValue clazz : valueList) {
            TypeMirror type = (TypeMirror) clazz.getValue();
            maybeScanProxy((TypeElement) types.asElement(type));
          }
        }
      }
    }).scan(x, this);
  }

  /**
   * Print a warning message if verbose mode is enabled. A warning is used to
   * ensure that the message shows up in Eclipse's editor (a note only makes it
   * into the error console).
   */
  public void debug(Element elt, String message, Object... args) {
    if (verbose) {
      messager.printMessage(Kind.WARNING, String.format(message, args), elt);
    }
  }

  public void executeJobs() {
    while (!jobs.isEmpty()) {
      Job job = jobs.remove(0);
      debug(job.element, "Scanning");
      try {
        job.scanner.scan(job.element, this);
      } catch (HaltException ignored) {
        // Already reported
      } catch (Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        poison(job.element, sw.toString());
      }
    }
    for (TypeElement proxyElement : proxiesRequiringMapping) {
      if (!getClientToDomainMap().containsKey(proxyElement)) {
        poison(proxyElement, Messages.proxyMustBeAnnotated());
      }
    }
  }

  /**
   * Utility method to look up raw types from class literals.
   */
  public DeclaredType findType(Class<?> clazz) {
    return types.getDeclaredType(elements.getTypeElement(clazz.getCanonicalName()));
  }

  /**
   * Utility method to look up raw types from the requestfactory.shared package.
   * This method is used instead of class literals in order to minimize the
   * number of dependencies that get packed into {@code requestfactoy-apt.jar}.
   */
  public DeclaredType findType(String simpleName) {
    return types.getDeclaredType(elements
        .getTypeElement("com.google.web.bindery.requestfactory.shared." + simpleName));
  }

  /**
   * Returns a map of client proxy elements to their domain counterparts.
   */
  public Map<TypeElement, TypeElement> getClientToDomainMap() {
    return Collections.unmodifiableMap(clientToDomainMain);
  }

  public boolean isPoisoned() {
    return poisoned;
  }

  /**
   * Verifies that the given type may be used with RequestFactory.
   * 
   * @see TransportableTypeVisitor
   */
  public boolean isTransportableType(TypeMirror asType) {
    return asType.accept(new TransportableTypeVisitor(), this);
  }

  public void maybeScanContext(TypeElement requestContext) {
    // Also ignore RequestContext itself
    if (fastFail(requestContext) || types.isSameType(requestContextType, requestContext.asType())) {
      return;
    }
    jobs.add(new Job(requestContext, new RequestContextScanner()));
  }

  public void maybeScanFactory(TypeElement factoryType) {
    if (fastFail(factoryType) || types.isSameType(requestFactoryType, factoryType.asType())) {
      return;
    }
    jobs.add(new Job(factoryType, new RequestFactoryScanner()));
  }

  public void maybeScanProxy(TypeElement proxyType) {
    if (fastFail(proxyType)) {
      return;
    }
    jobs.add(new Job(proxyType, new ProxyScanner()));
  }

  /**
   * Emits a fatal error message attached to an element. If the element or an
   * eclosing type is annotated with {@link SkipInterfaceValidation} the message
   * will be dropped.
   */
  public void poison(Element elt, String message) {
    if (suppressErrors) {
      return;
    }

    if (squelchMessage(elt, message)) {
      return;
    }

    if (respectAnnotations()) {
      Element check = elt;
      while (check != null) {
        if (check.getAnnotation(SkipInterfaceValidation.class) != null) {
          return;
        }
        check = check.getEnclosingElement();
      }
    }

    if (elt == null) {
      messager.printMessage(Kind.ERROR, message);
    } else {
      messager.printMessage(Kind.ERROR, message, elt);
    }
  }

  public void requireMapping(TypeElement proxyElement) {
    proxiesRequiringMapping.add(proxyElement);
  }

  /**
   * Emits a warning message, unless the element or an enclosing element are
   * annotated with a {@code @SuppressWarnings("requestfactory")}.
   */
  public void warn(Element elt, String message) {
    if (suppressWarnings) {
      return;
    }

    if (squelchMessage(elt, message)) {
      return;
    }

    if (respectAnnotations()) {
      SuppressWarnings suppress;
      Element check = elt;
      while (check != null) {
        if (check.getAnnotation(SkipInterfaceValidation.class) != null) {
          return;
        }
        suppress = check.getAnnotation(SuppressWarnings.class);
        if (suppress != null) {
          if (Arrays.asList(suppress.value()).contains("requestfactory")) {
            return;
          }
        }
        check = check.getEnclosingElement();
      }
    }

    messager.printMessage(Kind.WARNING, message + Messages.warnSuffix(), elt);
  }

  /**
   * This switch allows the RfValidatorTest code to be worked on in the IDE
   * without causing compilation failures.
   */
  boolean respectAnnotations() {
    return true;
  }

  private boolean fastFail(TypeElement element) {
    return !seen.add(element);
  }

  /**
   * Prevents duplicate messages from being emitted.
   */
  private boolean squelchMessage(Element elt, String message) {
    Set<String> set = previousMessages.get(elt);
    if (set == null) {
      set = new HashSet<String>();
      // HashMap allows the null key
      previousMessages.put(elt, set);
    }
    return !set.add(message);
  }
}