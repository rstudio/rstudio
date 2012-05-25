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
package com.google.gwt.place.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.PlaceHistoryMapperWithFactory;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import com.google.gwt.place.shared.WithTokenizers;
import com.google.gwt.place.testplacemappers.NoFactory;
import com.google.gwt.place.testplacemappers.WithFactory;
import com.google.gwt.place.testplaces.AbstractTokenizer;
import com.google.gwt.place.testplaces.Place1;
import com.google.gwt.place.testplaces.Place2;
import com.google.gwt.place.testplaces.Place3;
import com.google.gwt.place.testplaces.Place4;
import com.google.gwt.place.testplaces.Place6;
import com.google.gwt.place.testplaces.Tokenizer2;
import com.google.gwt.place.testplaces.Tokenizer3;
import com.google.gwt.place.testplaces.Tokenizer4;
import com.google.gwt.place.testplaces.TokenizerFactory;

import junit.framework.TestCase;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Test case for {@link PlaceHistoryGeneratorContext} that uses mock
 * CompilationStates.
 */
public class PlaceHistoryGeneratorContextTest extends TestCase {

  private static final JType[] EMPTY_JTYPE_ARRAY = new JType[0];

  private static TreeLogger createCompileLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(
        System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  private static TypeOracle createTypeOracle(Resource... resources) {
    Set<Resource> rtn = new HashSet<Resource>(
        Arrays.asList(JavaResourceBase.getStandardResources()));
    rtn.add(new RealJavaResource(Place.class));
    rtn.add(new RealJavaResource(PlaceTokenizer.class));
    rtn.add(new RealJavaResource(PlaceHistoryMapper.class));
    rtn.add(new RealJavaResource(PlaceHistoryMapperWithFactory.class));
    rtn.add(new RealJavaResource(WithTokenizers.class));
    rtn.add(new RealJavaResource(Prefix.class));
    rtn.add(new RealJavaResource(NoFactory.class));
    rtn.add(new RealJavaResource(WithFactory.class));
    rtn.add(new RealJavaResource(TokenizerFactory.class));
    rtn.add(new RealJavaResource(Place1.class));
    rtn.add(new RealJavaResource(Place2.class));
    rtn.add(new RealJavaResource(Place3.class));
    rtn.add(new RealJavaResource(Place4.class));
    rtn.add(new RealJavaResource(Place6.class));
    rtn.add(new RealJavaResource(AbstractTokenizer.class));
    rtn.add(new RealJavaResource(Tokenizer2.class));
    rtn.add(new RealJavaResource(Tokenizer3.class));
    rtn.add(new RealJavaResource(Tokenizer4.class));
    rtn.addAll(Arrays.asList(resources));
    return CompilationStateBuilder.buildFrom(createCompileLogger(), rtn).getTypeOracle();
  }

  public void testCreateNotAnInterface() throws UnableToCompleteException {
    MockJavaResource intf = new MockJavaResource("my.MyPlaceHistoryMapper") {

      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package my;\n");
        code.append("import com.google.gwt.place.shared.PlaceHistoryMapper;\n");

        code.append("public abstract class MyPlaceHistoryMapper implements PlaceHistoryMapper {\n");
        code.append("}\n");
        return code;
      }
    };

    TypeOracle typeOracle = createTypeOracle(intf);

    UnitTestTreeLogger logger = new UnitTestTreeLogger.Builder().createLogger();

    PlaceHistoryGeneratorContext context = PlaceHistoryGeneratorContext.create(
        logger, typeOracle, intf.getTypeName());

    assertNull(context);
  }

  public void testCreateNoFactory() throws UnableToCompleteException,
      NotFoundException {
    doTestCreate(NoFactory.class, null);
  }

  public void testCreateWithFactory() throws UnableToCompleteException,
      NotFoundException {
    doTestCreate(WithFactory.class, TokenizerFactory.class);
  }

  public void testNoFactory() throws UnableToCompleteException,
      NotFoundException {

    TypeOracle typeOracle = createTypeOracle();
    JClassType place1 = typeOracle.getType(Place1.class.getName());
    JClassType place2 = typeOracle.getType(Place2.class.getName());
    JClassType place3 = typeOracle.getType(Place3.class.getName());
    JClassType place4 = typeOracle.getType(Place4.class.getName());
    JClassType place6 = typeOracle.getType(Place6.class.getName());

    PlaceHistoryGeneratorContext context = createContext(TreeLogger.NULL,
        typeOracle, NoFactory.class.getName(), null);

    // Found all place prefixes?
    assertEquals(new HashSet<String>(Arrays.asList("", Place1.Tokenizer.PREFIX,
        "Place2", "Place3", "Place4")), context.getPrefixes());

    // Found all place types and correctly sorted them?
    assertEquals(Arrays.asList(place3, place4, place1, place2, place6),
        new ArrayList<JClassType>(context.getPlaceTypes()));

    // correctly maps place types to their prefixes?
    assertEquals(Place1.Tokenizer.PREFIX, context.getPrefix(place1));
    assertEquals("Place2", context.getPrefix(place2));
    assertEquals("Place3", context.getPrefix(place3));
    assertEquals("Place4", context.getPrefix(place4));
    assertEquals("", context.getPrefix(place6));

    // there obviously shouldn't be factory methods
    assertNull(context.getTokenizerGetter(Place1.Tokenizer.PREFIX));
    assertNull(context.getTokenizerGetter("Place2"));
    assertNull(context.getTokenizerGetter("Place3"));
    assertNull(context.getTokenizerGetter("Place4"));
    assertNull(context.getTokenizerGetter(""));

    // correctly maps prefixes to their tokenizer type?
    assertEquals(typeOracle.getType(Place1.Tokenizer.class.getCanonicalName()),
        context.getTokenizerType(Place1.Tokenizer.PREFIX));
    assertEquals(typeOracle.getType(Tokenizer2.class.getName()),
        context.getTokenizerType("Place2"));
    assertEquals(typeOracle.getType(Tokenizer3.class.getName()),
        context.getTokenizerType("Place3"));
    assertEquals(typeOracle.getType(Tokenizer4.class.getName()),
        context.getTokenizerType("Place4"));
    assertEquals(typeOracle.getType(Place6.Tokenizer.class.getCanonicalName()),
        context.getTokenizerType(""));
  }

  public void testWithFactory() throws UnableToCompleteException,
      NotFoundException {

    TypeOracle typeOracle = createTypeOracle();

    JClassType place1 = typeOracle.getType(Place1.class.getName());
    JClassType place2 = typeOracle.getType(Place2.class.getName());
    JClassType place3 = typeOracle.getType(Place3.class.getName());
    JClassType place4 = typeOracle.getType(Place4.class.getName());
    JClassType place6 = typeOracle.getType(Place6.class.getName());
    JClassType factory = typeOracle.getType(TokenizerFactory.class.getName());

    PlaceHistoryGeneratorContext context = createContext(TreeLogger.NULL,
        typeOracle, WithFactory.class.getName(),
        TokenizerFactory.class.getName());

    // Found all place prefixes?
    assertEquals(new HashSet<String>(Arrays.asList("", Place1.Tokenizer.PREFIX,
        TokenizerFactory.PLACE2_PREFIX, "Place3", "Place4")),
        context.getPrefixes());

    // Found all place types and correctly sorted them?
    assertEquals(Arrays.asList(place3, place4, place1, place2, place6),
        new ArrayList<JClassType>(context.getPlaceTypes()));

    // correctly maps place types to their prefixes?
    assertEquals(Place1.Tokenizer.PREFIX, context.getPrefix(place1));
    assertEquals(TokenizerFactory.PLACE2_PREFIX, context.getPrefix(place2));
    assertEquals("Place3", context.getPrefix(place3));
    assertEquals("Place4", context.getPrefix(place4));
    assertEquals("", context.getPrefix(place6));

    // correctly map prefixes to their factory method (or null)?
    assertEquals(factory.getMethod("getTokenizer1", EMPTY_JTYPE_ARRAY),
        context.getTokenizerGetter(Place1.Tokenizer.PREFIX));
    assertEquals(factory.getMethod("getTokenizer2", EMPTY_JTYPE_ARRAY),
        context.getTokenizerGetter(TokenizerFactory.PLACE2_PREFIX));
    assertEquals(factory.getMethod("getTokenizer3", EMPTY_JTYPE_ARRAY),
        context.getTokenizerGetter("Place3"));
    assertNull(context.getTokenizerGetter("Place4"));
    assertNull(context.getTokenizerGetter(""));

    // correctly maps prefixes to their tokenizer type (or null)?
    assertNull(context.getTokenizerType(Place1.Tokenizer.PREFIX));
    assertNull(context.getTokenizerType(TokenizerFactory.PLACE2_PREFIX));
    assertNull(context.getTokenizerType("Place3"));
    assertEquals(typeOracle.getType(Tokenizer4.class.getName()),
        context.getTokenizerType("Place4"));
    assertEquals(typeOracle.getType(Place6.Tokenizer.class.getCanonicalName()),
        context.getTokenizerType(""));
  }

  public void testDuplicatePrefix() {
    MockJavaResource intf = new MockJavaResource("my.MyPlaceHistoryMapper") {

      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package my;\n");
        code.append("import com.google.gwt.place.shared.PlaceHistoryMapperWithFactory;\n");
        code.append("import com.google.gwt.place.shared.WithTokenizers;\n");
        code.append("import com.google.gwt.place.shared.Prefix;\n");
        code.append("import com.google.gwt.place.testplaces.Place1;\n");
        code.append("import com.google.gwt.place.testplaces.Tokenizer2;\n");

        code.append("@WithTokenizers(Place1.Tokenizer.class)\n");
        code.append("public interface MyPlaceHistoryMapper extends PlaceHistoryMapperWithFactory<MyPlaceHistoryMapper.Factory> {\n");
        code.append("  interface Factory {\n");
        code.append("    @Prefix(Place1.Tokenizer.PREFIX) Tokenizer2 tokenizer2();\n");
        code.append("  }\n");
        code.append("}\n");
        return code;
      }
    };

    TypeOracle typeOracle = createTypeOracle(intf);

    UnitTestTreeLogger.Builder loggerBuilder = new UnitTestTreeLogger.Builder();
    loggerBuilder.expectError(String.format(
        "Found duplicate place prefix \"%s\" on %s, already seen on %s",
        Place1.Tokenizer.PREFIX, Place1.Tokenizer.class.getCanonicalName(),
        intf.getTypeName() + ".Factory#tokenizer2()"), null);
    UnitTestTreeLogger logger = loggerBuilder.createLogger();

    PlaceHistoryGeneratorContext context = createContext(logger, typeOracle,
        intf.getTypeName(), intf.getTypeName() + ".Factory");

    try {
      context.ensureInitialized();
      fail();
    } catch (UnableToCompleteException e) {
      // expected exception
    }

    logger.assertCorrectLogEntries();
  }

  public void testDuplicatePlaceType() {
    MockJavaResource intf = new MockJavaResource("my.MyPlaceHistoryMapper") {

      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package my;\n");
        code.append("import com.google.gwt.place.shared.PlaceHistoryMapperWithFactory;\n");
        code.append("import com.google.gwt.place.shared.PlaceTokenizer;\n");
        code.append("import com.google.gwt.place.shared.WithTokenizers;\n");
        code.append("import com.google.gwt.place.shared.Prefix;\n");
        code.append("import com.google.gwt.place.testplaces.Place1;\n");

        code.append("@WithTokenizers(Place1.Tokenizer.class)\n");
        code.append("public interface MyPlaceHistoryMapper extends PlaceHistoryMapperWithFactory<MyPlaceHistoryMapper.Factory> {\n");
        code.append("  interface Factory {\n");
        code.append("    @Prefix(\"anotherPrefix\") PlaceTokenizer<Place1> bar();\n");
        code.append("  }\n");
        code.append("}\n");
        return code;
      }
    };

    TypeOracle typeOracle = createTypeOracle(intf);

    UnitTestTreeLogger.Builder loggerBuilder = new UnitTestTreeLogger.Builder();
    loggerBuilder.expectError(
        String.format(
            "Found duplicate tokenizer's place type \"%s\" on %s, already seen on %s",
            Place1.class.getName(), Place1.Tokenizer.class.getCanonicalName(),
            intf.getTypeName() + ".Factory#bar()"), null);
    UnitTestTreeLogger logger = loggerBuilder.createLogger();

    PlaceHistoryGeneratorContext context = createContext(logger, typeOracle,
        intf.getTypeName(), intf.getTypeName() + ".Factory");

    try {
      context.ensureInitialized();
      fail();
    } catch (UnableToCompleteException e) {
      // expected exception
    }

    logger.assertCorrectLogEntries();
  }

  public void testPrefixContainingColon() {
    MockJavaResource intf = new MockJavaResource("my.MyPlaceHistoryMapper") {

      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package my;\n");
        code.append("import com.google.gwt.place.shared.Place;\n");
        code.append("import com.google.gwt.place.shared.PlaceHistoryMapperWithFactory;\n");
        code.append("import com.google.gwt.place.shared.PlaceTokenizer;\n");
        code.append("import com.google.gwt.place.shared.Prefix;\n");

        code.append("public interface MyPlaceHistoryMapper extends PlaceHistoryMapperWithFactory<MyPlaceHistoryMapper.Factory> {\n");
        code.append("  interface Factory {\n");
        code.append("    @Prefix(\"foo:bar\") PlaceTokenizer<Place> foo_bar();\n");
        code.append("  }\n");
        code.append("}\n");
        return code;
      }
    };

    TypeOracle typeOracle = createTypeOracle(intf);

    UnitTestTreeLogger.Builder loggerBuilder = new UnitTestTreeLogger.Builder();
    loggerBuilder.expectError(
        "Found place prefix \"foo:bar\" containing separator char \":\", on "
            + intf.getTypeName() + ".Factory#foo_bar()", null);
    UnitTestTreeLogger logger = loggerBuilder.createLogger();

    PlaceHistoryGeneratorContext context = createContext(logger, typeOracle,
        intf.getTypeName(), intf.getTypeName() + ".Factory");

    try {
      context.ensureInitialized();
      fail();
    } catch (UnableToCompleteException e) {
      // expected exception
    }

    logger.assertCorrectLogEntries();
  }

  private void doTestCreate(Class<? extends PlaceHistoryMapper> intf,
      Class<?> factory) throws UnableToCompleteException, NotFoundException {
    UnitTestTreeLogger logger = new UnitTestTreeLogger.Builder().createLogger();

    TypeOracle typeOracle = createTypeOracle();

    PlaceHistoryGeneratorContext context = PlaceHistoryGeneratorContext.create(
        logger, typeOracle, intf.getName());

    assertEquals(typeOracle.getType(String.class.getName()), context.stringType);
    assertEquals(typeOracle.getType(PlaceTokenizer.class.getName()),
        context.placeTokenizerType);
    assertSame(logger, context.logger);
    assertSame(typeOracle, context.typeOracle);

    assertEquals(typeOracle.getType(intf.getName()), context.interfaceType);

    if (factory == null) {
      assertNull(context.factoryType);
    } else {
      assertEquals(typeOracle.getType(factory.getName()), context.factoryType);
    }

    assertEquals(intf.getSimpleName() + "Impl", context.implName);
    assertEquals(intf.getPackage().getName(), context.packageName);

    logger.assertCorrectLogEntries();
  }

  private PlaceHistoryGeneratorContext createContext(TreeLogger logger,
      TypeOracle typeOracle, String interfaceName, String factoryName) {
    return new PlaceHistoryGeneratorContext(logger, typeOracle,
        typeOracle.findType(interfaceName), //
        typeOracle.findType(factoryName), //
        typeOracle.findType(String.class.getName()), //
        typeOracle.findType(PlaceTokenizer.class.getName()), //
        null, null);
  }
}
