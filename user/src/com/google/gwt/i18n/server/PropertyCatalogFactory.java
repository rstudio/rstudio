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
package com.google.gwt.i18n.server;

import com.google.gwt.i18n.client.Messages.Example;
import com.google.gwt.i18n.client.Messages.PluralCount;
import com.google.gwt.i18n.client.Messages.Select;
import com.google.gwt.i18n.server.MessageFormatUtils.MessageStyle;
import com.google.gwt.i18n.shared.GwtLocale;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Factory for GWT properties file format.
 */
public class PropertyCatalogFactory implements MessageCatalogFactory {

  // @VisibleForTesting
  static final String SELECTOR_BOILERPLATE_1 = "# Lines of the form"
      + " key[form|form]=msg are for alternate forms of";
  static final String SELECTOR_BOILERPLATE_2 = "# the message according to"
      + " Plural Count and Selector entries above.";

  static final String STRINGMAP_BOILERPLATE_1 = "# Lines of the form"
    + " key[entry]=msg are for individual entries, and";
  static final String STRINGMAP_BOILERPLATE_2 = "# the line without [] lists"
    + " the entries, separated by commas.";

  private static class PropertiesWriter extends DefaultVisitor
      implements Writer {

    private static String stringJoin(String joiner, String... values) {
      StringBuilder buf = new StringBuilder();
      boolean needsJoiner = false;
      for (String value : values) {
        if (needsJoiner) {
          buf.append(joiner);
        } else {
          needsJoiner = true;
        }
        buf.append(value);
      }
      return buf.toString();
    }

    private final PrintWriter writer;

    private String baseKey;

    public PropertiesWriter(PrintWriter writer) {
      this.writer = writer;
    }

    public void close() throws IOException {
      writer.close();
    }

    @Override
    public void endMessage(Message msg, MessageTranslation trans) {
      baseKey = null;
    }

    public MessageInterfaceVisitor visitClass() {
      return this;
    }

    @Override
    public MessageVisitor visitMessage(Message msg, MessageTranslation trans) {
      writer.println();
      String description = msg.getDescription();
      if (description != null) {
        writer.println("# Description: " + description);
      }
      String meaning = msg.getMeaning();
      if (meaning != null) {
        writer.println("# Meaning: " + meaning);
      }
      List<Parameter> params = msg.getParameters();
      for (int i = 0; i < params.size(); ++i) {
        Parameter param = params.get(i);
        writer.print("#   " + i + " - " + param.getName());
        if (param.isAnnotationPresent(PluralCount.class)) {
          writer.print(", Plural Count");
        }
        if (param.isAnnotationPresent(Select.class)) {
          writer.print(", Selector");
        }
        if (param.isAnnotationPresent(Example.class)) {
          Example exampleAnnot = param.getAnnotation(Example.class);
          writer.print(", Example: " + exampleAnnot.value());
        }
        writer.println();
      }
      int[] selectorIndices = msg.getSelectorParameterIndices();
      if (selectorIndices.length > 0) {
        if (selectorIndices[0] >= 0) {
          writer.println(SELECTOR_BOILERPLATE_1);
          writer.println(SELECTOR_BOILERPLATE_2);
        } else {
          writer.println(STRINGMAP_BOILERPLATE_1);
          writer.println(STRINGMAP_BOILERPLATE_2);
        }
      }
      baseKey = quoteKey(msg.getKey());
      writer.println(baseKey + "=" + propertiesMessage(msg.getMessageStyle(),
          msg.getDefaultMessage()));
      return this;
    }

    @Override
    public void visitMessageInterface(MessageInterface msgIntf, GwtLocale sourceLocale) {
      writer.println("# Messages from " + msgIntf.getQualifiedName());
      writer.println("# Source locale " + sourceLocale);
    }

    @Override
    public void visitTranslation(String[] formNames, boolean isDefault,
        MessageStyle style, String msg) {
      if (isDefault) {
        // default message is processed in processDefaultMessageBefore
        return;
      }
      if (msg == null) {
        msg = "";
      }
      String key = baseKey;
      key += "[" + stringJoin("|", formNames) + "]";
      writer.println(key + "=" + propertiesMessage(style, msg));
    }

    private String propertiesMessage(MessageStyle style, String msg) {
      if (msg == null) {
        // TODO(jat): is this the right thing to do if no translation was found?
        return "";
      }
      // TODO(jat): translate so property files have consistent quoting rules?
      return quoteValue(msg);
    }

    /**
     * Quote keys for use in a properties file.
     * 
     * In addition to the usual quoting, all spaces are backslash-quoted.
     * 
     * @param str key to quote
     * @return quoted key
     */
    private String quoteKey(String str) {
      str = str.replace("\\", "\\\\");
      str = str.replace(" ", "\\ ");
      return quoteSpecial(str);
    }

    /**
     * Quote strings for use in a properties file.
     * 
     * @param str string to quote
     * @return quoted string
     */
    private String quoteSpecial(String str) {
      return str.replaceAll("([\f\t\n\r$!=:#])", "\\\\$1");
    }

    /**
     * Quote values for use in a properties file.
     * 
     * In addition to the usual quoting, leading spaces are backslash-quoted.
     * 
     * @param str value to quote
     * @return quoted value
     */
    private String quoteValue(String str) {
      str = str.replace("\\", "\\\\");
      if (str.startsWith(" ")) {
        int n = 0;
        while (n < str.length() && str.charAt(n) == ' ') {
          n++;
        }
        str = str.substring(n);
        while (n-- > 0) {
          str = "\\ " + str;
        }
      }
      return quoteSpecial(str);
    }
  }

  public String getExtension() {
    return ".properties";
  }

  public Writer getWriter(Context context,
      String fileName) {
    PrintWriter pw = context.createTextFile(fileName, "UTF-8");
    if (pw == null) {
      return null;
    }
    PropertiesWriter writer = new PropertiesWriter(pw);
    return writer;
  }
}
