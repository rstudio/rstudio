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
package com.google.gwt.dev;

import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

import java.io.PrintStream;

class SignatureDumper {

  public interface Filter {
    boolean shouldPrint(JAbstractMethod method);

    boolean shouldPrint(JClassType type);

    boolean shouldPrint(JField field);
  }

  public static void dumpSignatures(TypeOracle typeOracle, PrintStream out,
      Filter filter) {
    out.println("# Contains all signatures dumped from the GWT compiler");
    out.println("FileVersion 1");
    out.println("GwtVersion " + About.getGwtVersionNum());
    out.print(dumpAllSignatures(typeOracle, filter));
    out.close();
  }

  private static void addMethods(JAbstractMethod[] methods,
      StringBuilder result, Filter filter) {
    for (JAbstractMethod currentMeth : methods) {
      if (!filter.shouldPrint(currentMeth)) {
        continue;
      }
      if (currentMeth.isConstructor() != null) {
        result.append(" method <init>");
      } else if (currentMeth.isMethod() != null) {
        result.append(" method ");
        if (currentMeth.isMethod().isStatic()) {
          result.append("static ");
        }
        result.append(currentMeth.getName());
      } else {
        continue;
      }
      result.append(" (");
      for (JParameter currentParam : currentMeth.getParameters()) {
        result.append(currentParam.getType().getJNISignature());
      }
      result.append(')');
      if (currentMeth.isConstructor() != null) {
        result.append('V');
      } else {
        result.append(((JMethod) currentMeth).getReturnType().getJNISignature());
      }
      result.append('\n');
    }
  }

  /**
   * Dumps the signatures within this typeOracle. Singatures may appear multiple
   * times.
   */
  private static String dumpAllSignatures(TypeOracle typeOracle, Filter filter) {
    StringBuilder result = new StringBuilder();
    for (JClassType current : typeOracle.getTypes()) {
      if (!filter.shouldPrint(current)) {
        continue;
      }
      if (current.isInterface() != null) {
        result.append("interface ");
      } else {
        result.append("class ");
      }
      result.append(current.getJNISignature());
      if (current.getSuperclass() != null) {
        result.append(" extends ");
        result.append(current.getSuperclass().getJNISignature());
      }
      for (JClassType currentInterface : current.getImplementedInterfaces()) {
        result.append(" implements ");
        result.append(currentInterface.getJNISignature());
      }
      result.append('\n');

      result.append(" method static <clinit> ()V\n");
      JConstructor[] constructors = current.getConstructors();
      if (constructors.length == 0) {
        result.append(" method <init> ()V\n");
      } else {
        addMethods(constructors, result, filter);
      }
      addMethods(current.getMethods(), result, filter);
      for (JField currentField : current.getFields()) {
        if (!filter.shouldPrint(currentField)) {
          continue;
        }
        result.append(" field ");
        if (currentField.isStatic()) {
          result.append("static ");
        }
        result.append(currentField.getName());
        result.append(' ');
        result.append(currentField.getType().getJNISignature());
        result.append('\n');
      }
    }
    return result.toString();
  }

}
