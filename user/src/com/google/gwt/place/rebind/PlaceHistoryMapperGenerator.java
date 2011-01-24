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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.place.impl.AbstractPlaceHistoryMapper;
import com.google.gwt.place.impl.AbstractPlaceHistoryMapper.PrefixAndToken;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;

/**
 * Generates implementations of
 * {@link com.google.gwt.place.shared.PlaceHistoryMapper PlaceHistoryMapper}.
 */
public class PlaceHistoryMapperGenerator extends Generator {
  private PlaceHistoryGeneratorContext context;

  @Override
  public String generate(TreeLogger logger, GeneratorContext generatorContext,
      String interfaceName) throws UnableToCompleteException {

    context = PlaceHistoryGeneratorContext.create(logger,
        generatorContext.getTypeOracle(), interfaceName);

    if (context == null) {
      return null;
    }

    PrintWriter out = generatorContext.tryCreate(logger, context.packageName,
        context.implName);

    if (out != null) {
      generateOnce(generatorContext, context, out);
    }

    return context.packageName + "." + context.implName;
  }

  private void generateOnce(GeneratorContext generatorContext, PlaceHistoryGeneratorContext context,
      PrintWriter out) throws UnableToCompleteException {

    TreeLogger logger = context.logger.branch(TreeLogger.DEBUG, String.format(
        "Generating implementation of %s", context.interfaceType.getName()));
    ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(
        context.packageName, context.implName);

    String superClassName = String.format("%s<%s>",
        AbstractPlaceHistoryMapper.class.getSimpleName(),
        context.factoryType == null ? "Void" : context.factoryType.getName());
    f.setSuperclass(superClassName);
    f.addImplementedInterface(context.interfaceType.getName());

    f.addImport(AbstractPlaceHistoryMapper.class.getName());
    f.addImport(context.interfaceType.getQualifiedSourceName());

    f.addImport(AbstractPlaceHistoryMapper.class.getCanonicalName());
    if (context.factoryType != null) {
      f.addImport(context.factoryType.getQualifiedSourceName());
    }

    f.addImport(Place.class.getCanonicalName());
    f.addImport(PlaceTokenizer.class.getCanonicalName());
    f.addImport(PrefixAndToken.class.getCanonicalName());

    f.addImport(GWT.class.getCanonicalName());

    SourceWriter sw = f.createSourceWriter(generatorContext, out);
    sw.println();

    writeGetPrefixAndToken(context, sw);
    sw.println();

    writeGetTokenizer(context, sw);
    sw.println();

    sw.outdent();
    sw.println("}");
    generatorContext.commit(logger, out);
  }

  private void writeGetPrefixAndToken(PlaceHistoryGeneratorContext context,
      SourceWriter sw) throws UnableToCompleteException {
    sw.println("protected PrefixAndToken getPrefixAndToken(Place newPlace) {");
    sw.indent();
    for (JClassType placeType : context.getPlaceTypes()) {
      String placeTypeName = placeType.getQualifiedSourceName();
      String prefix = context.getPrefix(placeType);

      sw.println("if (newPlace instanceof " + placeTypeName + ") {");
      sw.indent();
      sw.println(placeTypeName + " place = (" + placeTypeName + ") newPlace;");

      JMethod getter = context.getTokenizerGetter(prefix);
      if (getter != null) {
        sw.println(String.format("return new PrefixAndToken(\"%s\", "
            + "factory.%s().getToken(place));", escape(prefix),
            getter.getName()));
      } else {
        sw.println(String.format(
            "PlaceTokenizer<%s> t = GWT.create(%s.class);", placeTypeName,
            context.getTokenizerType(prefix).getQualifiedSourceName()));
        sw.println(String.format("return new PrefixAndToken(\"%s\", "
            + "t.getToken((%s) place));", escape(prefix), placeTypeName));
      }

      sw.outdent();
      sw.println("}");
    }

    sw.println("return null;");
    sw.outdent();
    sw.println("}");
  }

  private void writeGetTokenizer(PlaceHistoryGeneratorContext context,
      SourceWriter sw) throws UnableToCompleteException {
    sw.println("protected PlaceTokenizer<?> getTokenizer(String prefix) {");
    sw.indent();

    for (String prefix : context.getPrefixes()) {
      JMethod getter = context.getTokenizerGetter(prefix);

      sw.println("if (\"" + escape(prefix) + "\".equals(prefix)) {");
      sw.indent();

      if (getter != null) {
        sw.println("return factory." + getter.getName() + "();");
      } else {
        sw.println(String.format("return GWT.create(%s.class);",
            context.getTokenizerType(prefix).getQualifiedSourceName()));
      }

      sw.outdent();
      sw.println("}");
    }

    sw.println("return null;");
    sw.outdent();
    sw.println("}");
    sw.outdent();
  }

}
