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

import com.google.web.bindery.requestfactory.shared.JsonRpcProxy;
import com.google.web.bindery.requestfactory.shared.JsonRpcService;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.ProxyForName;
import com.google.web.bindery.requestfactory.shared.Service;
import com.google.web.bindery.requestfactory.shared.ServiceName;

/**
 * Contains string-formatting methods to produce error messages. This class
 * exists to avoid the need to duplicate error messages in test code. All method
 * parameters in this class accept {@code Object} so that the production code
 * can pass {@code javax.lang.model} types and the test code can pass Strings.
 */
class Messages {
  /*
   * Note to maintainers: When new messages are added to this class, the
   * RfValidatorTest.testErrorsAndWarnings() method should be updated to test
   * the new message.
   */

  public static String contextMissingDomainType(Object domainTypeName) {
    return String.format("Cannot fully validate context since domain type %s is not available.\n"
        + "You must run the ValidationTool as part of your server build process.", domainTypeName);
  }

  public static String contextMustBeAnnotated(Object requestContextName) {
    return String.format("The type %s must be annotated with %s, %s, or %s", requestContextName,
        Service.class.getSimpleName(), ServiceName.class.getSimpleName(), JsonRpcService.class
            .getSimpleName());
  }

  public static String contextRequiredReturnTypes(Object requestName, Object instanceRequestName) {
    return String.format("The return type must be a %s or %s", requestName, instanceRequestName);
  }

  public static String deobfuscatorMissingContext(Object contextName) {
    return String.format("Could not load domain mapping for context %s.\n"
        + "Check that both the shared interfaces and server domain types are on the classpath.",
        contextName);
  }

  public static String deobfuscatorMissingProxy(Object proxyName) {
    return String.format("Could not load domain mapping for proxy %s.\n"
        + "Check that both the shared interfaces and server domain types are on the classpath.",
        proxyName);
  }

  public static String domainFindNotStatic(Object domainTypeName) {
    return String.format("The domain object's find%s() method is not static", domainTypeName);
  }

  public static String domainGetIdStatic() {
    return "The domain type's getId() method must not be static";
  }

  public static String domainGetVersionStatic() {
    return "The domain type's getVersion() method must not be static";
  }

  public static String domainMethodNotPublic(Object domainMethodName) {
    return String.format("Domain method %s must be public", domainMethodName);
  }

  public static String domainMethodWrongModifier(boolean expectStatic, Object domainMethodName) {
    return String.format("Found %s domain method %s when %s method required", expectStatic
        ? "instance" : "static", domainMethodName, expectStatic ? "static" : "instance");
  }

  public static String domainMissingFind(Object domainType, Object simpleName,
      Object getIdReturnType, Object checkedTypeName) {
    return String.format("The domain type %s has no %s find%s(%s) method. "
        + "Attempting to send a %s to the server will result in a server error.", domainType,
        simpleName, simpleName, getIdReturnType, checkedTypeName);
  }

  public static String domainMissingMethod(Object description) {
    return String.format("Could not find domain method similar to %s", description);
  }

  public static String domainNoGetId(Object domainType) {
    return String.format("Domain type %s does not have a getId() method", domainType);
  }

  public static String domainNoGetVersion(Object domainType) {
    return String.format("Domain type %s does not have a getVersion() method", domainType);
  }

  public static String domainNotDefaultInstantiable(Object domainName, Object proxyName,
      Object requestContextName) {
    return String.format("The domain type %s is not default-instantiable."
        + " Calling %s.create(%s.class) will cause a server error.", domainName,
        requestContextName, proxyName);
  }

  public static String factoryMustBeAssignable(Object assignableTo) {
    return String.format("The return type of this method must return a %s", assignableTo);
  }

  public static String factoryMustReturnInterface(Object returnType) {
    return String.format("The return type %s must be an interface", returnType);
  }

  public static String factoryNoMethodParameters() {
    return "This method must have no parameters";
  }

  public static String methodNoDomainPeer(Object proxyTypeName, boolean isParameter) {
    return String.format("Cannot validate this method because the domain mapping for "
        + " %s type (%s) could not be resolved to a domain type", isParameter ? "a parameter of"
        : "the return", proxyTypeName);
  }

  public static String noSuchType(String binaryTypeName) {
    return String.format("Could not find root type %s", binaryTypeName);
  }

  public static String proxyMissingDomainType(Object missingDomainName) {
    return String.format("Cannot fully validate proxy since type %s is not available",
        missingDomainName);
  }

  public static String proxyMustBeAnnotated(Object typeName) {
    return String.format("The proxy type %s must be annotated with %s, %s, or %s", typeName,
        ProxyFor.class.getSimpleName(), ProxyForName.class.getSimpleName(), JsonRpcProxy.class
            .getSimpleName());
  }

  public static String proxyOnlyGettersSetters() {
    return "Only getters and setters allowed";
  }

  public static String rawType() {
    return "A raw type may not be used here";
  }

  public static String redundantAnnotation(Object annotationName) {
    return String.format("Redundant annotation: %s", annotationName);
  }

  public static String untransportableType(Object returnType) {
    return String.format("The type %s cannot be used here", returnType);
  }

  public static String warnSuffix() {
    return "\n\nAdd @SuppressWarnings(\"requestfactory\") to dismiss.";
  }

  private Messages() {
  }
}
