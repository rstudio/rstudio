/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.i18n.rebind.util.AbstractResource;
import com.google.gwt.user.rebind.AbstractMethodCreator;
import com.google.gwt.user.rebind.SourceWriter;

import java.util.HashMap;
import java.util.Map;

class ConstantsWithLookupImplCreator extends ConstantsImplCreator {
  final JMethod[] allInterfaceMethods;

  private final Map namesToMethodCreators = new HashMap();

  ConstantsWithLookupImplCreator(TreeLogger logger, SourceWriter writer,
      JClassType localizableClass, AbstractResource messageBindings,
      TypeOracle oracle) throws UnableToCompleteException {
    super(logger, writer, localizableClass, messageBindings, oracle);
    try {

      // Boolean
      JType booleanType = oracle.parse(boolean.class.getName());
      LookupMethodCreator booleanMethod = new LookupMethodCreator(this,
          booleanType) {
        public void printReturnTarget() {
          println("return target.booleanValue();");
        }

        public String returnTemplate() {
          return "boolean answer = {0}();\n cache.put(\"{0}\",new Boolean(answer));return answer;";
        }
      };
      namesToMethodCreators.put("getBoolean", booleanMethod);

      // Double
      JType doubleType = oracle.parse(double.class.getName());
      LookupMethodCreator doubleMethod = new LookupMethodCreator(this,
          doubleType) {
        public void printReturnTarget() {
          println("return target.doubleValue();");
        }

        public String returnTemplate() {
          return "double answer = {0}();\n cache.put(\"{0}\",new Double(answer));return answer;";
        }
      };
      namesToMethodCreators.put("getDouble", doubleMethod);

      // Int
      JType intType = oracle.parse(int.class.getName());
      LookupMethodCreator intMethod = new LookupMethodCreator(this, intType) {
        public void printReturnTarget() {
          println("return target.intValue();");
        }

        public String returnTemplate() {
          return "int answer = {0}();\n cache.put(\"{0}\",new Integer(answer));return answer;";
        }
      };

      namesToMethodCreators.put("getInt", intMethod);

      // Float
      JType floatType = oracle.parse(float.class.getName());
      LookupMethodCreator floatMethod = new LookupMethodCreator(this, floatType) {
        public String returnTemplate() {
          String val = "float v ={0}(); cache.put(\"{0}\", new Float(v));return v;";
          return val;
        }

        protected void printReturnTarget() {
          println("return target.floatValue();");
        }
      };
      namesToMethodCreators.put("getFloat", floatMethod);

      // Map
      JType mapType = oracle.parse(Map.class.getName());
      namesToMethodCreators.put("getMap",
          new LookupMethodCreator(this, mapType));

      // String
      JType stringType = oracle.parse(String.class.getName());
      LookupMethodCreator stringMethod = new LookupMethodCreator(this,
          stringType) {
        public String returnTemplate() {
          return "String answer = {0}();\n cache.put(\"{0}\",answer);return answer;";
        }
      };
      namesToMethodCreators.put("getString", stringMethod);

      // String Array
      JType stringArray = oracle.getArrayType(stringType);
      namesToMethodCreators.put("getStringArray", new LookupMethodCreator(this,
          stringArray));

      setNeedCache(true);
      allInterfaceMethods = getAllInterfaceMethods(localizableClass);
    } catch (TypeOracleException e) {
      throw error(logger, e);
    }
  }

  /**
   * Create the method body associated with the given method. Arguments are
   * arg0...argN.
   */
  protected void emitMethodBody(TreeLogger logger, JMethod method)
      throws UnableToCompleteException {
    checkMethod(logger, method);
    if (method.getParameters().length == 1) {
      String name = method.getName();
      AbstractMethodCreator c = (AbstractMethodCreator) namesToMethodCreators.get(name);
      if (c != null) {
        c.createMethodFor(logger, method, null);
        return;
      }
    }
    // fall through
    super.emitMethodBody(logger, method);
  }

  /**
   * Checks that the method has the right structure to implement
   * <code>Constant</code>.
   * 
   * @param method method to check
   */
  private void checkMethod(TreeLogger logger, JMethod method)
      throws UnableToCompleteException {
    if (namesToMethodCreators.get(method.getName()) != null) {
      JParameter[] params = method.getParameters();
      // getString() might be returning a String argument, so leave it alone.
      if (params.length == 0) {
        return;
      }
      if (params.length != 1
          || !params[0].getType().getQualifiedSourceName().equals(
              "java.lang.String")) {
        String s = method + " must have a single String argument.";
        throw error(logger, s);
      }
    } else {
      if (method.getParameters().length > 0) {
        throw error(
            logger,
            "User-defined methods in interfaces extending ConstantsWithLookup must have no parameters and a return type of int, String, String[], ...");
      }
    }
  }
}
