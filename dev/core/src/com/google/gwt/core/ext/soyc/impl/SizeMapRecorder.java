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
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.js.SizeBreakdown;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.util.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;
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
    public final String type;
    public final String description;

    public TypedProgramReference(String type, String description) {
      this.type = type;
      this.description = description;
    }
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
  public static void escapeXml(String code, int start, int end,
      boolean quoteApostrophe, StringBuilder builder) {
    int lastIndex = 0;
    int len = end - start;
    char[] c = new char[len];

    code.getChars(start, end, c, 0);
    for (int i = 0; i < len; i++) {
      if ((c[i] >= '\uD800') && (c[i] <= '\uDBFF')) {
        builder.append(c, lastIndex, i - lastIndex);
        builder.append("(non-valid utf-8 character)");
        lastIndex = i + 1;
        break;
      } else if ((c[i] >= '\uDC00') && (c[i] <= '\uDFFF')) {
        builder.append(c, lastIndex, i - lastIndex);
        builder.append("(non-valid utf-8 character)");
        lastIndex = i + 1;
        break;
      } else if (c[i] == '\0') {
        builder.append(c, lastIndex, i - lastIndex);
        builder.append("(null)");
        lastIndex = i + 1;
        break;
      } else if (c[i] == '\uffff') {
        builder.append(c, lastIndex, i - lastIndex);
        builder.append("(uffff)");
        lastIndex = i + 1;
        break;
      } else if (c[i] == '\ufffe') {
        builder.append(c, lastIndex, i - lastIndex);
        builder.append("(ufffe)");
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
      }
    }
    builder.append(c, lastIndex, len - lastIndex);
  }
  
  public static void recordMap(TreeLogger logger, OutputStream out,
      SizeBreakdown[] sizeBreakdowns, JavaToJavaScriptMap jjsmap,
      Map<JsName, String> obfuscateMap) throws IOException {
    out = new GZIPOutputStream(out);
    Writer writer = new OutputStreamWriter(out, Util.DEFAULT_ENCODING);

    writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    writer.append("<sizemaps>\n");

    for (int i = 0; i < sizeBreakdowns.length; i++) {
      writer.append("<sizemap fragment=\"" + i + "\" " + "size=\""
          + sizeBreakdowns[i].getSize() + "\">\n");
      for (Entry<JsName, Integer> sizeMapEntry : sizeBreakdowns[i].getSizeMap().entrySet()) {
        JsName name = sizeMapEntry.getKey();
        int size = sizeMapEntry.getValue();
        TypedProgramReference typedRef = typedProgramReference(name, jjsmap,
            obfuscateMap);
        writer.append("  <size " + "type=\"" + escapeXml(typedRef.type)
            + "\" " + "ref=\"" + escapeXml(typedRef.description) + "\" "
            + "size=\"" + size + "\"/>\n");
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

    JReferenceType type = jjsmap.nameToType(name);
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
