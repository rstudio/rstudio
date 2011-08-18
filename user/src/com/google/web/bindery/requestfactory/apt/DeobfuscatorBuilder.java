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

import com.google.web.bindery.requestfactory.vm.impl.Deobfuscator;
import com.google.web.bindery.requestfactory.vm.impl.OperationData;
import com.google.web.bindery.requestfactory.vm.impl.OperationKey;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

/**
 * Visits a RequestFactory to create its associated DeobfuscatorBuilder, a
 * self-configuring subtype of
 * {@link com.google.web.bindery.requestfactory.vm.impl.Deobfuscator.Builder}
 * which provides the ServiceLayer with type- and method-mapping information.
 */
class DeobfuscatorBuilder extends ScannerBase<Void> {
  private TypeElement requestFactoryElement;
  private final StringBuilder sb = new StringBuilder();

  public String toString() {
    return sb.toString();
  }

  /**
   * Examine a method defined within a RequestFactory.
   */
  @Override
  public Void visitExecutable(ExecutableElement x, State state) {
    if (shouldIgnore(x, state)) {
      return null;
    }
    final TypeElement requestContextElement =
        (TypeElement) state.types.asElement(x.getReturnType());
    new ScannerBase<Void>() {

      /**
       * Scan a method within a RequestContext.
       */
      @Override
      public Void visitExecutable(ExecutableElement x, State state) {
        if (shouldIgnore(x, state)) {
          return null;
        }
        String requestContextBinaryName =
            state.elements.getBinaryName(requestContextElement).toString();
        String clientMethodDescriptor = x.asType().accept(new DescriptorBuilder(), state);
        ExecutableElement domainElement = (ExecutableElement) state.getClientToDomainMap().get(x);
        if (domainElement == null) {
          /*
           * No mapping from the client to domain type, probably because of an
           * unresolved ServiceName annotation. This can be fixed when building
           * the server by running ValidationTool.
           */
          if (state.mustResolveAllAnnotations()) {
            state.poison(requestContextElement, Messages
                .deobfuscatorMissingContext(requestContextElement.getSimpleName()));
          }
          return super.visitExecutable(x, state);
        }
        String domainMethodDescriptor =
            domainElement.asType().accept(new DescriptorBuilder(), state);
        String methodName = x.getSimpleName().toString();

        OperationKey key =
            new OperationKey(requestContextBinaryName, methodName, clientMethodDescriptor);
        println("withOperation(new OperationKey(\"%s\"),", key.get());
        println("  new OperationData.Builder()");
        println("  .withClientMethodDescriptor(\"%s\")", clientMethodDescriptor);
        println("  .withDomainMethodDescriptor(\"%s\")", domainMethodDescriptor);
        println("  .withMethodName(\"%s\")", methodName);
        println("  .withRequestContext(\"%s\")", requestContextBinaryName);
        println("  .build());");
        return super.visitExecutable(x, state);
      }

      /**
       * Scan a RequestContext.
       */
      @Override
      public Void visitType(TypeElement x, State state) {
        scanAllInheritedMethods(x, state);
        return null;
      }
    }.scan(requestContextElement, state);
    return null;
  }

  /**
   * Scan a RequestFactory type.
   */
  @Override
  public Void visitType(TypeElement x, State state) {
    requestFactoryElement = x;
    String simpleName = computeSimpleName(x, state);
    String packageName = state.elements.getPackageOf(x).getQualifiedName().toString();

    println("// Automatically Generated -- DO NOT EDIT");
    println("// %s", state.elements.getBinaryName(x));
    println("package %s;", packageName);
    println("import %s;", Arrays.class.getCanonicalName());
    println("import %s;", OperationData.class.getCanonicalName());
    println("import %s;", OperationKey.class.getCanonicalName());
    println("public final class %s extends %s {", simpleName, Deobfuscator.Builder.class
        .getCanonicalName());
    println("{");
    scanAllInheritedMethods(x, state);
    writeTypeAndTokenMap(state);
    println("}}");

    // Don't write the deobfuscator if something has gone wrong.
    if (state.isPoisoned()) {
      return null;
    }

    try {
      JavaFileObject obj = state.filer.createSourceFile(packageName + "." + simpleName, x);
      Writer w = obj.openWriter();
      w.write(sb.toString());
      w.close();
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      state.poison(x, sw.toString());
    }
    return null;
  }

  private String computeSimpleName(TypeElement x, State state) {
    // See constants in Deobfuscator
    String simpleName = state.elements.getBinaryName(x).toString() + "DeobfuscatorBuilder";
    if (state.isClientOnly()) {
      simpleName += "Lite";
    }
    simpleName = simpleName.substring(simpleName.lastIndexOf('.') + 1);
    return simpleName;
  }

  private void println(String line, Object... args) {
    sb.append(String.format(line, args)).append("\n");
  }

  /**
   * Write calls to {@code withRawTypeToken} and
   * {@code withClientToDomainMappings}.
   */
  private void writeTypeAndTokenMap(State state) {
    /*
     * A map and its comparator to build up the mapping between domain types and
     * the client type(s) that it is mapped to.
     */
    TypeComparator comparator = new TypeComparator(state);
    Map<TypeElement, SortedSet<TypeElement>> domainToClientMappings =
        new TreeMap<TypeElement, SortedSet<TypeElement>>(comparator);
    // Map accumulated by previous visitors
    Map<Element, Element> clientToDomainMap = state.getClientToDomainMap();
    // Get all types used by the RequestFactory
    Set<TypeElement> referredTypes = ReferredTypesCollector.collect(requestFactoryElement, state);

    for (TypeElement clientType : referredTypes) {
      // Ignare non-proxy types
      if (!state.types.isAssignable(clientType.asType(), state.baseProxyType)) {
        continue;
      }
      String binaryName = state.elements.getBinaryName(clientType).toString();
      // withRawTypeToken("1234ABC", "com.example.FooProxy");
      println("withRawTypeToken(\"%s\", \"%s\");", OperationKey.hash(binaryName), binaryName);

      TypeElement domainType = (TypeElement) clientToDomainMap.get(clientType);
      if (domainType == null) {
        /*
         * Missing proxy mapping, probably due to an unresolved ProxyForName. If
         * we're running as part of a build tool or an IDE, the
         * mustResolveAllAnnotations() call below will return false. The
         * isMappingRequired() check avoid false-positives on proxy supertypes
         * that extend BaseProxy but that don't declare a ProxyFor annotation.
         */
        if (state.mustResolveAllAnnotations() && state.isMappingRequired(domainType)) {
          state.poison(clientType, Messages.deobfuscatorMissingProxy(clientType.getSimpleName()));
        }
        continue;
      }
      // Generic get-and-add code
      SortedSet<TypeElement> clientTypes = domainToClientMappings.get(domainType);
      if (clientTypes == null) {
        clientTypes = new TreeSet<TypeElement>(comparator);
        domainToClientMappings.put(domainType, clientTypes);
      }
      clientTypes.add(clientType);
    }

    for (Map.Entry<TypeElement, SortedSet<TypeElement>> entry : domainToClientMappings.entrySet()) {
      // Arrays.asList("com.example.FooView1Proxy", "com.example.FooView2Proxy")
      StringBuilder list = new StringBuilder("Arrays.asList(");
      boolean needsComma = false;
      for (TypeElement elt : entry.getValue()) {
        if (needsComma) {
          list.append(", ");
        } else {
          needsComma = true;
        }
        list.append('"').append(state.elements.getBinaryName(elt)).append('"');
      }
      list.append(")");

      // withClientToDomainMappings("com.example.Domain", Arrays.asList(...))
      println("withClientToDomainMappings(\"%s\", %s);", state.elements.getBinaryName(entry
          .getKey()), list);
    }
  }
}
