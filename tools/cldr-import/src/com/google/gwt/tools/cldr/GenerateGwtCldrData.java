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
import org.unicode.cldr.util.CLDRFile.Factory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Generate a country list for each locale, taking into account the literate
 * population of each country speaking the language.
 */
@SuppressWarnings("unchecked")
public class GenerateGwtCldrData {

  private static final GwtLocaleFactory factory = new GwtLocaleFactoryImpl();

  private static boolean DEBUG = false;

  private static final Class<? extends Processor>[] PROCESSORS;
  
  static {
    PROCESSORS = new Class[] {
        CurrencyDataProcessor.class,
        DateTimeFormatInfoProcessor.class,
        ListFormattingProcessor.class,
    };
  }

  public static void main(String[] args) throws IOException, SecurityException,
      NoSuchMethodException, IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException {
    System.out.println("Starting to generate from CLDR data (ignore -D lines "
        + "produced by cldr-tools)");
    UOption[] options = {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.SOURCEDIR().setDefault(CldrUtility.MAIN_DIRECTORY), // C:\cvsdata/unicode\cldr\diff\summary
        outputDir().setDefault("./"),
        restrictLocales(),
      };
    UOption.parseArgs(args, options);
    String sourceDir = options[2].value; // SOURCEDIR
    String targetDir = options[3].value; // outputDir
    DEBUG = options[4].doesOccur; // --restrictLocales
    Factory cldrFactory = Factory.make(sourceDir, ".*");
    Set<String> locales = cldrFactory.getAvailable();
    if (DEBUG) {
      locales = new HashSet<String>(Arrays.asList(new String[] {
          "root", "en", "en_US", "en_CA", "ar", "ar_IQ"}));
    }
    System.out.println("Processing " + locales.size() + " locales");
    File outputDir = new File(targetDir);
    LocaleData localeData = new LocaleData(factory, locales);
    for (Class<? extends Processor> processorClass : PROCESSORS) {
      Constructor<? extends Processor> ctor = processorClass.getConstructor(
          File.class, Factory.class, LocaleData.class);
      Processor processor = ctor.newInstance(outputDir, cldrFactory,
          localeData);
      processor.run();
    }
    System.out.println("Finished.");
  }

  private static UOption outputDir() {
    return UOption.create("outdir", 'o', UOption.REQUIRES_ARG);
  }

  private static UOption restrictLocales() {
    return UOption.create("restrictLocales", 'r', UOption.NO_ARG);
  }
}
