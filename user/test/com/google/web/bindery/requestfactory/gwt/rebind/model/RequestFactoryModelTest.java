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
package com.google.web.bindery.requestfactory.gwt.rebind.model;

import com.google.web.bindery.autobean.shared.Splittable;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.web.bindery.requestfactory.server.TestContextImpl;
import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.InstanceRequest;
import com.google.web.bindery.requestfactory.shared.Locator;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.Service;
import com.google.web.bindery.requestfactory.shared.ServiceLocator;
import com.google.web.bindery.requestfactory.shared.ValueProxy;

import junit.framework.TestCase;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * Test case for
 * {@link com.google.web.bindery.requestfactory.gwt.rebind.model.RequestFactoryModel} that
 * uses mock CompilationStates.
 */
public class RequestFactoryModelTest extends TestCase {

  /**
   * Constructs an empty interface representation of a type.
   */
  private static class EmptyMockJavaResource extends MockJavaResource {

    private final StringBuilder code = new StringBuilder();

    public EmptyMockJavaResource(Class<?> clazz) {
      super(clazz.getName());

      code.append("package ").append(clazz.getPackage().getName()).append(";\n");
      code.append("public interface ").append(clazz.getSimpleName());

      int numParams = clazz.getTypeParameters().length;
      if (numParams != 0) {
        code.append("<");
        for (int i = 0; i < numParams; i++) {
          if (i != 0) {
            code.append(",");
          }
          code.append("T").append(i);
        }
        code.append(">");
      }

      code.append("{}\n");
    }

    @Override
    public CharSequence getContent() {
      return code;
    }
  }

  /**
   * Loads the actual source of a type. This should be used only for types
   * directly tested by this test. Note that use of this class requires your
   * source files to be on your classpath.
   */
  private static class RealJavaResource extends MockJavaResource {

    public RealJavaResource(Class<?> clazz) {
      super(clazz.getName());
    }

    @Override
    public CharSequence getContent() {
      String resourceName = getTypeName().replace('.', '/') + ".java";
      InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
          resourceName);
      assertNotNull("Could not open " + resourceName, stream);
      return Util.readStreamAsString(stream);
    }
  }

  private static TreeLogger createCompileLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(
        System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  private TreeLogger logger;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    logger = createCompileLogger();
  }

  public void testBadCollectionType() {
    testModelWithMethodDecl(
        "Request<SortedSet<Integer>> badReturnType();",
        "Requests that return collections may be declared with java.util.List or java.util.Set only");
  }

  public void testBadCollectionTypeNotParameterized() {
    testModelWithMethodDecl("Request<SortedSet> badReturnType();",
        "Requests that return collections of List or Set must be parameterized");
  }

  public void testBadReturnType() {
    testModelWithMethodDecl("Request<Iterable> badReturnType();",
        "Invalid Request parameterization java.lang.Iterable");
  }

  public void testDuplicateBooleanGetters() {
    testModelWithMethodDecl("Request<t.ProxyWithRepeatedGetters> method();",
        "Duplicate accessors for property foo: getFoo() and isFoo()");
  }

  public void testMissingProxyFor() {
    testModelWithMethodDeclArgs("Request<TestProxy> okMethodProxy();",
        TestContextImpl.class.getName(), null,
        "The t.TestProxy type does not have a @ProxyFor, "
            + "@ProxyForName, or @JsonRpcProxy annotation");
  }

  public void testMissingService() {
    testModelWithMethodDeclArgs("Request<String> okMethod();", null,
        TestContextImpl.class.getName(),
        "RequestContext subtype t.TestContext is missing a "
            + "@Service or @JsonRpcService annotation");
  }

  public void testModelWithMethodDecl(final String clientMethodDecls,
      String... expected) {
    testModelWithMethodDeclArgs(clientMethodDecls,
        TestContextImpl.class.getName(), TestContextImpl.class.getName(),
        expected);
  }

  public void testModelWithMethodDeclArgs(final String clientMethodDecls,
      final String serviceClass, String proxyClass, String... expected) {
    Set<Resource> javaResources = getJavaResources(proxyClass);
    javaResources.add(new MockJavaResource("t.TestRequestFactory") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + RequestFactory.class.getName() + ";\n");
        code.append("interface TestRequestFactory extends RequestFactory {\n");
        code.append("TestContext testContext();");
        code.append("}");
        return code;
      }
    });
    javaResources.add(new MockJavaResource("t.TestContext") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Request.class.getName() + ";\n");
        code.append("import " + InstanceRequest.class.getName() + ";\n");

        code.append("import " + RequestContext.class.getName() + ";\n");
        code.append("import " + SortedSet.class.getName() + ";\n");
        code.append("import " + List.class.getName() + ";\n");
        code.append("import " + Set.class.getName() + ";\n");
        code.append("import " + Service.class.getName() + ";\n");
        code.append("import " + TestContextImpl.class.getName() + ";\n");

        if (serviceClass != null) {
          code.append("@Service(" + serviceClass + ".class)");
        }
        code.append("interface TestContext extends RequestContext {\n");
        code.append(clientMethodDecls);
        code.append("}");
        return code;
      }
    });

    CompilationState state = CompilationStateBuilder.buildFrom(logger,
        javaResources);

    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.ERROR);
    for (String expectedMsg : expected) {
      builder.expectError(expectedMsg, null);
    }
    builder.expectError(RequestFactoryModel.poisonedMessage(), null);
    UnitTestTreeLogger testLogger = builder.createLogger();
    try {
      new RequestFactoryModel(testLogger, state.getTypeOracle().findType(
          "t.TestRequestFactory"));
      fail("Should have complained");
    } catch (UnableToCompleteException e) {
    }
    testLogger.assertCorrectLogEntries();
  }

  private Set<Resource> getJavaResources(final String proxyClass) {
    MockJavaResource[] javaFiles = {new MockJavaResource("t.AddressProxy") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + ProxyFor.class.getName() + ";\n");
        code.append("import " + EntityProxy.class.getName() + ";\n");
        if (proxyClass != null) {
          code.append("@ProxyFor(" + proxyClass + ".class)");
        }
        code.append("interface TestProxy extends EntityProxy {\n");
        code.append("}");
        System.out.println(code);
        return code;
      }
    }, new MockJavaResource("t.ProxyWithRepeatedGetters") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + ProxyFor.class.getName() + ";\n");
        code.append("import " + EntityProxy.class.getName() + ";\n");
        if (proxyClass != null) {
          code.append("@ProxyFor(" + proxyClass + ".class)");
        }
        code.append("interface ProxyWithRepeatedGetters extends EntityProxy {\n");
        code.append("  boolean getFoo();");
        code.append("  boolean isFoo();");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("java.util.List") {
        // Tests a Driver interface that extends more than RFED
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package java.util;\n");
        code.append("public interface List<T> extends Collection<T> {\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("java.util.Set") {
        // Tests a Driver interface that extends more than RFED
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package java.util;\n");
        code.append("public interface Set<T> extends Collection<T> {\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("java.util.SortedSet") {
        // Tests a Driver interface that extends more than RFED
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package java.util;\n");
        code.append("public interface SortedSet<T> extends Set<T> {\n");
        code.append("}");
        return code;
      }
    }};

    Set<Resource> toReturn = new HashSet<Resource>(Arrays.asList(javaFiles));

    toReturn.addAll(Arrays.asList(new Resource[] {
        new EmptyMockJavaResource(Iterable.class),
        new EmptyMockJavaResource(EntityProxy.class),
        new EmptyMockJavaResource(InstanceRequest.class),
        new EmptyMockJavaResource(Locator.class),
        new EmptyMockJavaResource(RequestFactory.class),
        new EmptyMockJavaResource(Receiver.class),
        new EmptyMockJavaResource(ServiceLocator.class),
        new EmptyMockJavaResource(Splittable.class),
        new EmptyMockJavaResource(ValueProxy.class),

        new RealJavaResource(Request.class),
        new RealJavaResource(Service.class),
        new RealJavaResource(ProxyFor.class),
        new EmptyMockJavaResource(RequestContext.class),}));
    toReturn.addAll(Arrays.asList(JavaResourceBase.getStandardResources()));
    return toReturn;
  }
}
