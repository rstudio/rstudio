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
import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.asm.CollectAnnotationData;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 * Interface for resolving various aspects of a class.
 */
public interface Resolver {

  Map<String, JRealClassType> getBinaryMapper();

  TypeOracle getTypeOracle();

  boolean resolveAnnotation(TreeLogger logger,
      CollectAnnotationData annotVisitor,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations);

  boolean resolveAnnotations(TreeLogger logger,
      List<CollectAnnotationData> annotations,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations);

  boolean resolveClass(TreeLogger logger, JRealClassType type);
}
