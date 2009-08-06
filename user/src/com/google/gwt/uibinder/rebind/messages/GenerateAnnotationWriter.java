/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.uibinder.rebind.messages;

import com.google.gwt.uibinder.rebind.IndentedWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a {@literal @}Generate annotation in a Messages interface,
 * and can write it out at code gen time.
 */
class GenerateAnnotationWriter {
  private static String toArgsList(List<String> strings) {
    StringBuilder b = new StringBuilder();
    boolean first = true;
    for (String s : strings) {
      if (first)  {
        first = false;
      } else {
        b.append(",\n  ");
      }
      b.append(s);
    }
    return b.toString();
  }

  private static String toArrayLiteral(String[] strings) {
    StringBuilder b = new StringBuilder("{");
    for (String s : strings) {
      b.append(String.format("\"%s\", ", s));
    }
    b.append('}');
    return b.toString();
  }

  private final String[] formats;
  private final String fileName;
  private final String[] locales;

  public GenerateAnnotationWriter(String[] formats, String fileName,
      String[] locales) {
    this.formats = formats;
    this.fileName = fileName;
    this.locales = locales;
  }

  public void write(IndentedWriter w) {
    boolean hasFormats = formats.length > 0;
    boolean hasFileName = fileName.length() > 0;
    boolean hasLocales = locales.length > 0;

    if (hasFormats || hasFileName || hasLocales) {
      List<String> args = new ArrayList<String>();
      if (hasFormats) {
        args.add(String.format("format = %s", toArrayLiteral(formats)));
      }
      if (hasFileName) {
        args.add(String.format("fileName = \"%s\"", fileName));
      }
      if (hasLocales) {
        args.add(String.format("locales = %s", toArrayLiteral(locales)));
      }

      w.write("@Generate(");
      w.indent();
      w.write(toArgsList(args));
      w.outdent();
      w.write(")");
    }
  }
}
