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
package com.google.gwt.app.rebind;

import com.google.gwt.app.place.AbstractPlaceHistoryHandler;
import com.google.gwt.app.place.Place;
import com.google.gwt.app.place.PlaceTokenizer;
import com.google.gwt.app.place.AbstractPlaceHistoryHandler.PrefixAndToken;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.Map.Entry;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Generates implementations of
 * {@link com.google.gwt.app.place.PlaceHistoryHandler PlaceHistoryHandler}.
 */
public class PlaceHistoryHandlerGenerator extends Generator {
  private PlaceHistoryGeneratorContext context;

  @Override
  public String generate(TreeLogger logger, GeneratorContext generatorContext,
      String interfaceName) throws UnableToCompleteException {

    context = PlaceHistoryGeneratorContext.create(logger, generatorContext,
        interfaceName);

    PrintWriter out = generatorContext.tryCreate(logger, context.packageName,
        context.implName);

    if (out != null) {
      generateOnce(context, out);
    }

    return context.packageName + "." + context.implName;
  }

  private void generateOnce(PlaceHistoryGeneratorContext context,
      PrintWriter out) throws UnableToCompleteException {

    TreeLogger logger = context.logger.branch(TreeLogger.DEBUG, String.format(
        "Generating implementation of %s", context.interfaceType.getName()));
    ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(
        context.packageName, context.implName);

    String superClassName = String.format("%s<%s>",
        AbstractPlaceHistoryHandler.class.getSimpleName(),
        context.factoryType == null ? "Void" : context.factoryType.getName());
    f.setSuperclass(superClassName);
    f.addImplementedInterface(context.interfaceType.getName());
    
    f.addImport(AbstractPlaceHistoryHandler.class.getName());
    f.addImport(context.interfaceType.getQualifiedSourceName());

    f.addImport(AbstractPlaceHistoryHandler.class.getCanonicalName());
    if (context.factoryType != null) {
      f.addImport(context.factoryType.getQualifiedSourceName());
    }

    f.addImport(Place.class.getCanonicalName());
    f.addImport(PlaceTokenizer.class.getCanonicalName());
    f.addImport(PrefixAndToken.class.getCanonicalName());

    for (Entry<String, JClassType> entry : context.getTokenizerTypes().entrySet()) {
      f.addImport(entry.getValue().getQualifiedSourceName());
      f.addImport(context.getPlaceType(entry.getKey()).getQualifiedSourceName());
    }

    if (context.hasNonFactoryTokenizer()) {
      f.addImport(GWT.class.getCanonicalName());
    }

    SourceWriter sw = f.createSourceWriter(context.generatorContext, out);
    sw.println();

    writeGetPrefixAndToken(context, sw);
    sw.println();

    writeGetTokenizer(context, sw);
    sw.println();

    sw.outdent();
    sw.println("}");
    context.generatorContext.commit(logger, out);
  }

  private void writeGetPrefixAndToken(PlaceHistoryGeneratorContext context,
      SourceWriter sw) throws UnableToCompleteException {
    sw.println("protected PrefixAndToken getPrefixAndToken(Place newPlace) {");
    sw.indent();
    for (String prefix : context.getPrefixes()) {
      String placeTypeName = context.getPlaceType(prefix).getName();

      sw.println("if (newPlace instanceof " + placeTypeName + ") {");
      sw.indent();
      sw.println(placeTypeName + " place = (" + placeTypeName + ") newPlace;");

      JMethod getter = context.getTokenizerGetter(prefix);
      if (getter != null) {
        sw.println(String.format("return new PrefixAndToken(\"%s\", "
            + "factory.%s().getToken(place));", prefix, getter.getName()));
      } else {
        sw.println(String.format(
            "PlaceTokenizer<%s> t = GWT.create(%s.class);", placeTypeName,
            context.getTokenizerType(prefix).getName()));
        sw.println(String.format("return new PrefixAndToken(\"%s\", "
            + "t.getToken((%s) place));", prefix, placeTypeName));
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

      sw.println("if (\"" + prefix + "\".equals(prefix)) {");
      sw.indent();

      if (getter != null) {
        sw.println("return factory." + getter.getName() + "();");
      } else {
        sw.println(String.format("return GWT.create(%s.class);",
            context.getTokenizerType(prefix).getName()));
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
