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
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.user.rebind.SourceWriter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class ConstantsWithLookupImplCreator extends ConstantsImplCreator {

  /**
   * Used partition size if no one is specified.
   * 
   * Used in constructor without a partition size.
   */
  private static final int DEFAULT_PARTITIONS_SIZE = 500;
  
  final JMethod[] allInterfaceMethods;

  private final Map<String, LookupMethodCreator> namesToMethodCreators = new HashMap<>();
  
  private final Map<JMethod, List<List<JMethod>>> neededPartitionLookups = new HashMap<>();

  private final int partitionsSize;

  /**
   * Constructor for <code>ConstantsWithLookupImplCreator</code>. The default partition size of
   * {@value #DEFAULT_PARTITIONS_SIZE} is used.
   * 
   * @param logger logger to print errors
   * @param writer <code>Writer</code> to print to
   * @param localizableClass class/interface to conform to
   * @param resourceList resource bundle used to generate the class
   * @param oracle types
   * @throws UnableToCompleteException
   * 
   * @see LookupMethodCreator#DEFAULT_PARTITIONS_SIZE
   */
  ConstantsWithLookupImplCreator(TreeLogger logger, SourceWriter writer,
      JClassType localizableClass, ResourceList resourceList, TypeOracle oracle)
      throws UnableToCompleteException {
    this(logger, writer, localizableClass, resourceList, oracle, DEFAULT_PARTITIONS_SIZE);
  }
  
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
      JClassType localizableClass, ResourceList resourceList, TypeOracle oracle, 
      int partitionsSize) throws UnableToCompleteException {
    super(logger, writer, localizableClass, resourceList, oracle);
    this.partitionsSize = partitionsSize;
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
          return "boolean answer = {0}();\n"
              + "cache.put(\"{0}\",new Boolean(answer));\n"
              + "return answer;";
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
          return "double answer = {0}();\n"
              + "cache.put(\"{0}\",new Double(answer));\n"
              + "return answer;";
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
          return "int answer = {0}();\n"
              + "cache.put(\"{0}\",new Integer(answer));\n"
              + "return answer;";
        }
      };

      namesToMethodCreators.put("getInt", intMethod);

      // Float
      JType floatType = oracle.parse(float.class.getName());
      LookupMethodCreator floatMethod = new LookupMethodCreator(this, floatType) {
        @Override
        public String returnTemplate() {
          String val = "float answer = {0}();\n"
              + "cache.put(\"{0}\", new Float(answer));\n"
              + "return answer;";
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
          return "String answer = {0}();\n"
              + "cache.put(\"{0}\",answer);\n"
              + "return answer;";
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

  @Override
  protected void classEpilog() {
    createNeededPartitionLookups();
    super.classEpilog();
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
      LookupMethodCreator c = getLookupMethodCreator(name);
      if (c != null) {
        createMethodWithPartitionCheckFor(c, method);
        return;
      }
    }
    // fall through
    super.emitMethodBody(logger, method, locale);
  }

  void addNeededPartitionLookups(JMethod targetMethod,
      List<List<JMethod>> methodToCreatePartitionLookups) {
    neededPartitionLookups.put(targetMethod, methodToCreatePartitionLookups);
  }

  void createMethodWithPartitionCheckFor(LookupMethodCreator methodCreator, JMethod targetMethod) {
    List<List<JMethod>> methodPartitions = findMethodsToCreateWithPartitionSize(targetMethod,
        methodCreator.getReturnType());

    String nextPartitionMethod = null;
    final List<List<JMethod>> methodToCreatePartitionLookups;
    final List<JMethod> methodsToCreate;
    if (methodPartitions.size() > 1) {
      nextPartitionMethod = createPartitionMethodName(targetMethod, 0);
      methodsToCreate = methodPartitions.get(0);
      methodToCreatePartitionLookups = methodPartitions.subList(1, methodPartitions.size());
    } else {
      methodsToCreate = methodPartitions.isEmpty() ? Collections.<JMethod> emptyList()
          : methodPartitions.get(0);
      methodToCreatePartitionLookups = Collections.emptyList();
    }
    addNeededPartitionLookups(targetMethod, methodToCreatePartitionLookups);
    methodCreator.createCacheLookupFor();
    methodCreator.createMethodFor(targetMethod, methodsToCreate, nextPartitionMethod);
  }

  String createPartitionMethodName(JMethod targetMethod, int partitionIndex) {
    final String templatePartitionMethodName = "{0}FromPartition{1}";
    return MessageFormat.format(templatePartitionMethodName, new Object[] {
        targetMethod.getName(), partitionIndex});
  }

  List<JMethod> findAllMethodsToCreate(JMethod targetMethod, JType methodReturnType) {
    JMethod[] allMethods = allInterfaceMethods;
    JType erasedType = methodReturnType.getErasedType();
    List<JMethod> methodsToCreate = new ArrayList<>();
    for (JMethod methodToCheck : allMethods) {
      if (methodToCheck.getReturnType().getErasedType().equals(erasedType)
          && methodToCheck != targetMethod) {
        methodsToCreate.add(methodToCheck);
      }
    }
    return methodsToCreate;
  }

  List<List<JMethod>> findMethodsToCreateWithPartitionSize(JMethod targetMethod,
      JType methodReturnType) {
    List<JMethod> allMethodsToCreate = findAllMethodsToCreate(targetMethod, methodReturnType);
    return Lists.partition(allMethodsToCreate, partitionsSize);
  }

  LookupMethodCreator getLookupMethodCreator(String name) {
    return namesToMethodCreators.get(name);
  }

  /**
   * Visible for testing only.
   */
  Map<JMethod, List<List<JMethod>>> getNeededPartitionLookups() {
    return neededPartitionLookups;
  }

  /**
   * Checks that the method has the right structure to implement
   * <code>Constant</code>.
   * 
   * @param method method to check
   */
  private void checkMethod(TreeLogger logger, JMethod method)
      throws UnableToCompleteException {
    if (getLookupMethodCreator(method.getName()) != null) {
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

  private void createNeededPartitionLookups() {
    for (Entry<JMethod, List<List<JMethod>>> neededPartitionLookup : 
      neededPartitionLookups.entrySet()) {
      JMethod targetMethod = neededPartitionLookup.getKey();
      LookupMethodCreator lookupMethodCreator = getLookupMethodCreator(targetMethod.getName());
      List<List<JMethod>> methodForPartitionLookups = neededPartitionLookup.getValue();
      int partitionStartIndex = 0;
      Iterator<List<JMethod>> neededPartitionIterator = methodForPartitionLookups.iterator();
      while (neededPartitionIterator.hasNext()) {
        String currentPartitionLookupMethodName = createPartitionMethodName(targetMethod,
            partitionStartIndex++);
        List<JMethod> methodsToCreate = neededPartitionIterator.next();
        String nextPartitionMethod = null;
        if (neededPartitionIterator.hasNext()) {
          nextPartitionMethod = createPartitionMethodName(targetMethod, partitionStartIndex);
        }
        lookupMethodCreator.createPartitionLookup(currentPartitionLookupMethodName, targetMethod,
            methodsToCreate, nextPartitionMethod);
      }
    }
  }
}
