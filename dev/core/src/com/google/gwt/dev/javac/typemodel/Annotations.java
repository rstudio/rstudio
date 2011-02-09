/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.javac.typemodel;

import com.google.gwt.core.ext.typeinfo.HasAnnotations;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.dev.util.collect.Maps;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Default implementation of the {@link HasAnnotations} interface.
 */
class Annotations implements HasAnnotations {

  private static final Comparator<Annotation> ANNOTATION_COMPARATOR = new Comparator<Annotation>() {
    /**
     * An element can only be annotated with one annotation of a particular
     * type. So we only need to sort by annotation type name, since there won't
     * be any duplicates.
     */
    public int compare(Annotation o1, Annotation o2) {
      return o1.annotationType().getName().compareTo(
          o2.annotationType().getName());
    }
  };

  private static Map<Class<? extends Annotation>, Annotation> copyOfAnnotations(
      Annotations otherAnnotations) {
    Map<Class<? extends Annotation>, Annotation> declaredAnnotations = new HashMap<Class<? extends Annotation>, Annotation>();
    if (otherAnnotations != null) {
      Annotation[] otherDeclaredAnnotations = otherAnnotations.getDeclaredAnnotations();
      for (Annotation otherDeclaredAnnotation : otherDeclaredAnnotations) {
        Class<? extends Annotation> otherDeclaredAnnotationType = otherDeclaredAnnotation.annotationType();
        assert (otherDeclaredAnnotationType != null);
        assert (!declaredAnnotations.containsKey(otherDeclaredAnnotationType));

        declaredAnnotations.put(otherDeclaredAnnotationType,
            otherDeclaredAnnotation);
      }
    }
    return declaredAnnotations;
  }

  /**
   * All annotations declared on the annotated element.
   */
  private Map<Class<? extends Annotation>, Annotation> declaredAnnotations;

  /**
   * Lazily initialized collection of annotations declared on or inherited by
   * the annotated element.
   */
  private Map<Class<? extends Annotation>, Annotation> lazyAnnotations = null;

  /**
   * If not <code>null</code> the parent to inherit annotations from.
   */
  private Annotations parent;

  Annotations() {
    this.declaredAnnotations = Maps.create();
  }

  Annotations(Annotations otherAnnotations) {
    this(copyOfAnnotations(otherAnnotations));
  }

  Annotations(Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
    this.declaredAnnotations = Maps.normalize(declaredAnnotations);
  }

  public void addAnnotations(
      Map<Class<? extends Annotation>, Annotation> annotations) {
    if (annotations != null) {
      assert (!annotations.containsValue(null));
      declaredAnnotations = Maps.putAll(declaredAnnotations, annotations);
    }
  }

  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    initializeAnnotations();
    return annotationClass.cast(lazyAnnotations.get(annotationClass));
  }

  public Annotation[] getAnnotations() {
    initializeAnnotations();
    List<Annotation> values = new ArrayList<Annotation>(
        lazyAnnotations.values());
    Collections.sort(values, ANNOTATION_COMPARATOR);
    return values.toArray(new Annotation[values.size()]);
  }

  public Annotation[] getDeclaredAnnotations() {
    List<Annotation> values = new ArrayList<Annotation>(
        declaredAnnotations.values());
    Collections.sort(values, ANNOTATION_COMPARATOR);
    return values.toArray(new Annotation[values.size()]);
  }

  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return getAnnotation(annotationClass) != null;
  }

  void setParent(Annotations parent) {
    this.parent = parent;
  }

  private void initializeAnnotations() {
    if (lazyAnnotations != null) {
      return;
    }

    if (parent != null) {
      lazyAnnotations = new HashMap<Class<? extends Annotation>, Annotation>();
      parent.initializeAnnotations();
      for (Entry<Class<? extends Annotation>, Annotation> entry : parent.lazyAnnotations.entrySet()) {
        if (entry.getValue().annotationType().isAnnotationPresent(
            Inherited.class)) {
          lazyAnnotations.put(entry.getKey(), entry.getValue());
        }
      }

      lazyAnnotations.putAll(declaredAnnotations);
      lazyAnnotations = Maps.normalize(lazyAnnotations);
    } else {
      lazyAnnotations = declaredAnnotations;
    }
  }
}
