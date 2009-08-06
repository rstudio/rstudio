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
 * Represents a method in a Messages interface. Can write both the method
 * declaration and its invocation.
 */
public class MessageWriter {
  /**
   * Escapes ' and { chars, which have special meaning to Messages
   * interfaces.
   */
  public static String escapeMessageFormat(String messageFormatStyleText) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < messageFormatStyleText.length(); i++) {
      char c = messageFormatStyleText.charAt(i);
      if (c == '\'') {
        b.append("''");
      } else if (c == '{') {
        b.append("'{'");
      } else {
        b.append(c);
      }
    }
    return b.toString();
  }
  private String defaultMessage;
  private final String description;
  private final String key;
  private final String name;
  private final List<PlaceholderWriter> placeholders =
    new ArrayList<PlaceholderWriter>();

  private final String meaning;

  MessageWriter(String description, String key, String meaning,
      String name) {
    this.description = description;
    this.key = key;
    this.meaning = meaning;
    this.name = name;
  }

  public void addPlaceholder(PlaceholderWriter placeholder) {
    this.placeholders.add(placeholder);
  }

  public String getInvocation() {
    StringBuilder b = new StringBuilder(String.format("%s(", name));
    int countdown = placeholders.size();
    for (PlaceholderWriter ph : placeholders) {
      b.append(ph.getValue());
      if (--countdown > 0) {
        b.append(",");
      }
    }
    b.append(")");
    return b.toString();
  }

  public int getPlaceholderCount() {
    return placeholders.size();
  }

  public void setDefaultMessage(String defaultMessage) {
    this.defaultMessage = defaultMessage;
  }

  public void writeDeclaration(IndentedWriter pw) {
    pw.write("@DefaultMessage(\"%s\")", defaultMessage);
    if (description.length() > 0) {
      pw.write("@Description(\"%s\")", description);
    }
    if (key.length() > 0) {
      pw.write("@Key(\"%s\")", key);
    }
    if (meaning.length() > 0) {
      pw.write("@Meaning(\"%s\")", meaning);
    }
    if (placeholders.isEmpty()) {
      pw.write("String %s();", name);
    } else {
      pw.write("String %s(", name);
      pw.indent();

      int countdown = placeholders.size();
      for (PlaceholderWriter ph : placeholders) {
        String comma = --countdown > 0 ? "," : "";
        pw.write(ph.getDeclaration() + comma);
      }
      pw.write(");");
      pw.outdent();
    }
    pw.newline();
  }
}
