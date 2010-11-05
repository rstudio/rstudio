/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.requestfactory.server;

import com.google.gwt.dev.asm.AnnotationVisitor;
import com.google.gwt.dev.asm.ClassReader;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.asm.commons.EmptyVisitor;
import com.google.gwt.dev.asm.commons.Method;
import com.google.gwt.dev.asm.signature.SignatureReader;
import com.google.gwt.dev.asm.signature.SignatureVisitor;
import com.google.gwt.dev.util.Name;
import com.google.gwt.dev.util.Name.BinaryName;
import com.google.gwt.dev.util.Name.SourceOrBinaryName;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.InstanceRequest;
import com.google.gwt.requestfactory.shared.ProxyFor;
import com.google.gwt.requestfactory.shared.ProxyForName;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.Service;
import com.google.gwt.requestfactory.shared.ServiceName;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates validation logic to determine if a {@link RequestFactory}
 * interface, its {@link RequestContext}, and associated {@link EntityProxy}
 * interfaces match their domain counterparts. This implementation examines the
 * classfiles directly in order to avoid the need to load the types into the
 * JVM.
 * <p>
 * This class is amenable to being used as a unit test:
 * 
 * <pre>
 * public void testRequestFactory() {
 *   Logger logger = Logger.getLogger("");
 *   RequestFactoryInterfaceValidator v = new RequestFactoryInterfaceValidator(
 *     logger, new ClassLoaderLoader(Thread.currentThread().getContextClassLoader()));
 *   v.validateRequestContext(MyRequestContext.class.getName());
 *   assertFalse(v.isPoisoned());
 * }
 * </pre>
 * This class also has a {@code main} method and can be used as a build-time
 * tool:
 * 
 * <pre>
 * java -cp gwt-servlet.jar:your-code.jar \
 *   com.google.gwt.requestfactory.server.RequestFactoryInterfaceValidator \
 *   com.example.MyRequestFactory
 * </pre>
 */
public class RequestFactoryInterfaceValidator {
  /**
   * An implementation of {@link Loader} that uses a {@link ClassLoader} to
   * retrieve the class files.
   */
  public static class ClassLoaderLoader implements Loader {
    private final ClassLoader loader;

    public ClassLoaderLoader(ClassLoader loader) {
      this.loader = loader;
    }

    public boolean exists(String resource) {
      return loader.getResource(resource) != null;
    }

    public InputStream getResourceAsStream(String resource) {
      return loader.getResourceAsStream(resource);
    }
  }

  /**
   * Abstracts the mechanism by which class files are loaded.
   * 
   * @see ClassLoaderLoader
   */
  public interface Loader {
    /**
     * Returns true if the specified resource can be loaded.
     * 
     * @param resource a resource name (e.g. <code>com/example/Foo.class</code>)
     */
    boolean exists(String resource);

    /**
     * Returns an InputStream to access the specified resource, or
     * <code>null</code> if no such resource exists.
     * 
     * @param resource a resource name (e.g. <code>com/example/Foo.class</code>)
     */
    InputStream getResourceAsStream(String resource);
  }

  /**
   * Used internally as a placeholder for types that cannot be mapped to a
   * domain object.
   */
  interface MissingDomainType {
  }

  /**
   * Collects the ProxyFor or Service annotation from an EntityProxy or
   * RequestContext type.
   */
  private class DomainMapper extends EmptyVisitor {
    private final ErrorContext logger;
    private String domainInternalName;

    public DomainMapper(ErrorContext logger) {
      this.logger = logger;
      logger.spam("Finding domain mapping annotation");
    }

    public String getDomainInternalName() {
      return domainInternalName;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
        String superName, String[] interfaces) {
      if ((access & Opcodes.ACC_INTERFACE) == 0) {
        logger.poison("Type must be an interface");
      }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      boolean foundProxy = desc.equals(Type.getDescriptor(ProxyFor.class));
      boolean foundProxyName = desc.equals(Type.getDescriptor(ProxyForName.class));
      boolean foundService = desc.equals(Type.getDescriptor(Service.class));
      boolean foundServiceName = desc.equals(Type.getDescriptor(ServiceName.class));

      if (foundProxy || foundService) {
        return new EmptyVisitor() {

          @Override
          public void visit(String name, Object value) {
            domainInternalName = ((Type) value).getInternalName();
          }

        };
      }

      if (foundProxyName || foundServiceName) {
        return new EmptyVisitor() {
          @Override
          public void visit(String name, Object value) {
            String sourceName = (String) value;

            /*
             * The input is a source name, so we need to convert it to an
             * internal name. We'll do this by substituting dollar signs for the
             * last slash in the name until there are no more slashes.
             */
            StringBuffer desc = new StringBuffer(sourceName.replace('.', '/'));
            while (!loader.exists(desc.toString() + ".class")) {
              logger.spam("Did not find " + desc.toString());
              int idx = desc.lastIndexOf("/");
              if (idx == -1) {
                return;
              }
              desc.setCharAt(idx, '$');
            }

            domainInternalName = desc.toString();
            logger.spam(domainInternalName);
          }
        };
      }
      return null;
    }
  }

  /**
   * Improves error messages by providing context for the user.
   */
  private class ErrorContext {
    private final Logger logger;
    private final ErrorContext parent;
    private Type currentType;
    private Method currentMethod;

    public ErrorContext(Logger logger) {
      this.logger = logger;
      this.parent = null;
    }

    private ErrorContext(ErrorContext parent) {
      this.logger = parent.logger;
      this.parent = parent;
    }

    public void poison(String msg, Object... args) {
      poison();
      logger.logp(Level.SEVERE, currentType(), currentMethod(),
          String.format(msg, args));
      poisoned = true;
    }

    public void poison(String msg, Throwable t) {
      poison();
      logger.logp(Level.SEVERE, currentType(), currentMethod(), msg, t);
      poisoned = true;
    }

    public ErrorContext setMethod(Method method) {
      ErrorContext toReturn = new ErrorContext(this);
      toReturn.currentMethod = method;
      return toReturn;
    }

    public ErrorContext setType(Type type) {
      ErrorContext toReturn = new ErrorContext(this);
      toReturn.currentType = type;
      return toReturn;
    }

    public void spam(String msg, Object... args) {
      logger.logp(Level.FINEST, currentType(), currentMethod(),
          String.format(msg, args));
    }

    private String currentMethod() {
      if (currentMethod != null) {
        return print(currentMethod);
      }
      if (parent != null) {
        return parent.currentMethod();
      }
      return null;
    }

    private String currentType() {
      if (currentType != null) {
        return print(currentType);
      }
      if (parent != null) {
        return parent.currentType();
      }
      return null;
    }

    /**
     * Populate {@link RequestFactoryInterfaceValidator#badTypes} with the
     * current context.
     */
    private void poison() {
      if (currentType != null) {
        badTypes.add(currentType.getClassName());
      }
      if (parent != null) {
        parent.poison();
      }
    }
  }

  /**
   * Collects information about domain objects. This visitor is intended to be
   * iteratively applied to collect all methods in a type hierarchy.
   */
  private class MethodsInHierarchyCollector extends EmptyVisitor {
    private final ErrorContext logger;
    private Set<RFMethod> methods = new LinkedHashSet<RFMethod>();
    private Set<String> seen = new HashSet<String>();

    private MethodsInHierarchyCollector(ErrorContext logger) {
      this.logger = logger;
    }

    public Set<RFMethod> exec(String internalName) {
      RequestFactoryInterfaceValidator.this.visit(logger, internalName, this);
      return methods;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
        String superName, String[] interfaces) {
      if (!seen.add(name)) {
        return;
      }
      if (!"java/lang/Object".equals(superName)) {
        RequestFactoryInterfaceValidator.this.visit(logger, superName, this);
      }
      if (interfaces != null) {
        for (String intf : interfaces) {
          RequestFactoryInterfaceValidator.this.visit(logger, intf, this);
        }
      }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
        String signature, String[] exceptions) {
      // Ignore initializers
      if ("<clinit>".equals(name) || "<init>".equals(name)) {
        return null;
      }
      RFMethod method = new RFMethod(name, desc);
      method.setDeclaredStatic((access & Opcodes.ACC_STATIC) != 0);
      method.setDeclaredSignature(signature);
      methods.add(method);
      return null;
    }
  }

  private static class RFMethod extends Method {
    private boolean isDeclaredStatic;
    private String signature;

    public RFMethod(String name, String desc) {
      super(name, desc);
    }

    public String getSignature() {
      return signature;
    }

    public boolean isDeclaredStatic() {
      return isDeclaredStatic;
    }

    public void setDeclaredSignature(String signature) {
      this.signature = signature;
    }

    public void setDeclaredStatic(boolean value) {
      isDeclaredStatic = value;
    }

    @Override
    public String toString() {
      return (isDeclaredStatic ? "static " : "") + super.toString();
    }
  }

  private class SupertypeCollector extends EmptyVisitor {
    private final ErrorContext logger;
    private final Set<String> seen = new HashSet<String>();
    private final List<Type> supertypes = new ArrayList<Type>();

    public SupertypeCollector(ErrorContext logger) {
      this.logger = logger;
    }

    public List<Type> exec(Type type) {
      RequestFactoryInterfaceValidator.this.visit(logger,
          type.getInternalName(), this);
      return supertypes;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
        String superName, String[] interfaces) {
      if (!seen.add(name)) {
        return;
      }
      supertypes.add(Type.getObjectType(name));
      if (!"java/lang/Object".equals(name)) {
        RequestFactoryInterfaceValidator.this.visit(logger, superName, this);
      }
      if (interfaces != null) {
        for (String intf : interfaces) {
          RequestFactoryInterfaceValidator.this.visit(logger, intf, this);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  static final Set<Class<?>> VALUE_TYPES = Collections.unmodifiableSet(new HashSet<Class<?>>(
      Arrays.asList(Boolean.class, Character.class, Class.class, Date.class,
          Enum.class, Number.class, String.class, Void.class)));

  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("Usage: java -cp gwt-servlet.jar:your-code.jar "
          + RequestFactoryInterfaceValidator.class.getCanonicalName()
          + " com.example.MyRequestFactory");
      System.exit(1);
    }
    RequestFactoryInterfaceValidator validator = new RequestFactoryInterfaceValidator(
        Logger.getLogger(RequestFactoryInterfaceValidator.class.getName()),
        new ClassLoaderLoader(Thread.currentThread().getContextClassLoader()));
    validator.validateRequestFactory(args[0]);
    System.exit(validator.isPoisoned() ? 1 : 0);
  }

  private static String print(Method method) {
    StringBuilder sb = new StringBuilder();
    sb.append(print(method.getReturnType())).append(" ").append(
        method.getName()).append("(");
    for (Type t : method.getArgumentTypes()) {
      sb.append(print(t)).append(" ");
    }
    sb.append(")");
    return sb.toString();
  }

  private static String print(Type type) {
    return SourceOrBinaryName.toSourceName(type.getClassName());
  }

  /**
   * A set of binary type names that are known to be bad.
   */
  private final Set<String> badTypes = new HashSet<String>();

  /**
   * Maps client types (e.g. FooProxy) to server domain types (e.g. Foo).
   */
  private final Map<Type, Type> clientToDomainType = new HashMap<Type, Type>();
  private final Map<Type, Type> domainToClientType = new HashMap<Type, Type>();
  /**
   * The type {@link EntityProxy}.
   */
  private final Type entityProxyIntf = Type.getType(EntityProxy.class);
  /**
   * A placeholder type for client types that could not be resolved to a domain
   * type.
   */
  private final Type errorType = Type.getType(MissingDomainType.class);
  /**
   * The type {@link InstanceRequest}.
   */
  private final Type instanceRequestIntf = Type.getType(InstanceRequest.class);
  private final Loader loader;
  /**
   * A cache of all methods defined in a type hierarchy.
   */
  private final Map<Type, Set<RFMethod>> methodsInHierarchy = new HashMap<Type, Set<RFMethod>>();
  /**
   * The type {@link Object}.
   */
  private final Type objectType = Type.getObjectType("java/lang/Object");
  private final ErrorContext parentLogger;
  private boolean poisoned;
  /**
   * The type {@link Request}.
   */
  private final Type requestIntf = Type.getType(Request.class);
  /**
   * The type {@link RequestContext}.
   */
  private final Type requestContextIntf = Type.getType(RequestContext.class);
  /**
   * A map of a type to all types that it could be assigned to.
   */
  private final Map<Type, List<Type>> supertypes = new HashMap<Type, List<Type>>();
  /**
   * A set to prevent re-validation of a type.
   */
  private final Set<String> validatedTypes = new HashSet<String>();

  /**
   * Contains vaue types (e.g. Integer).
   */
  private final Set<Type> valueTypes = new HashSet<Type>();

  public RequestFactoryInterfaceValidator(Logger logger, Loader loader) {
    this.parentLogger = new ErrorContext(logger);
    this.loader = loader;
    for (Class<?> clazz : VALUE_TYPES) {
      valueTypes.add(Type.getType(clazz));
    }
  }

  /**
   * Reset the poisoned status of the validator so that it may be reused without
   * destroying cached state.
   */
  public void antidote() {
    poisoned = false;
  }

  /**
   * Returns true if validation failed.
   */
  public boolean isPoisoned() {
    return poisoned;
  }

  /**
   * This method checks an EntityProxy interface against its peer domain object
   * to determine if the server code would be able to process a request using
   * the methods defined in the EntityProxy interface. It does not perform any
   * checks as to whether or not the EntityProxy could actually be generated by
   * the Generator.
   * <p>
   * This method may be called repeatedly on a single instance of the validator.
   * Doing so will amortize type calculation costs.
   * <p>
   * Checks implemented:
   * <ul>
   * <li> <code>binaryName</code> implements EntityProxy</li>
   * <li><code>binaryName</code> has a {@link ProxyFor} or {@link ProxyForName}
   * annotation</li>
   * <li>The domain object has getId() and getVersion() methods</li>
   * <li>All property methods in the EntityProxy can be mapped onto an
   * equivalent domain method</li>
   * <li>All referenced EntityProxy types are valid</li>
   * </ul>
   * 
   * @param binaryName the binary name (e.g. {@link Class#getName()}) of the
   *          EntityProxy subtype
   */
  public void validateEntityProxy(String binaryName) {
    if (fastFail(binaryName)) {
      return;
    }

    Type proxyType = Type.getObjectType(BinaryName.toInternalName(binaryName));
    ErrorContext logger = parentLogger.setType(proxyType);

    // Quick sanity check for calling code
    if (!isAssignable(logger, entityProxyIntf, proxyType)) {
      parentLogger.poison("%s is not a %s", print(proxyType),
          EntityProxy.class.getSimpleName());
      return;
    }

    // Find the domain type
    Type domainType = getDomainType(logger, proxyType);
    if (domainType == errorType) {
      logger.poison(
          "The type %s must be annotated with a @%s or @%s annotation",
          BinaryName.toSourceName(binaryName), ProxyFor.class.getSimpleName(),
          ProxyForName.class.getSimpleName());
      return;
    }

    // Check for getId() and getVersion() in domain
    checkIdAndVersion(logger, domainType);

    // Collect all methods in the client proxy type
    Set<RFMethod> clientPropertyMethods = getMethodsInHierarchy(logger,
        proxyType);

    // Find the equivalent domain getter/setter method
    for (RFMethod clientPropertyMethod : clientPropertyMethods) {
      // Ignore stableId(). Can't use descriptor due to overrides
      if ("stableId".equals(clientPropertyMethod.getName())
          && clientPropertyMethod.getArgumentTypes().length == 0) {
        continue;
      }
      checkPropertyMethod(logger, clientPropertyMethod, domainType);
      maybeCheckReferredProxies(logger, clientPropertyMethod);
    }
  }

  /**
   * This method checks a RequestContext interface against its peer domain
   * domain object to determine if the server code would be able to process a
   * request using the the methods defined in the RequestContext interface. It
   * does not perform any checks as to whether or not the RequestContext could
   * actually be generated by the Generator.
   * <p>
   * This method may be called repeatedly on a single instance of the validator.
   * Doing so will amortize type calculation costs.
   * <p>
   * Checks implemented:
   * <ul>
   * <li> <code>binaryName</code> implements RequestContext</li>
   * <li><code>binaryName</code> has a {@link Service} or {@link ServiceName}
   * annotation</li>
   * <li>All service methods in the RequestContext can be mapped onto an
   * equivalent domain method</li>
   * <li>All referenced EntityProxy types are valid</li>
   * </ul>
   * 
   * @param binaryName the binary name (e.g. {@link Class#getName()}) of the
   *          RequestContext subtype
   * @see #validateEntityProxy(String)
   */
  public void validateRequestContext(String binaryName) {
    if (fastFail(binaryName)) {
      return;
    }

    Type requestContextType = Type.getObjectType(BinaryName.toInternalName(binaryName));
    final ErrorContext logger = parentLogger.setType(requestContextType);

    // Quick sanity check for calling code
    if (!isAssignable(logger, requestContextIntf, requestContextType)) {
      logger.poison("%s is not a %s", print(requestContextType),
          RequestContext.class.getSimpleName());
      return;
    }

    Type domainServiceType = getDomainType(logger, requestContextType);
    if (domainServiceType == errorType) {
      logger.poison(
          "The type %s must be annotated with a @%s or @%s annotation",
          BinaryName.toSourceName(binaryName), Service.class.getSimpleName(),
          ServiceName.class.getSimpleName());
      return;
    }

    for (RFMethod method : getMethodsInHierarchy(logger, requestContextType)) {
      // Ignore methods in RequestContext itself
      if (findCompatibleMethod(logger, requestContextIntf, method, false, true) != null) {
        continue;
      }

      // Check the client method against the domain
      checkClientMethodInDomain(logger, method, domainServiceType);
      maybeCheckReferredProxies(logger, method);
    }
  }

  /**
   * This method checks a RequestFactory interface.
   * <p>
   * This method may be called repeatedly on a single instance of the validator.
   * Doing so will amortize type calculation costs. It does not perform any
   * checks as to whether or not the RequestContext could actually be generated
   * by the Generator.
   * <p>
   * Checks implemented:
   * <ul>
   * <li> <code>binaryName</code> implements RequestFactory</li>
   * <li>All referenced RequestContext types are valid</li>
   * </ul>
   * 
   * @param binaryName the binary name (e.g. {@link Class#getName()}) of the
   *          RequestContext subtype
   * @see #validateRequestContext(String)
   */
  public void validateRequestFactory(String binaryName) {
    if (fastFail(binaryName)) {
      return;
    }

    Type requestFactoryType = Type.getObjectType(BinaryName.toInternalName(binaryName));
    ErrorContext logger = parentLogger.setType(requestFactoryType);

    // Quick sanity check for calling code
    if (!isAssignable(logger, Type.getType(RequestFactory.class),
        requestFactoryType)) {
      logger.poison("%s is not a %s", print(requestFactoryType),
          RequestFactory.class.getSimpleName());
      return;
    }

    // Validate each RequestContext method in the RF
    for (Method contextMethod : getMethodsInHierarchy(logger,
        requestFactoryType)) {
      Type returnType = contextMethod.getReturnType();
      if (isAssignable(logger, requestContextIntf, returnType)) {
        validateRequestContext(returnType.getClassName());
      }
    }
  }

  /**
   * Given the binary name of a domain type, return the EntityProxy type that
   * has been seen to map to the domain type.
   */
  String getEntityProxyTypeName(String domainTypeNameBinaryName) {
    Type key = Type.getObjectType(BinaryName.toInternalName(domainTypeNameBinaryName));
    Type found = domainToClientType.get(key);
    return found == null ? null : found.getClassName();
  }

  /**
   * Check that a given method RequestContext method declaration can be mapped
   * to the server's domain type.
   */
  private void checkClientMethodInDomain(ErrorContext logger, RFMethod method,
      Type domainServiceType) {
    logger = logger.setMethod(method);

    // Create a "translated" method declaration to search for
    // Request<BlahProxy> foo(int a, BarProxy bar) -> Blah foo(int a, Bar bar);
    Type returnType = getReturnType(logger, method);
    Method searchFor = createDomainMethod(logger, new Method(method.getName(),
        returnType, method.getArgumentTypes()));

    RFMethod found = findCompatibleMethod(logger, domainServiceType, searchFor);

    if (found != null) {
      boolean isInstance = isAssignable(logger, instanceRequestIntf,
          method.getReturnType());
      if (isInstance && found.isDeclaredStatic()) {
        logger.poison("The method %s is declared to return %s, but the"
            + " service method is static", method.getName(),
            InstanceRequest.class.getCanonicalName());
      } else if (!isInstance && !found.isDeclaredStatic) {
        logger.poison("The method %s is declared to return %s, but the"
            + " service method is not static", method.getName(),
            Request.class.getCanonicalName());
      }
    }
  }

  /**
   * Check that the domain object has <code>getId()</code> and
   * <code>getVersion</code> methods.
   */
  private void checkIdAndVersion(ErrorContext logger, Type domainType) {
    if (objectType.equals(domainType)) {
      return;
    }
    logger = logger.setType(domainType);
    Method getIdString = new Method("getId", "()Ljava/lang/String;");
    Method getIdLong = new Method("getId", "()Ljava/lang/Long;");
    if (findCompatibleMethod(logger, domainType, getIdString, false, true) == null
        && findCompatibleMethod(logger, domainType, getIdLong, false, true) == null) {
      logger.poison("Did not find a getId() method that"
          + " returns a String or a Long");
    }

    Method getVersion = new Method("getVersion", "()Ljava/lang/Integer;");
    if (findCompatibleMethod(logger, domainType, getVersion) == null) {
      logger.poison("Did not find a getVersion() method"
          + " that returns an Integer");
    }
  }

  /**
   * Ensure that the given property method on an EntityProxy exists on the
   * domain object.
   */
  private void checkPropertyMethod(ErrorContext logger,
      Method clientPropertyMethod, Type domainType) {
    logger = logger.setMethod(clientPropertyMethod);

    findCompatibleMethod(logger, domainType,
        createDomainMethod(logger, clientPropertyMethod));
  }

  /**
   * Convert a method declaration using client types (e.g. FooProxy) to domain
   * types (e.g. Foo).
   */
  private Method createDomainMethod(ErrorContext logger, Method clientMethod) {
    Type[] args = clientMethod.getArgumentTypes();
    for (int i = 0, j = args.length; i < j; i++) {
      args[i] = getDomainType(logger, args[i]);
    }
    Type returnType = getDomainType(logger, clientMethod.getReturnType());
    return new Method(clientMethod.getName(), returnType, args);
  }

  /**
   * Common checks to quickly determine if a type needs to be checked.
   */
  private boolean fastFail(String binaryName) {
    if (!Name.isBinaryName(binaryName)) {
      parentLogger.poison("%s is not a binary name", binaryName);
      return true;
    }

    // Allow the poisoned flag to be reset without losing data
    if (badTypes.contains(binaryName)) {
      parentLogger.poison("Type type %s was previously marked as bad",
          binaryName);
      return true;
    }

    // Don't revalidate the same type
    if (!validatedTypes.add(binaryName)) {
      return true;
    }
    return false;
  }

  /**
   * Finds a compatible method declaration in <code>domainType</code>'s
   * hierarchy that is assignment-compatible with the given Method.
   */
  private RFMethod findCompatibleMethod(final ErrorContext logger,
      Type domainType, Method searchFor) {
    return findCompatibleMethod(logger, domainType, searchFor, true, false);
  }

  /**
   * Finds a compatible method declaration in <code>domainType</code>'s
   * hierarchy that is assignment-compatible with the given Method.
   */
  private RFMethod findCompatibleMethod(final ErrorContext logger,
      Type domainType, Method searchFor, boolean mustFind,
      boolean allowOverloads) {
    String methodName = searchFor.getName();
    Type[] clientArgs = searchFor.getArgumentTypes();
    Type clientReturnType = searchFor.getReturnType();
    // Pull all methods out of the domain type
    Map<String, List<RFMethod>> domainLookup = new LinkedHashMap<String, List<RFMethod>>();
    for (RFMethod method : getMethodsInHierarchy(logger, domainType)) {
      List<RFMethod> list = domainLookup.get(method.getName());
      if (list == null) {
        list = new ArrayList<RFMethod>();
        domainLookup.put(method.getName(), list);
      }
      list.add(method);
    }

    // Find the matching method in the domain object
    List<RFMethod> methods = domainLookup.get(methodName);
    if (methods == null) {
      if (mustFind) {
        logger.poison("Could not find any methods named %s in %s", methodName,
            print(domainType));
      }
      return null;
    }
    if (methods.size() > 1 && !allowOverloads) {
      StringBuilder sb = new StringBuilder();
      sb.append(String.format("Method overloads found in type %s named %s:\n",
          print(domainType), methodName));
      for (RFMethod method : methods) {
        sb.append("  ").append(print(method)).append("\n");
      }
      logger.poison(sb.toString());
      return null;
    }

    // Check each overloaded name
    for (RFMethod domainMethod : methods) {
      // Box any primitive types to simplify compotibility check
      Type[] domainArgs = domainMethod.getArgumentTypes();
      for (int i = 0, j = domainArgs.length; i < j; i++) {
        domainArgs[i] = maybeBoxType(domainArgs[i]);
      }
      Type domainReturnType = maybeBoxType(domainMethod.getReturnType());

      /*
       * Make sure the client args can be passed into the domain args and the
       * domain return type into the client return type.
       */
      if (isAssignable(logger, domainArgs, clientArgs)
          && isAssignable(logger, clientReturnType, domainReturnType)) {

        logger.spam("Mapped client method " + print(searchFor) + " to "
            + print(domainMethod));
        return domainMethod;
      }
    }
    if (mustFind) {
      StringBuilder sb = new StringBuilder();
      sb.append(String.format(
          "Could not find matching method in %s:\nPossible matches:\n",
          print(domainType)));
      for (RFMethod domainMethod : methods) {
        sb.append("  ").append(print(domainMethod)).append("\n");
      }
      logger.poison(sb.toString());
    }
    return null;
  }

  /**
   * This looks like it should be a utility method somewhere else, but I can't
   * find it.
   */
  private Type getBoxedType(Type primitive) {
    switch (primitive.getSort()) {
      case Type.BOOLEAN:
        return Type.getType(Boolean.class);
      case Type.BYTE:
        return Type.getType(Byte.class);
      case Type.CHAR:
        return Type.getType(Character.class);
      case Type.DOUBLE:
        return Type.getType(Double.class);
      case Type.FLOAT:
        return Type.getType(Float.class);
      case Type.INT:
        return Type.getType(Integer.class);
      case Type.LONG:
        return Type.getType(Long.class);
      case Type.SHORT:
        return Type.getType(Short.class);
      case Type.VOID:
        return Type.getType(Void.class);
    }
    throw new RuntimeException(primitive.getDescriptor()
        + " is not a primitive type");
  }

  /**
   * Convert the type used in a client-side EntityProxy or RequestContext
   * declaration to the equivalent domain type. Value types and supported
   * collections are a pass-through. EntityProxy types will be resolved to their
   * domain object type. RequestContext types will be resolved to their service
   * object.
   */
  private Type getDomainType(ErrorContext logger, Type clientType) {
    Type toReturn = clientToDomainType.get(clientType);
    if (toReturn != null) {
      return toReturn;
    }
    if (isValueType(logger, clientType) || isCollectionType(logger, clientType)) {
      toReturn = clientType;
    } else if (entityProxyIntf.equals(clientType)) {
      toReturn = objectType;
    } else {
      logger = logger.setType(clientType);
      DomainMapper pv = new DomainMapper(logger);
      visit(logger, clientType.getInternalName(), pv);
      if (pv.getDomainInternalName() == null) {
        logger.poison("%s has no mapping to a domain type (e.g. @%s or @%s)",
            print(clientType), ProxyFor.class.getSimpleName(),
            Service.class.getSimpleName());
        toReturn = errorType;
      } else {
        toReturn = Type.getObjectType(pv.getDomainInternalName());
      }
    }
    clientToDomainType.put(clientType, toReturn);
    if (isAssignable(logger, entityProxyIntf, clientType)) {
      Type previousProxyType = domainToClientType.put(toReturn, clientType);
      if (previousProxyType != null) {
        logger.poison(
            "The domain type %s has more than one proxy type: %s and %s",
            toReturn.getClassName(), previousProxyType.getClassName(),
            clientType.getClassName());
      }
    }
    return toReturn;
  }

  /**
   * Collect all of the methods defined within a type hierarchy.
   */
  private Set<RFMethod> getMethodsInHierarchy(ErrorContext logger,
      Type domainType) {
    Set<RFMethod> toReturn = methodsInHierarchy.get(domainType);
    if (toReturn == null) {
      logger = logger.setType(domainType);
      toReturn = new MethodsInHierarchyCollector(logger).exec(domainType.getInternalName());
      methodsInHierarchy.put(domainType, Collections.unmodifiableSet(toReturn));
    }
    return toReturn;
  }

  /**
   * Examines a generic RequestContext method declaration and determines the
   * expected domain return type. This implementation is limited in that it will
   * not attempt to resolve type bounds since that would essentially require
   * implementing TypeOracle. In the case where the type bound cannot be
   * resolved, this method will return Object's type.
   */
  private Type getReturnType(ErrorContext logger, RFMethod method) {
    logger = logger.setMethod(method);
    final String[] returnType = {"java/lang/Object"};
    String signature = method.getSignature();

    final int expectedCount;
    if (method.getReturnType().equals(instanceRequestIntf)) {
      expectedCount = 2;
    } else if (method.getReturnType().equals(requestIntf)) {
      expectedCount = 1;
    } else {
      logger.spam("Punting on " + signature);
      return Type.getObjectType(returnType[0]);
    }

    // TODO(bobv): If a class-based TypeOracle is built, use that instead
    new SignatureReader(signature).accept(new SignatureAdapter() {
      @Override
      public SignatureVisitor visitReturnType() {
        return new SignatureAdapter() {
          int count;

          @Override
          public SignatureVisitor visitTypeArgument(char wildcard) {
            if (++count == expectedCount) {
              return new SignatureAdapter() {
                @Override
                public void visitClassType(String name) {
                  returnType[0] = name;
                }
              };
            }
            return super.visitTypeArgument(wildcard);
          }
        };
      }
    });

    logger.spam("Extracted " + returnType[0]);
    return Type.getObjectType(returnType[0]);
  }

  private List<Type> getSupertypes(ErrorContext logger, Type type) {
    if (type.getSort() != Type.OBJECT) {
      return Collections.emptyList();
    }
    List<Type> toReturn = supertypes.get(type);
    if (toReturn != null) {
      return toReturn;
    }
    logger = logger.setType(type);
    toReturn = new SupertypeCollector(logger).exec(type);
    supertypes.put(type, Collections.unmodifiableList(toReturn));
    return toReturn;
  }

  private boolean isAssignable(ErrorContext logger, Type possibleSupertype,
      Type possibleSubtype) {
    // Fast-path for same type
    if (possibleSupertype.equals(possibleSubtype)) {
      return true;
    }

    // Box primitive types to make this simple
    if (possibleSupertype.getSort() != Type.OBJECT) {
      possibleSupertype = getBoxedType(possibleSupertype);
    }
    if (possibleSubtype.getSort() != Type.OBJECT) {
      possibleSubtype = getBoxedType(possibleSubtype);
    }

    // Supertype calculation is cached
    List<Type> allSupertypes = getSupertypes(logger, possibleSubtype);
    return allSupertypes.contains(possibleSupertype);
  }

  private boolean isAssignable(ErrorContext logger, Type[] possibleSupertypes,
      Type[] possibleSubtypes) {
    // Check the same number of types
    if (possibleSupertypes.length != possibleSubtypes.length) {
      return false;
    }
    for (int i = 0, j = possibleSupertypes.length; i < j; i++) {
      if (!isAssignable(logger, possibleSupertypes[i], possibleSubtypes[i])) {
        return false;
      }
    }
    return true;
  }

  private boolean isCollectionType(ErrorContext logger, Type type) {
    return "java/util/List".equals(type.getInternalName())
        || "java/util/Set".equals(type.getInternalName());
  }

  private boolean isValueType(ErrorContext logger, Type type) {
    if (type.getSort() != Type.OBJECT) {
      return true;
    }
    if (valueTypes.contains(type)) {
      return true;
    }
    logger = logger.setType(type);
    List<Type> types = getSupertypes(logger, type);
    for (Type t : types) {
      if (valueTypes.contains(t)) {
        valueTypes.add(type);
        return true;
      }
    }
    return false;
  }

  private Type maybeBoxType(Type maybePrimitive) {
    if (maybePrimitive.getSort() == Type.OBJECT) {
      return maybePrimitive;
    }
    return getBoxedType(maybePrimitive);
  }

  /**
   * Examine an array of Types and call {@link #validateEntityProxy(String)} if
   * the type is an EntityProxy.
   */
  private void maybeCheckEntityProxyType(ErrorContext logger, Type... types) {
    for (Type type : types) {
      if (isAssignable(logger, entityProxyIntf, type)) {
        validateEntityProxy(type.getClassName());
      }
    }
  }

  /**
   * Examine the arguments ond return value of a method and check any
   * EntityProxies referred.
   */
  private void maybeCheckReferredProxies(ErrorContext logger, RFMethod method) {
    Type[] argTypes = method.getArgumentTypes();
    Type returnType = getReturnType(logger, method);

    // Check EntityProxy args ond return types against the domain
    maybeCheckEntityProxyType(logger, argTypes);
    maybeCheckEntityProxyType(logger, returnType);
  }

  /**
   * Load the classfile for the given binary name and apply the provided
   * visitor.
   * 
   * @return <code>true</code> if the visitor was successfully visited
   */
  private boolean visit(ErrorContext logger, String internalName,
      ClassVisitor visitor) {
    assert Name.isInternalName(internalName) : "internalName";
    logger.spam("Visiting " + internalName);
    InputStream inputStream = null;
    try {
      inputStream = loader.getResourceAsStream(internalName + ".class");
      if (inputStream == null) {
        logger.poison("Could not find class file for " + internalName);
        return false;
      }
      ClassReader reader = new ClassReader(inputStream);
      reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG
          | ClassReader.SKIP_FRAMES);
      return true;
    } catch (IOException e) {
      logger.poison("Unable to open " + internalName, e);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException ignored) {
        }
      }
    }
    return false;
  }
}
