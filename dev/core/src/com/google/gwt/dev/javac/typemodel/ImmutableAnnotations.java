/*
 * Copyright 2013 Google Inc.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Immutable implementation of the {@link HasAnnotations}
 * interface. Does not support inheritance.
 */
class ImmutableAnnotations implements HasAnnotations {

  public static final ImmutableAnnotations EMPTY = new ImmutableAnnotations() {
    @Override
    public Annotation[] getDeclaredAnnotations() {
      return new Annotation[0];
    }
  };

  private final Map<Class<? extends Annotation>, Annotation> members;

  private ImmutableAnnotations() {
    this.members = Maps.create();
  }

  private ImmutableAnnotations(ImmutableAnnotations base,
      Map<Class<? extends Annotation>, Annotation> additions) {
    this.members = copyOfAnnotations(base, additions);
  }

  /**
   * Returns a possibly new instance with additional annotations.
   * (If additions is null or empty, the original will be returned.)
   */
  public ImmutableAnnotations plus(Map<Class<? extends Annotation>, Annotation> additions) {
    if (additions == null || additions.size() == 0) {
      return this;
    }
    return new ImmutableAnnotations(this, additions);
  }

  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    return annotationClass.cast(members.get(annotationClass));
  }

  public Annotation[] getAnnotations() {
    return getDeclaredAnnotations();
  }

  public Annotation[] getDeclaredAnnotations() {
    List<Annotation> values = new ArrayList<Annotation>(members.values());
    Collections.sort(values, Annotations.ANNOTATION_COMPARATOR);
    return values.toArray(new Annotation[values.size()]);
  }

  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return getAnnotation(annotationClass) != null;
  }

  private static Map<Class<? extends Annotation>, Annotation> copyOfAnnotations(
      ImmutableAnnotations base, Map<Class<? extends Annotation>, Annotation> additions) {
    Map<Class<? extends Annotation>, Annotation> result =
        new HashMap<Class<? extends Annotation>, Annotation>();
    result.putAll(base.members);
    for (Annotation addition : additions.values()) {
      Class<? extends Annotation> type = addition.annotationType();
      assert (type != null);
      assert (!result.containsKey(type));
      result.put(type, addition);
    }
    return Maps.normalize(result);
  }
}
