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
package com.google.web.bindery.requestfactory.gwt.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.javac.testing.GeneratorContextBuilder;
import com.google.gwt.dev.javac.testing.JavaSource;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Tests for {@link RequestFactoryGenerator}.
 */
public class RequestFactoryGeneratorTest extends TestCase {

  // Common imports for generated clients.
  private static final String COMMON_IMPORTS = "import com.google.api.gwt.shared.AuthScope;\n"
      + "import com.google.web.bindery.autobean.shared.AutoBean.PropertyName;\n"
      + "import com.google.web.bindery.requestfactory.shared.JsonRpcService;\n"
      + "import com.google.web.bindery.requestfactory.shared.Request;\n"
      + "import com.google.web.bindery.requestfactory.shared.RequestContext;\n"
      + "import com.google.web.bindery.requestfactory.shared.RequestFactory;\n";

  // Classes needed by by RequestFactoryModel
  private static final JavaSource SET = createMockJavaSource(
      "java.util.Set",
      "package java.util;",
      "public interface Set<V> {}");

  private static final JavaSource LIST = createMockJavaSource(
      "java.util.List",
      "package java.util;",
      "public interface List<V> {}");

  private static final JavaSource AUTH_SCOPE = createMockJavaSource(
      "com.google.api.gwt.shared.AuthScope",
      "package com.google.api.gwt.shared;",
      "public @interface AuthScope {}");

  private static final JavaSource SPLITTABLE = createMockJavaSource(
      "com.google.web.bindery.autobean.shared.Splittable",
      "package com.google.web.bindery.autobean.shared;",
      "public interface Splittable {}");

  private static final JavaSource PROPERTY_NAME = createMockJavaSource(
      "com.google.web.bindery.autobean.shared.AutoBean.PropertyName",
      "package com.google.web.bindery.autobean.shared.AutoBean;",
      "public @interface PropertyName {}");

  private static final JavaSource ENTITY_PROXY = createMockJavaSource(
      "com.google.web.bindery.requestfactory.shared.EntityProxy",
      "package com.google.web.bindery.requestfactory.shared;",
      "public interface EntityProxy {}");

  private static final JavaSource INSTANCE_REQUEST = createMockJavaSource(
      "com.google.web.bindery.requestfactory.shared.InstanceRequest",
      "package com.google.web.bindery.requestfactory.shared;",
      "public interface InstanceRequest {}");

  private static final JavaSource JSON_RPC_SERVICE = createMockJavaSource(
      "com.google.web.bindery.requestfactory.shared.JsonRpcService",
      "package com.google.web.bindery.requestfactory.shared;",
      "public @interface JsonRpcService {}");

  private static final JavaSource REQUEST = createMockJavaSource(
      "com.google.web.bindery.requestfactory.shared.Request",
      "package com.google.web.bindery.requestfactory.shared;",
      "public interface Request<T> {}");

  private static final JavaSource REQUEST_CONTEXT = createMockJavaSource(
      "com.google.web.bindery.requestfactory.shared.RequestContext",
      "package com.google.web.bindery.requestfactory.shared;",
      "public interface RequestContext {}");

  private static final JavaSource REQUEST_FACTORY = createMockJavaSource(
      "com.google.web.bindery.requestfactory.shared.RequestFactory",
      "package com.google.web.bindery.requestfactory.shared;",
      "public interface RequestFactory {}");

  private static final JavaSource VALUE_PROXY = createMockJavaSource(
      "com.google.web.bindery.requestfactory.shared.ValueProxy",
      "package com.google.web.bindery.requestfactory.shared;",
      "public interface ValueProxy {}");

  private TreeLogger logger;
  private StringWriter writer;
  private GeneratorContextBuilder contextBuilder;
  private RequestFactoryGenerator generator;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    writer = new StringWriter();
    logger = createTreeLogger(new PrintWriter(writer));
    generator = new RequestFactoryGenerator();
    contextBuilder = GeneratorContextBuilder.newCoreBasedBuilder();
    contextBuilder.add(SET);
    contextBuilder.add(LIST);
    contextBuilder.add(AUTH_SCOPE);
    contextBuilder.add(SPLITTABLE);
    contextBuilder.add(PROPERTY_NAME);
    contextBuilder.add(ENTITY_PROXY);
    contextBuilder.add(INSTANCE_REQUEST);
    contextBuilder.add(JSON_RPC_SERVICE);
    contextBuilder.add(REQUEST);
    contextBuilder.add(REQUEST_CONTEXT);
    contextBuilder.add(REQUEST_FACTORY);
    contextBuilder.add(VALUE_PROXY);
  }

  private static TreeLogger createTreeLogger(PrintWriter writer) {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(writer);
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  public void testContextIsNotAService() throws Exception {
    JavaSource simpleEnumTest = createMockJavaSource("test.Simple",
        "package test;",
        COMMON_IMPORTS,
        "public interface Simple extends RequestFactory {",
        "  SimpleContext simple();",
        "  public interface SimpleContext extends RequestContext {",
        "    GetByIdRequest getById(Long id);",
        "    public interface GetByIdRequest extends Request<String> {",
        "      GetByIdRequest setId(Long id);",
        "    }",
        "  }",
        "}");

    contextBuilder.add(simpleEnumTest);
    StandardGeneratorContext ctx =
        (StandardGeneratorContext) contextBuilder.buildGeneratorContext();
    assertNotNull(ctx.getTypeOracle().findType("test.Simple"));
    try {
      assertEquals("test.SimpleImpl",
          generator.generate(logger, contextBuilder.buildGeneratorContext(), "test.Simple"));
      fail("RequestContext is not annotated as a service");
    } catch (UnableToCompleteException expected) {
      assertTrue(writer.toString().contains("RequestContext subtype test.Simple.SimpleContext "
          + "is missing a @Service or @JsonRpcService annotation"));
    }
  }

  public void testExtraEnumsPulledFromParameterizedParameters() throws Exception {
    JavaSource simpleEnumTest = createMockJavaSource("test.Simple",
        "package test;",
        COMMON_IMPORTS,
        "public interface Simple extends RequestFactory {",
        "  SimpleContext simple();",
        "  @JsonRpcService",
        "  public interface SimpleContext extends RequestContext {",
        "    GetByIdRequest getById(Long id);",
        "    public interface GetByIdRequest extends Request<String> {",
        "      public enum GetByIdEnum {}",
        "      GetByIdRequest setId(Long id);",
        "      GetByIdRequest setEnums(java.util.List<GetByIdEnum> enums);",
        "    }",
        "  }",
        "}");

    contextBuilder.add(simpleEnumTest);
    StandardGeneratorContext ctx =
        (StandardGeneratorContext) contextBuilder.buildGeneratorContext();
    assertNotNull(ctx.getTypeOracle().findType("test.Simple"));
    try {
      assertEquals("test.SimpleImpl", generator.generate(logger, ctx, "test.Simple"));
    } catch (UnableToCompleteException e) {
      throw new RuntimeException("Could not complete with errors:\n" + writer.toString());
    }
    assertNotNull(ctx.getGeneratedUnitMap().get("test.SimpleImpl"));
    String source = ctx.getGeneratedUnitMap().get("test.SimpleImpl").getSource();
    assertTrue(source.contains("@com.google.web.bindery.autobean.shared.impl.EnumMap.ExtraEnums("
        + "{test.Simple.SimpleContext.GetByIdRequest.GetByIdEnum.class})"));
  }

  private static JavaSource createMockJavaSource(String typeName, final String... lines) {
    return new JavaSource(typeName) {
      @Override
      public String getSource() {
        StringBuilder code = new StringBuilder();
        for (String line : lines) {
          code.append(line + "\n");
        }
        return code.toString();
      }
    };
  }
}


