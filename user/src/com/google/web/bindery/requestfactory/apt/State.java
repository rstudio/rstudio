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
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
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

  /**
   * Implements comparable for priority ordering.
   */
  private static class Job implements Comparable<Job> {
    private static long count;

    public final TypeElement element;
    public final ScannerBase<?> scanner;
    private final long order = count++;
    private final int priority;

    public Job(TypeElement element, ScannerBase<?> scanner, int priority) {
      this.element = element;
      this.priority = priority;
      this.scanner = scanner;
    }

    @Override
    public int compareTo(Job o) {
      int c = priority - o.priority;
      if (c != 0) {
        return c;
      }
      return Long.signum(order - o.order);
    }

    @Override
    public String toString() {
      return scanner.getClass().getSimpleName() + " " + element.getSimpleName();
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

  final TypeMirror baseProxyType;
  final Elements elements;
  final DeclaredType entityProxyIdType;
  final DeclaredType entityProxyType;
  final DeclaredType extraTypesAnnotation;
  final Filer filer;
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
  private final Map<Element, Element> clientToDomainMain;
  private final SortedSet<Job> jobs = new TreeSet<Job>();
  private final Messager messager;
  private boolean poisoned;
  private boolean requireAllMappings;
  private final boolean suppressErrors;
  private final boolean suppressWarnings;
  private final boolean verbose;
  /**
   * Prevents duplicate messages from being emitted.
   */
  private final Map<Element, Set<String>> previousMessages = new HashMap<Element, Set<String>>();

  private final Set<TypeElement> typesRequiringMapping = new LinkedHashSet<TypeElement>();
  private boolean clientOnly;

  public State(ProcessingEnvironment processingEnv) {
    clientToDomainMain = new HashMap<Element, Element>();
    elements = processingEnv.getElementUtils();
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();
    types = processingEnv.getTypeUtils();
    suppressErrors = Boolean.parseBoolean(processingEnv.getOptions().get("suppressErrors"));
    suppressWarnings = Boolean.parseBoolean(processingEnv.getOptions().get("suppressWarnings"));
    verbose = Boolean.parseBoolean(processingEnv.getOptions().get("verbose"));

    baseProxyType = findType("BaseProxy");
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
   * Add a mapping from a client method to a domain method.
   */
  public void addMapping(ExecutableElement clientMethod, ExecutableElement domainMethod) {
    if (domainMethod == null) {
      debug(clientMethod, "No domain mapping");
    } else {
      debug(clientMethod, "Found domain method %s", domainMethod.toString());
    }
    clientToDomainMain.put(clientMethod, domainMethod);
  }

  /**
   * Add a mapping from a client type to a domain type.
   */
  public void addMapping(TypeElement clientType, TypeElement domainType) {
    if (domainType == null) {
      debug(clientType, "No domain mapping");
    } else {
      debug(clientType, "Found domain type %s", domainType.toString());
    }
    clientToDomainMain.put(clientType, domainType);
  }

  /**
   * Check an element for an {@code ExtraTypes} annotation. Handles both methods
   * and types.
   */
  public void checkExtraTypes(Element x) {
    (new ExtraTypesScanner<Void>() {
      @Override
      public Void visitExecutable(ExecutableElement x, State state) {
        checkForAnnotation(x, state);
        return null;
      }

      @Override
      public Void visitType(TypeElement x, State state) {
        checkForAnnotation(x, state);
        return null;
      }

      @Override
      protected void scanExtraType(TypeElement extraType) {
        maybeScanProxy(extraType);
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
      Job job = jobs.first();
      jobs.remove(job);
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
    if (clientOnly) {
      // Don't want to check for mappings in client-only mode
      return;
    }
    for (TypeElement element : typesRequiringMapping) {
      if (!getClientToDomainMap().containsKey(element)) {
        if (types.isAssignable(element.asType(), requestContextType)) {
          poison(element, Messages.contextMustBeAnnotated(element.getSimpleName()));
        } else {
          poison(element, Messages.proxyMustBeAnnotated(element.getSimpleName()));
        }
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
   * Returns a map of client elements to their domain counterparts. The keys may
   * be RequestContext or Proxy types or methods within those types.
   */
  public Map<Element, Element> getClientToDomainMap() {
    return Collections.unmodifiableMap(clientToDomainMain);
  }

  public boolean isClientOnly() {
    return clientOnly;
  }

  public boolean isMappingRequired(TypeElement element) {
    return typesRequiringMapping.contains(element);
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
    jobs.add(new Job(requestContext, new RequestContextScanner(), 0));
    if (!clientOnly) {
      jobs.add(new Job(requestContext, new DomainChecker(), 1));
    }
  }

  public void maybeScanFactory(TypeElement factoryType) {
    if (fastFail(factoryType) || types.isSameType(requestFactoryType, factoryType.asType())) {
      return;
    }
    jobs.add(new Job(factoryType, new RequestFactoryScanner(), 0));
    jobs.add(new Job(factoryType, new DeobfuscatorBuilder(), 2));
  }

  public void maybeScanProxy(TypeElement proxyType) {
    if (fastFail(proxyType)) {
      return;
    }
    jobs.add(new Job(proxyType, new ProxyScanner(), 0));
    if (!clientOnly) {
      jobs.add(new Job(proxyType, new DomainChecker(), 1));
    }
  }

  public boolean mustResolveAllAnnotations() {
    return requireAllMappings;
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
    poisoned = true;
    if (elt == null) {
      messager.printMessage(Kind.ERROR, message);
    } else {
      messager.printMessage(Kind.ERROR, message, elt);
    }
  }

  public void requireMapping(TypeElement interfaceElement) {
    typesRequiringMapping.add(interfaceElement);
  }

  /**
   * Set to {@code true} to indicate that only JVM-client support code needs to
   * be generated.
   */
  public void setClientOnly(boolean clientOnly) {
    this.clientOnly = clientOnly;
  }

  /**
   * Set to {@code true} if it is an error for unresolved ProxyForName and
   * ServiceName annotations to be left over.
   */
  public void setMustResolveAllMappings(boolean requireAllMappings) {
    this.requireAllMappings = requireAllMappings;
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
   * Utility method to look up raw types from the requestfactory.shared package.
   * This method is used instead of class literals in order to minimize the
   * number of dependencies that get packed into {@code requestfactoy-apt.jar}.
   * If the requested type cannot be found, the State will be poisoned.
   */
  private DeclaredType findType(String simpleName) {
    TypeElement element =
        elements.getTypeElement("com.google.web.bindery.requestfactory.shared." + simpleName);
    if (element == null) {
      poison(null, "Unable to find RequestFactory built-in type. "
          + "Is requestfactory-[client|server].jar on the classpath?");
      return null;
    }
    return types.getDeclaredType(element);
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