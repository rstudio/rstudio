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
        writer.append("  <size " + "type=\"" + Util.escapeXml(typedRef.type)
            + "\" " + "ref=\"" + Util.escapeXml(typedRef.description) + "\" "
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
