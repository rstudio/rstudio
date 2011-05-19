/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.core.ext.soyc.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.js.SizeBreakdown;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.util.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

/**
 * Records an array of {@link SizeBreakdown} to a gzipped XML file. That file is
 * then read to produce a Story of Your Compile.
 */
public class SizeMapRecorder {

  /**
   * A human-accessible type and description of a program reference. These are
   * produced by
   * {@link SizeMapRecorder#typedProgramReference(JsName, JavaToJavaScriptMap)}
   * and used by
   * {@link SizeMapRecorder#recordMap(TreeLogger, OutputStream, SizeBreakdown[], JavaToJavaScriptMap)}
   * .
   */
  private static class TypedProgramReference {
    public final String description;
    public final String type;

    public TypedProgramReference(String type, String description) {
      this.type = type;
      this.description = description;
    }
  }

  /**
   * Sorts by JsName.getIdent().
   */
  private static final Comparator<JsName> JSNAME_SORT = new Comparator<JsName>() {
    public int compare(JsName o1, JsName o2) {
      return o1.getIdent().compareTo(o2.getIdent());
    }
  };

  /**
   * Returns the hexadecimal representation of a character.
   */
  public static StringBuilder charToHex(char c) {
    char hexDigit[] =
        {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    StringBuilder toReturn = new StringBuilder();
    byte charByte = (byte) (c >>> 8);
    toReturn.append(hexDigit[(charByte >> 4) & 0x0F]);
    toReturn.append(hexDigit[charByte & 0x0F]);
    charByte = (byte) (c & 0xFF);
    toReturn.append(hexDigit[(charByte >> 4) & 0x0F]);
    toReturn.append(hexDigit[charByte & 0x0F]);
    return toReturn;
  }

/**
   * Escapes '&', '<', '>', '"', and '\'' to their XML entity equivalents.
   */
  public static String escapeXml(String unescaped) {
    StringBuilder builder = new StringBuilder();
    escapeXml(unescaped, 0, unescaped.length(), true, builder);
    return builder.toString();
  }

/**
   * Escapes '&', '<', '>', '"', and optionally ''' to their XML entity
   * equivalents. The portion of the input string between start (inclusive) and
   * end (exclusive) is scanned.  The output is appended to the given
   * StringBuilder.
   * 
   * @param code the input String
   * @param start the first character position to scan.
   * @param end the character position following the last character to scan.
   * @param quoteApostrophe if true, the &apos; character is quoted as
   *     &amp;apos;
   * @param builder a StringBuilder to be appended with the output.
   */
  public static void escapeXml(String code, int start, int end, boolean quoteApostrophe,
      StringBuilder builder) {
    // See http://www.w3.org/TR/2006/REC-xml11-20060816/#charsets.
    int lastIndex = 0;
    int len = end - start;
    char[] c = new char[len];

    code.getChars(start, end, c, 0);
    for (int i = 0; i < len; i++) {
      if ((c[i] < '\u0020')) {
        builder.append(c, lastIndex, i - lastIndex);
        if (c[i] == '\u0000') {
          builder.append("\\0");
        } else if (c[i] == '\u0009') {
          builder.append("\\t");
        } else if (c[i] == '\n') {
          builder.append("\\n");
        } else if (c[i] == '\r') {
          builder.append("\\r");
        } else {
          builder.append("(invalid xml character: \\u" + charToHex(c[i]) + ")");
        }
        lastIndex = i + 1;
      } else if (((c[i] >= '\u007F') && (c[i] <= '\u0084'))
          || ((c[i] >= '\u0086') && (c[i] <= '\u009F'))
          || ((c[i] >= '\uD800') && (c[i] <= '\uDBFF'))
          || ((c[i] >= '\uDC00') && (c[i] <= '\uDFFF'))
          || ((c[i] >= '\uFDD0') && (c[i] <= '\uFDDF')) || (c[i] == '\u00A0') || (c[i] == '\uFFFF')
          || (c[i] == '\uFFFE')) {
        builder.append(c, lastIndex, i - lastIndex);
        builder.append("(invalid xml character: \\u" + charToHex(c[i]) + ")");
        lastIndex = i + 1;
      } else if (c[i] == '&') {
        builder.append(c, lastIndex, i - lastIndex);
        builder.append("&amp;");
        lastIndex = i + 1;
      } else if (c[i] == '>') {
        builder.append(c, lastIndex, i - lastIndex);
        builder.append("&gt;");
        lastIndex = i + 1;
      } else if (c[i] == '<') {
        builder.append(c, lastIndex, i - lastIndex);
        builder.append("&lt;");
        lastIndex = i + 1;
      } else if (c[i] == '\"') {
        builder.append(c, lastIndex, i - lastIndex);
        builder.append("&quot;");
        lastIndex = i + 1;
      } else if (c[i] == '\'') {
        if (quoteApostrophe) {
          builder.append(c, lastIndex, i - lastIndex);
          builder.append("&apos;");
          lastIndex = i + 1;
        }
      } else if (c[i] > '\u007F') {
        builder.append(c, lastIndex, i - lastIndex);
        builder.append("&#x" + charToHex(c[i]) + ";");
        lastIndex = i + 1;
      }
    }
    builder.append(c, lastIndex, len - lastIndex);
  }

  /**
   * @param logger a TreeLogger
   */
  public static void recordMap(TreeLogger logger, OutputStream out, SizeBreakdown[] sizeBreakdowns,
      JavaToJavaScriptMap jjsmap, Map<JsName, String> obfuscateMap) throws IOException {
    out = new GZIPOutputStream(out);
    Writer writer = new OutputStreamWriter(out, Util.DEFAULT_ENCODING);

    writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    writer.append("<sizemaps>\n");

    for (int i = 0; i < sizeBreakdowns.length; i++) {
      writer.append("<sizemap fragment=\"" + i + "\" " + "size=\"" + sizeBreakdowns[i].getSize()
          + "\">\n");
      Map<JsName, Integer> sizeMap = new TreeMap<JsName, Integer>(JSNAME_SORT);
      sizeMap.putAll(sizeBreakdowns[i].getSizeMap());
      for (Entry<JsName, Integer> sizeMapEntry : sizeMap.entrySet()) {
        JsName name = sizeMapEntry.getKey();
        int size = sizeMapEntry.getValue();
        TypedProgramReference typedRef = typedProgramReference(name, jjsmap, obfuscateMap);
        writer.append("  <size " + "type=\"" + escapeXml(typedRef.type) + "\" " + "ref=\""
            + escapeXml(typedRef.description) + "\" " + "size=\"" + size + "\"/>\n");
      }
      writer.append("</sizemap>\n");
    }

    writer.append("</sizemaps>");
    writer.close();
  }

  private static TypedProgramReference typedProgramReference(JsName name,
      JavaToJavaScriptMap jjsmap, Map<JsName, String> obfuscateMap) {
    JMethod method = jjsmap.nameToMethod(name);
    if (method != null) {
      StringBuilder sb = new StringBuilder();
      sb.append(method.getEnclosingType().getName());
      sb.append("::");
      sb.append(method.getName());
      sb.append("(");
      for (JType type : method.getOriginalParamTypes()) {
        sb.append(type.getJsniSignatureName());
      }
      sb.append(")");
      sb.append(method.getOriginalReturnType().getJsniSignatureName());
      String desc = sb.toString();
      return new TypedProgramReference("method", desc);
    }

    JField field = jjsmap.nameToField(name);
    if ((field != null) && (field.getEnclosingType() != null)) {
      StringBuilder sb = new StringBuilder();
      sb.append(field.getEnclosingType().getName());
      sb.append("::");
      sb.append(field.getName());
      return new TypedProgramReference("field", sb.toString());
    }

    JClassType type = jjsmap.nameToType(name);
    if (type != null) {
      return new TypedProgramReference("type", type.getName());
    }

    String string = obfuscateMap.get(name);
    if (string != null) {
      return new TypedProgramReference("string", string);
    }

    return new TypedProgramReference("var", name.getShortIdent());
  }
}
