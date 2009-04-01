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
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.i18n.rebind.AbstractResource.ResourceList;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.user.rebind.AbstractMethodCreator;
import com.google.gwt.user.rebind.SourceWriter;

import java.util.HashMap;
import java.util.Map;

class ConstantsWithLookupImplCreator extends ConstantsImplCreator {
  final JMethod[] allInterfaceMethods;

  private final Map<String, AbstractMethodCreator> namesToMethodCreators = new HashMap<String, AbstractMethodCreator>();

  /**
   * Constructor for <code>ConstantsWithLookupImplCreator</code>.
   * 
   * @param logger logger to print errors
   * @param writer <code>Writer</code> to print to
   * @param localizableClass class/interface to conform to
   * @param resourceList resource bundle used to generate the class
   * @param oracle types
   * @throws UnableToCompleteException
   */
  ConstantsWithLookupImplCreator(TreeLogger logger, SourceWriter writer,
      JClassType localizableClass, ResourceList resourceList, TypeOracle oracle)
      throws UnableToCompleteException {
    super(logger, writer, localizableClass, resourceList, oracle);
    try {

      // Boolean
      JType booleanType = oracle.parse(boolean.class.getName());
      LookupMethodCreator booleanMethod = new LookupMethodCreator(this,
          booleanType) {
        @Override
        public void printReturnTarget() {
          println("return target.booleanValue();");
        }

        @Override
        public String returnTemplate() {
          return "boolean answer = {0}();\ncache.put(\"{0}\",new Boolean(answer));\nreturn answer;";
        }
      };
      namesToMethodCreators.put("getBoolean", booleanMethod);

      // Double
      JType doubleType = oracle.parse(double.class.getName());
      LookupMethodCreator doubleMethod = new LookupMethodCreator(this,
          doubleType) {
        @Override
        public void printReturnTarget() {
          println("return target.doubleValue();");
        }

        @Override
        public String returnTemplate() {
          return "double answer = {0}();\ncache.put(\"{0}\",new Double(answer));\nreturn answer;";
        }
      };
      namesToMethodCreators.put("getDouble", doubleMethod);

      // Int
      JType intType = oracle.parse(int.class.getName());
      LookupMethodCreator intMethod = new LookupMethodCreator(this, intType) {
        @Override
        public void printReturnTarget() {
          println("return target.intValue();");
        }

        @Override
        public String returnTemplate() {
          return "int answer = {0}();\ncache.put(\"{0}\",new Integer(answer));\nreturn answer;";
        }
      };

      namesToMethodCreators.put("getInt", intMethod);

      // Float
      JType floatType = oracle.parse(float.class.getName());
      LookupMethodCreator floatMethod = new LookupMethodCreator(this, floatType) {
        @Override
        public String returnTemplate() {
          String val = "float answer = {0}();\ncache.put(\"{0}\", new Float(answer));\nreturn answer;";
          return val;
        }

        @Override
        protected void printReturnTarget() {
          println("return target.floatValue();");
        }
      };
      namesToMethodCreators.put("getFloat", floatMethod);

      // Map - use erased type for matching
      JType mapType = oracle.parse(Map.class.getName()).getErasedType();
      namesToMethodCreators.put("getMap",
          new LookupMethodCreator(this, mapType) {
            @Override
            public String getReturnTypeName() {
              return ConstantsMapMethodCreator.GENERIC_STRING_MAP_TYPE;
            }
          });

      // String
      JType stringType = oracle.parse(String.class.getName());
      LookupMethodCreator stringMethod = new LookupMethodCreator(this,
          stringType) {
        @Override
        public String returnTemplate() {
          return "String answer = {0}();\ncache.put(\"{0}\",answer);\nreturn answer;";
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
  @Override
  protected void emitMethodBody(TreeLogger logger, JMethod method,
      GwtLocale locale) throws UnableToCompleteException {
    checkMethod(logger, method);
    if (method.getParameters().length == 1) {
      String name = method.getName();
      AbstractMethodCreator c = namesToMethodCreators.get(name);
      if (c != null) {
        c.createMethodFor(logger, method, name, null, locale);
        return;
      }
    }
    // fall through
    super.emitMethodBody(logger, method, locale);
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
      // user may have specified a method named getInt/etc with no parameters
      // this isn't a conflict, so treat them like any other Constant methods
      if (params.length == 0) {
        checkConstantMethod(logger, method);
      } else {
        if (params.length != 1
            || !params[0].getType().getQualifiedSourceName().equals(
                "java.lang.String")) {
          throw error(logger, method + " must have a single String argument.");
        }
        checkReturnType(logger, method);
      }
    } else {
      checkConstantMethod(logger, method);
    }
  }
}
