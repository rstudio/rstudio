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
 * An implementation of the {@link HasAnnotations} interface that supports inheritance
 * of annotations. This is a mutable type, but it's an error to change it after doing
 * a query that looks at inherited annotations.
 */
class Annotations implements HasAnnotations {

  static final Comparator<Annotation> ANNOTATION_COMPARATOR = new Comparator<Annotation>() {
    /**
     * An element can only be annotated with one annotation of a particular
     * type. So we only need to sort by annotation type name, since there won't
     * be any duplicates.
     */
    @Override
    public int compare(Annotation o1, Annotation o2) {
      return o1.annotationType().getName().compareTo(
          o2.annotationType().getName());
    }
  };

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

  Annotations(Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
    this.declaredAnnotations = Maps.normalize(declaredAnnotations);
  }

  /**
   * Adds annotations to the set. It's an error to call this after calling
   * {@link #getAnnotation}, {@link #getAnnotations}, or
   * {@link #isAnnotationPresent}.
   */
  public void addAnnotations(
      Map<Class<? extends Annotation>, Annotation> additions) {
    assert lazyAnnotations == null;
    if (additions != null) {
      assert (!additions.containsValue(null));
      declaredAnnotations = Maps.putAll(declaredAnnotations, additions);
    }
  }

  @Override
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    initializeAnnotations();
    return annotationClass.cast(lazyAnnotations.get(annotationClass));
  }

  @Override
  public Annotation[] getAnnotations() {
    initializeAnnotations();
    List<Annotation> values = new ArrayList<Annotation>(lazyAnnotations.values());
    Collections.sort(values, ANNOTATION_COMPARATOR);
    return values.toArray(new Annotation[values.size()]);
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    List<Annotation> values = new ArrayList<Annotation>(
        declaredAnnotations.values());
    Collections.sort(values, ANNOTATION_COMPARATOR);
    return values.toArray(new Annotation[values.size()]);
  }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return getAnnotation(annotationClass) != null;
  }

  /**
   * Sets a parent to inherit annotations from, or null to clear. It's an error to call
   * this after calling {@link #getAnnotation}, {@link #getAnnotations}, or
   * {@link #isAnnotationPresent}.
   */
  void setParent(Annotations parent) {
    assert lazyAnnotations == null;
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
