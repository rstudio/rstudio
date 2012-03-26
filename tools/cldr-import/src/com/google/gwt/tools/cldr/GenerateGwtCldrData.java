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
package com.google.gwt.tools.cldr;

import com.google.gwt.i18n.server.GwtLocaleFactoryImpl;
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import com.ibm.icu.dev.tool.UOption;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generate a country list for each locale, taking into account the literate
 * population of each country speaking the language.
 */
@SuppressWarnings("unchecked")
public class GenerateGwtCldrData {

  private static final GwtLocaleFactory factory = new GwtLocaleFactoryImpl();

  private static final String DEFAULT_PROCESSORS = "CurrencyDataProcessor,"
      + "DateTimeFormatInfoProcessor,ListFormattingProcessor,LocalizedNamesProcessor";

  public static void main(String[] args) throws IOException, SecurityException,
      NoSuchMethodException, IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException {
    System.out.println("Starting to generate from CLDR data (ignore -D lines "
        + "produced by cldr-tools)");
    UOption[] options = {
        UOption.HELP_H(), UOption.HELP_QUESTION_MARK(),
        UOption.SOURCEDIR().setDefault(CldrUtility.MAIN_DIRECTORY),
        outputDir().setDefault("./"),
        restrictLocales(),
        processors().setDefault(DEFAULT_PROCESSORS),
    };
    UOption.parseArgs(args, options);
    String sourceDir = options[2].value; // SOURCEDIR
    String targetDir = options[3].value; // outputDir
    String restrictLocales = options[4].value; // --restrictLocales
    String procNames = options[5].value; // processors

    List<Class<? extends Processor>> processorClasses = new ArrayList<Class<? extends Processor>>();
    for (String procName : procNames.split(",")) {
      if (!procName.contains(".")) {
        procName = Processor.class.getPackage().getName() + "." + procName;
      }
      Throwable thrown = null;
      try {
        Class<?> clazz = Class.forName(procName);
        processorClasses.add(clazz.asSubclass(Processor.class));
      } catch (ClassNotFoundException e) {
        thrown = e;
      } catch (ClassCastException e) {
        thrown = e;
      }
      if (thrown != null) {
        System.err.println("Ignoring " + procName + " (" + thrown + ")");
      }
    }
    Factory cldrFactory = Factory.make(sourceDir, ".*");
    Set<String> locales = cldrFactory.getAvailable();
    if (restrictLocales != null) {
      Set<String> newLocales = new HashSet<String>();
      newLocales.add("root");  // always include root or things break
      for (String locale : restrictLocales.split(",")) {
        if (!locales.contains(locale)) {
          System.err.println("Ignoring non-existent locale " + locale);
          continue;
        }
        newLocales.add(locale);
      }
      locales = newLocales;
    }
    System.out.println("Processing " + locales.size() + " locales");
    File outputDir = new File(targetDir);
    LocaleData localeData = new LocaleData(factory, locales);
    for (Class<? extends Processor> processorClass : processorClasses) {
      Constructor<? extends Processor> ctor =
          processorClass.getConstructor(File.class, Factory.class, LocaleData.class);
      Processor processor = ctor.newInstance(outputDir, cldrFactory, localeData);
      processor.run();
    }
    System.out.println("Finished.");
  }

  private static UOption outputDir() {
    return UOption.create("outdir", 'o', UOption.REQUIRES_ARG);
  }

  private static UOption processors() {
    return UOption.create("processors", 'p', UOption.REQUIRES_ARG);
  }

  private static UOption restrictLocales() {
    return UOption.create("restrictLocales", 'r', UOption.REQUIRES_ARG);
  }
}
