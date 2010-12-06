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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.javac.asm.CollectAnnotationData;
import com.google.gwt.dev.javac.typemodel.JAbstractMethod;
import com.google.gwt.dev.javac.typemodel.JClassType;
import com.google.gwt.dev.javac.typemodel.JMethod;
import com.google.gwt.dev.javac.typemodel.JPackage;
import com.google.gwt.dev.javac.typemodel.JRealClassType;
import com.google.gwt.dev.javac.typemodel.JTypeParameter;
import com.google.gwt.dev.javac.typemodel.TypeOracle;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 * Interface for resolving various aspects of a class.
 */
public interface Resolver {

  void addImplementedInterface(JRealClassType type, JClassType intf);

  void addThrows(JAbstractMethod method, JClassType exception);

  Map<String, JRealClassType> getBinaryMapper();

  TypeOracle getTypeOracle();

  JMethod newMethod(JClassType type, String name,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
      JTypeParameter[] typeParams);

  void newParameter(JAbstractMethod method, JType argType, String argName,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
      boolean argNamesAreReal);

  JRealClassType newRealClassType(JPackage pkg, String enclosingTypeName,
      boolean isLocalType, String className, boolean isIntf);

  boolean resolveAnnotation(TreeLogger logger,
      CollectAnnotationData annotVisitor,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations);

  boolean resolveAnnotations(TreeLogger logger,
      List<CollectAnnotationData> annotations,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations);

  boolean resolveClass(TreeLogger logger, JRealClassType type);

  void setReturnType(JAbstractMethod method, JType returnType);

  void setSuperClass(JRealClassType type, JClassType superType);
}
