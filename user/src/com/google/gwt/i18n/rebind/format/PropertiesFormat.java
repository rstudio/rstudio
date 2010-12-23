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
package com.google.gwt.i18n.rebind.format;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.i18n.client.PluralRule.PluralForm;
import com.google.gwt.i18n.rebind.AnnotationsResource;
import com.google.gwt.i18n.rebind.AbstractResource.ResourceList;
import com.google.gwt.i18n.rebind.AnnotationsResource.ArgumentInfo;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;

/**
 * Writes GWT-style Java properties files for translation.  This catalog
 * format does not support aggregation of messages from multiple interfaces
 * since there is no way to distinguish messages from another interface from
 * those that were from this interface but no longer used.  The output file
 * is assumed to be in UTF-8 encoding rather than using the {@code \\uXXXX}
 * escapes.
 */
public class PropertiesFormat implements MessageCatalogFormat {

  public String getExtension() {
    return ".properties";
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.i18n.rebind.format.MessageCatalogFormat#write(com.google.gwt.i18n.rebind.util.AbstractResource,
   *      java.io.File, com.google.gwt.core.ext.typeinfo.JClassType)
   */
  public void write(TreeLogger logger, String locale,
      ResourceList resourceList, PrintWriter out, JClassType messageInterface) {
    writeComment(out, "Generated from "
        + messageInterface.getQualifiedSourceName());
    if (locale != null) {
      writeComment(out, "for locale " + locale);
    }
    // Sort keys for deterministic output.
    Set<String> keySet = resourceList.keySet();
    String[] sortedKeys = keySet.toArray(new String[keySet.size()]);
    Arrays.sort(sortedKeys);
    for (String key : sortedKeys) {
      out.println();
      AnnotationsResource annotResource = resourceList.getAnnotationsResource(
          logger, key);
      if (annotResource != null) {
        // Write comments from the annotations.
        writeAnnotComments(out, annotResource, key);
      }
      
      // Collect plural forms for this locale.
      PluralForm[] pluralForms = resourceList.getPluralForms(key);
      if (pluralForms != null) {
        for (PluralForm form : pluralForms) {
          String name = form.getName();
          if ("other".equals(name)) {
            // write the "other" description here, and the default message
            writeComment(out, "- " + form.getDescription());
            write(out, key, resourceList.getString(key));
          } else {
            String comment = "- plural form '" + form.getName() + "': "
                + form.getDescription();
            if (!form.getWarnIfMissing()) {
              comment += " (optional)";
            }
            writeComment(out, comment);
            String translated = resourceList.getStringExt(key,
                form.getName());
            if (translated == null) {
              translated = "";
            }
            write(out, key + "[" + form.getName() + "]", translated);
          }
        }
      } else {
        write(out, key, resourceList.getString(key));
      }
    }
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
  
  /**
   * Write a key-value pair to a properties file with proper quoting.
   * 
   * @param out PrintWriter to output to
   * @param key property key
   * @param value property value
   */
  private void write(PrintWriter out, String key, String value) {
    out.print(quoteKey(key));
    out.print('=');
    out.println(quoteValue(value));
  }
  
  /**
   * Write comments before a line, pulled from the annotations on a
   * given method.
   * 
   * @param out PrintWriter stream to write to
   * @param annotResource AnnotationsResource to get annotation data from
   * @param key key of method for lookup in annotResource
   */
  private void writeAnnotComments(PrintWriter out, AnnotationsResource annotResource, String key) {
    String desc = annotResource.getDescription(key);
    if (desc != null) {
      writeComment(out, "Description: " + desc);
    }
    String meaning = annotResource.getMeaning(key);
    if (meaning != null) {
      writeComment(out, "Meaning: " + meaning);
    }
    Iterable<ArgumentInfo> arguments = annotResource.argumentsIterator(key);
    StringBuffer buf = new StringBuffer();
    if (arguments != null) {
      int i = 0;
      for (ArgumentInfo argInfo : arguments) {
        if (i > 0) {
          buf.append(", ");
        }
        buf.append(i++ + "=");
        buf.append(argInfo.name);
        boolean inParen = false;
        if (argInfo.optional) {
          buf.append(" (Optional");
          inParen = true;
        }
        if (argInfo.isSelect) {
          if (inParen) {
            buf.append("; ");
          } else {
            buf.append(" (");
            inParen = true;
          }
          buf.append("Selector");
        }
        if (argInfo.isPluralCount) {
          if (inParen) {
            buf.append("; ");
          } else {
            buf.append(" (");
            inParen = true;
          }
          buf.append("Plural Count");
        }
        if (argInfo.example != null) {
          if (inParen) {
            buf.append("; ");
          } else {
            buf.append(" (");
            inParen = true;
          }
          buf.append("Example: " + argInfo.example);
        }
        if (inParen) {
          buf.append(')');
        }
      }
      if (i > 0) {
        writeComment(out, buf.toString());
      }
    }
  }

  /**
   * Write a comment to a properties file.
   * 
   * @param out PrintWriter to output to
   * @param comment comment to write
   */
  private void writeComment(PrintWriter out, String comment) {
    out.println("# " + comment);
  }
}
