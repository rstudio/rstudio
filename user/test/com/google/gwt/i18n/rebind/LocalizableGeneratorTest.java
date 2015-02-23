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
package com.google.gwt.i18n.rebind;

import com.google.gwt.codegen.server.AbortablePrintWriter;
import com.google.gwt.codegen.server.CodeGenContext;
import com.google.gwt.codegen.server.JavaSourceWriterBuilder;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.DefaultConfigurationProperty;
import com.google.gwt.core.ext.DefaultSelectionProperty;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.StubGeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.TypeOracleTestingUtils;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.MockResourceOracle;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.dev.shell.FailErrorLogger;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.i18n.server.GwtLocaleFactoryImpl;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Test {@link LocaleInfoGenerator}.
 */
public class LocalizableGeneratorTest extends TestCase {

  private static final MockJavaResource LOCALIZABLE = new MockJavaResource(
      "com.google.gwt.i18n.shared.Localizable") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.i18n.shared;\n");
      code.append("public interface Localizable { }\n");
      return code;
    }
  };

  private static final MockJavaResource LOCALIZABLE_RESOURCE = new MockJavaResource(
      "com.google.gwt.i18n.client.LocalizableResource") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.i18n.client;\n");
      code.append("import com.google.gwt.i18n.shared.Localizable;\n");
      code.append("public interface LocalizableResource extends Localizable { }\n");
      return code;
    }
  };

  private static final MockJavaResource MESSAGES = new MockJavaResource(
      "com.google.gwt.i18n.client.Messages") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.i18n.client;\n");
      code.append("public interface Messages extends LocalizableResource {\n");
      code.append("  public @interface DefaultMessage {\n");
      code.append("    String value();\n");
      code.append("  }\n");
      code.append("}\n");
      return code;
    }
  };

  private static final MockJavaResource CONSTANTS = new MockJavaResource(
      "com.google.gwt.i18n.client.Constants") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.i18n.client;\n");
      code.append("public interface Constants extends LocalizableResource { }\n");
      return code;
    }
  };

  private static final MockJavaResource CONSTANTS_WITH_LOOKUP = new MockJavaResource(
      "com.google.gwt.i18n.client.ConstantsWithLookup") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.i18n.client;\n");
      code.append("public interface ConstantsWithLookup extends LocalizableResource { }\n");
      return code;
    }
  };

  private static final MockJavaResource SAFE_HTML = new MockJavaResource(
      "com.google.gwt.safehtml.shared.SafeHtml") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.safehtml.shared;\n");
      code.append("public interface SafeHtml { }\n");
      return code;
    }
  };

  private static final MockJavaResource TEST = new MockJavaResource(
      "foo.Test") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;\n");
      code.append("import com.google.gwt.i18n.shared.Localizable;\n");
      code.append("import java.util.Map;\n");
      code.append("public interface Test extends Localizable {\n");
      code.append("  void foo();\n");
      code.append("  Map<String, String> bar(Map<String, String> map);\n");
      code.append("  <T> T baz(Map<String, T> list, String key);\n");
      code.append("}\n");
      return code;
    }
  };

  private static final MockJavaResource TEST_CLASS = new MockJavaResource(
      "foo.TestClass") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;\n");
      code.append("import com.google.gwt.i18n.shared.Localizable;\n");
      code.append("public class TestClass implements Localizable {\n");
      code.append("  public void foo() {}\n");
      code.append("  public final void fooFinal() {}\n");
      code.append("  protected void bar() {}\n");
      code.append("  private void baz() {}\n");
      code.append("  void biff() {}\n");
      code.append("}\n");
      return code;
    }
  };

  private static final MockJavaResource TEST_EN = new MockJavaResource(
      "foo.Test_en") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;\n");
      code.append("import java.util.Map;\n");
      code.append("public class Test_en implements Test {\n");
      code.append("  public void foo() {}\n");
      code.append("  public Map<String, String>  bar(Map<String, String> map) { return null; }\n");
      code.append("  public <T> T baz(Map<String, T> list, String key) { return null; }\n");
      code.append("}\n");
      return code;
    }
  };

  private static final MockJavaResource TEST_EN_GB = new MockJavaResource(
      "foo.Test_en_GB") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;\n");
      code.append("public class Test_en_GB extends Test_en {\n");
      code.append("  @Override public void foo() {}\n");
      code.append("}\n");
      return code;
    }
  };

  private static final MockJavaResource TEST_EN_US = new MockJavaResource(
      "foo.Test_en_US") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;\n");
      code.append("public class Test_en_US extends Test_en {\n");
      code.append("  @Override public void foo() {}\n");
      code.append("}\n");
      return code;
    }
  };

  private static final MockJavaResource TEST_MESSAGES = new MockJavaResource(
      "foo.TestMessages") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;\n");
      code.append("import com.google.gwt.i18n.client.Messages;\n");
      code.append("import com.google.gwt.i18n.client.Messages.DefaultMessage;\n");
      code.append("public interface TestMessages extends Messages {\n");
      code.append("  @DefaultMessage(\"Abc\")\n");
      code.append("  String message();\n");
      code.append("}\n");
      return code;
    }
  };

  private static final MockJavaResource TEST_MESSAGES_WRONG_RETURN_TYPE = new MockJavaResource(
      "foo.TestMessagesWrongReturnType") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;\n");
      code.append("import com.google.gwt.i18n.client.Messages;\n");
      code.append("import com.google.gwt.i18n.client.Messages.DefaultMessage;\n");
      code.append("public interface TestMessagesWrongReturnType extends Messages {\n");
      code.append("  @DefaultMessage(\"Abc\")\n");
      code.append("  Object message();\n");
      code.append("}\n");
      return code;
    }
  };

  private static final MockJavaResource TEST_MESSAGES_SAFE_HTML_AS_STRING = new MockJavaResource(
      "foo.TestMessagesSafeHtmlAsString") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;\n");
      code.append("import com.google.gwt.i18n.client.Messages;\n");
      code.append("import com.google.gwt.i18n.client.Messages.DefaultMessage;\n");
      code.append("import com.google.gwt.safehtml.shared.SafeHtml;\n");
      code.append("public interface TestMessagesSafeHtmlAsString extends Messages {\n");
      code.append("  @DefaultMessage(\"Abc {0}\")\n");
      code.append("  String message(SafeHtml param);\n");
      code.append("}\n");
      return code;
    }
  };

  private Map<String, StringWriter> bufs;

  private CodeGenContext ctx;

  private GwtLocaleFactory factory;

  private TypeOracle typeOracle;

  public void testNotOverridable() throws NotFoundException {
    JClassType testClass = typeOracle.getType("foo.TestClass");
    LocalizableGenerator gen = new LocalizableGenerator();
    GwtLocale en = factory.fromString("en");
    Map<String, Set<GwtLocale>> localeMap = new TreeMap<String, Set<GwtLocale>>();
    String genClass = gen.generateRuntimeSelection(ctx, testClass,
        testClass.getQualifiedSourceName(), en, localeMap);
    assertEquals("foo.TestClass_en_runtimeSelection", genClass);
    StringWriter buf = bufs.get("TestClass_en_runtimeSelection");
    String genText = buf.toString();
    assertTrue("Should have delegated foo", genText.contains("foo("));
    assertFalse("Should not have delegated fooFinal", genText.contains("fooFinal("));
    assertTrue("Should have delegated bar", genText.contains("bar("));
    assertFalse("Should not have delegated baz", genText.contains("baz("));
    assertTrue("Should have delegated biff", genText.contains("biff("));
  }

  public void testRuntimeSelection() throws IOException, NotFoundException {
    JClassType test = typeOracle.getType("foo.Test");
    LocalizableGenerator gen = new LocalizableGenerator();
    GwtLocale en = factory.fromString("en");
    GwtLocale en_US = factory.fromString("en_US");
    GwtLocale en_US_POSIX = factory.fromString("en_US_POSIX");
    GwtLocale en_GB = factory.fromString("en_GB");
    GwtLocale en_PK = factory.fromString("en_PK");
    Map<String, Set<GwtLocale>> localeMap = new TreeMap<String, Set<GwtLocale>>();
    localeMap.put("foo.Test_en_US", new TreeSet<GwtLocale>(Arrays.asList(en_US, en_US_POSIX)));
    localeMap.put("foo.Test_en_GB", new TreeSet<GwtLocale>(Arrays.asList(en_GB)));
    localeMap.put("foo.Test_en", new TreeSet<GwtLocale>(Arrays.asList(en_PK)));
    String genClass = gen.generateRuntimeSelection(ctx, test, test.getQualifiedSourceName(), en,
        localeMap);
    assertEquals("foo.Test_en_runtimeSelection", genClass);
    StringWriter buf = bufs.get("Test_en_runtimeSelection");
    String genText = buf.toString();
    String ensureStartString = "void ensureInstance() {\n";
    int ensurePos = genText.indexOf(ensureStartString);
    assertTrue("Did not find ensureInstance", ensurePos >= 0);
    ensurePos += ensureStartString.length();
    String ensureEndString = "  }\n}\n";
    int ensureEndPos = genText.length() - ensureEndString.length();
    assertEquals(ensureEndString, genText.substring(ensureEndPos));
    String ensureBody = genText.substring(ensurePos, ensureEndPos);
    BufferedReader reader = new BufferedReader(new StringReader(ensureBody));
    // skip past prolog
    String line = reader.readLine();
    while (!line.contains("getLocaleName()")) {
      line = reader.readLine();
    }
    assertEquals("if (\"en_GB\".equals(locale)) {", reader.readLine().trim());
    assertEquals("instance = new foo.Test_en_GB();", reader.readLine().trim());
    assertEquals("return;", reader.readLine().trim());
    assertEquals("}", reader.readLine().trim());
    assertEquals("if (\"en_US\".equals(locale)", reader.readLine().trim());
    assertEquals("|| \"en_US_POSIX\".equals(locale)) {", reader.readLine().trim());
    assertEquals("instance = new foo.Test_en_US();", reader.readLine().trim());
    assertEquals("return;", reader.readLine().trim());
    assertEquals("}", reader.readLine().trim());
    assertEquals("instance = new foo.Test();", reader.readLine().trim());
    assertNull(reader.readLine());
  }

  public void testMessages() throws UnableToCompleteException {
    GeneratorContext context = new MockGeneratorContext(typeOracle, bufs);
    TreeLogger logger = new FailErrorLogger();

    LocalizableGenerator gen = new LocalizableGenerator();
    String generatedClassName = gen.generate(logger, context, TEST_MESSAGES.getTypeName());

    StringWriter writer = bufs.get(generatedClassName);
    assertNotNull("Class " + generatedClassName + " not generated", writer);
  }

  public void testMessagesWrongReturnType() {
    GeneratorContext context = new MockGeneratorContext(typeOracle, bufs);
    UnitTestTreeLogger.Builder loggerBuilder = new UnitTestTreeLogger.Builder();
    loggerBuilder.expectError(Pattern.compile(
        "All methods in interfaces extending Messages must have a return type .*"),
        null /* no exception */);
    UnitTestTreeLogger logger =  loggerBuilder.createLogger();

    LocalizableGenerator gen = new LocalizableGenerator();
    try {
      gen.generate(logger, context, TEST_MESSAGES_WRONG_RETURN_TYPE.getTypeName());
      fail("generate() should have failed");
    } catch (UnableToCompleteException e) {
      // ok
    }
    logger.assertLogEntriesContainExpected();
  }

  public void testMessagesWithStringReturnTypeAndSafeHtmlArgument() {
    GeneratorContext context = new MockGeneratorContext(typeOracle, bufs);
    UnitTestTreeLogger.Builder loggerBuilder = new UnitTestTreeLogger.Builder();
    loggerBuilder.expectError(
        "Message methods with SafeHtml arguments can only have SafeHtml return type",
        null /* no exception */);
    UnitTestTreeLogger logger =  loggerBuilder.createLogger();

    LocalizableGenerator gen = new LocalizableGenerator();
    try {
      gen.generate(logger, context, TEST_MESSAGES_SAFE_HTML_AS_STRING.getTypeName());
      fail("generate() should have failed");
    } catch (UnableToCompleteException e) {
      // ok
    }
    logger.assertLogEntriesContainExpected();
  }

  @Override
  protected void setUp() throws NotFoundException {
    factory = new GwtLocaleFactoryImpl();
    TreeLogger logger = new FailErrorLogger();
    typeOracle = TypeOracleTestingUtils.buildStandardTypeOracleWith(
        logger, LOCALIZABLE, LOCALIZABLE_RESOURCE, MESSAGES, CONSTANTS, CONSTANTS_WITH_LOOKUP,
        SAFE_HTML, TEST, TEST_EN, TEST_EN_US, TEST_EN_GB, TEST_CLASS, TEST_MESSAGES,
        TEST_MESSAGES_WRONG_RETURN_TYPE, TEST_MESSAGES_SAFE_HTML_AS_STRING);
    bufs = new HashMap<String, StringWriter>();
    ctx = new CodeGenContext() {
      @Override
      public JavaSourceWriterBuilder addClass(String pkgName, String className) {
        return addClass(null, pkgName, className);
      }
      
      @Override
      public JavaSourceWriterBuilder addClass(String superPath, String pkgName, String className) {
        StringWriter buf = new StringWriter();
        bufs.put(className, buf);
        AbortablePrintWriter apw = new AbortablePrintWriter(new PrintWriter(buf));
        return new JavaSourceWriterBuilder(apw, pkgName, className);
      }
      
      @Override
      public void error(String msg) {
        fail(msg);
      }
      
      @Override
      public void error(String msg, Throwable cause) {
        fail(msg);
      }
      
      @Override
      public void error(Throwable cause) {
        fail(cause.getMessage());
      }
      
      @Override
      public void warn(String msg) {
        System.out.println(msg);
      }
      
      @Override
      public void warn(String msg, Throwable cause) {
        System.out.println(msg);
      }
      
      @Override
      public void warn(Throwable cause) {
        System.out.println(cause.getMessage());
      }
    };
  }

  private static final class MockGeneratorContext extends StubGeneratorContext {
    private final PropertyOracle propertyOracle = new MockI18nPropertyOracle();
    private final ResourceOracle resourceOracle = new MockResourceOracle();
    private final TypeOracle typeOracle;
    private final Map<String, StringWriter> bufs;

    private MockGeneratorContext(TypeOracle typeOracle, Map<String, StringWriter> bufs) {
      this.typeOracle = typeOracle;
      this.bufs = bufs;
    }

    @Override
    public PropertyOracle getPropertyOracle() {
      return propertyOracle;
    }

    @Override
    public ResourceOracle getResourcesOracle() {
      return resourceOracle;
    }

    @Override
    public TypeOracle getTypeOracle() {
      return typeOracle;
    }

    @Override
    public PrintWriter tryCreate(TreeLogger logger, String packageName, String simpleName) {
      StringWriter writer = new StringWriter();
      bufs.put(packageName + "." + simpleName, writer);
      return new PrintWriter(writer);
    }

    @Override
    public void commit(TreeLogger logger, PrintWriter pw) {
      pw.flush();
    }
  }

  private static final class MockI18nPropertyOracle implements PropertyOracle {
    @Override
    public SelectionProperty getSelectionProperty(TreeLogger logger, String propertyName)
        throws BadPropertyValueException {
      if (LocaleUtils.PROP_LOCALE.equals(propertyName)) {
        TreeSet<String> allowedLocales = new TreeSet<String>();
        allowedLocales.add("en");
        return new DefaultSelectionProperty("en", "en", propertyName, allowedLocales);
      }
      throw new BadPropertyValueException(propertyName);
    }

    @Override
    public ConfigurationProperty getConfigurationProperty(String propertyName)
        throws BadPropertyValueException {
      String value;
      if (LocaleUtils.PROP_LOCALE_COOKIE.equals(propertyName)) {
        value = "";
      } else if (LocaleUtils.PROP_LOCALE_QUERY_PARAM.equals(propertyName)) {
        value = "locale";
      } else if (LocaleUtils.PROP_RUNTIME_LOCALES.equals(propertyName)) {
        value = "en";
      } else {
        throw new BadPropertyValueException(propertyName);
      }
      return new DefaultConfigurationProperty(propertyName, Collections.singletonList(value));
    }
  }
}
