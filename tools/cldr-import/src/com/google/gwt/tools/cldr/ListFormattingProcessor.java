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

import com.google.gwt.i18n.shared.GwtLocale;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

/**
 * Extract list formatting information from CLDR data.
 */
public class ListFormattingProcessor extends Processor {

  public ListFormattingProcessor(File outputDir, InputFactory cldrFactory, LocaleData localeData) {
    super(outputDir, cldrFactory, localeData);
  }

  @Override
  protected void cleanupData() {
    localeData.removeCompleteDuplicates("list");
  }

  @Override
  protected void loadData() throws IOException {
    System.out.println("Loading data for list formatting");
    localeData.addVersions(cldrFactory);
    localeData.addEntries("list", cldrFactory, "//ldml/listPatterns/listPattern/",
        "listPatternPart", "type");
  }

  @Override
  protected void writeOutputFiles() throws IOException {
    Set<GwtLocale> localesToPrint = localeData.getNonEmptyLocales("list");
    String path = "rebind/cldr/ListPatterns";

    writeVersionFile(path + ".versions.txt", localesToPrint);

    for (GwtLocale locale : localesToPrint) {
      PrintWriter pw = null;

      for (String key : localeData.getKeys("list", locale)) {
        if (pw == null) {
          pw = createOutputFile(path + "_" + locale.getAsString() + ".properties");
          printPropertiesHeader(pw);
          pw.println();
          printVersion(pw, locale, "# ");
        }
        pw.println(key + "=" + localeData.getEntry("list", locale, key));
      }
      if (pw != null) {
        pw.close();
      }
    }
  }
}
