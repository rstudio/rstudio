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
package com.google.gwt.dev.javac.asm;

import com.google.gwt.dev.asm.AnnotationVisitor;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.javac.asm.CollectClassData.AnnotationEnum;
import com.google.gwt.dev.util.StringInterner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects data from (possibly nested) annotations on a single entity.
 */
public class CollectAnnotationData extends AnnotationVisitor {

  /**
   * Holds annotation fields/values.
   */
  public static class AnnotationData {
    private final String desc;
    private final Map<String, Object> values = new HashMap<String, Object>();
    private final boolean visible;

    protected AnnotationData(String desc, boolean visible) {
      this.desc = StringInterner.get().intern(desc);
      this.visible = visible;
    }

    public void addValue(String name, Object value) {
      values.put(StringInterner.get().intern(name), value);
    }

    /**
     * @return the desc
     */
    public String getDesc() {
      return desc;
    }

    /**
     * @return the values
     */
    public Map<String, Object> getValues() {
      return values;
    }

    /**
     * @return the visible
     */
    public boolean isVisible() {
      return visible;
    }
  }

  /**
   * Collects data inside an array-valued annotation parameter.
   */
  public static class MyAnnotationArrayVisitor extends AnnotationVisitor {

    private final Callback<Object> callback;
    private final List<Object> values = new ArrayList<Object>();

    public MyAnnotationArrayVisitor(Callback<Object> callback) {
      super(Opcodes.ASM4);
      this.callback = callback;
    }

    @Override
    public void visit(String name, Object value) {
      values.add(value);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc) {
      // TODO(jat): what should visible be set to?
      return new CollectAnnotationData(desc, true,
          new Callback<CollectAnnotationData.AnnotationData>() {
            @Override
            public void call(CollectAnnotationData.AnnotationData value) {
              values.add(value);
            }
          });
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
      return new MyAnnotationArrayVisitor(new Callback<Object>() {
        @Override
        public void call(Object value) {
          values.add(value);
        }
      });
    }

    @Override
    public void visitEnd() {
      callback.call(values.toArray());
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
      values.add(new AnnotationEnum(desc, value));
    }
  }

  /**
   * Generic callback type taking a parameter.
   *
   * @param <T> type of the argument to the callback
   */
  private interface Callback<T> {

    /**
     * Invoke the callback.
     *
     * @param value value to pass to the callback.
     */
    void call(T value);
  }

  private final CollectAnnotationData.AnnotationData annotation;
  private final Callback<CollectAnnotationData.AnnotationData> callback;

  /**
   * Construct the collector.
   *
   * @param desc class descriptor of the annotation class
   * @param visible true if the annotation is visible at runtime
   */
  public CollectAnnotationData(String desc, boolean visible) {
    this(desc, visible, null);
  }

  /**
   * Construct the collector.
   *
   * @param desc class descriptor of the annotation class
   * @param visible true if the annotation is visible at runtime
   * @param callback callback to be called when the annotation is finished
   */
  CollectAnnotationData(String desc, boolean visible,
      Callback<CollectAnnotationData.AnnotationData> callback) {
    super(Opcodes.ASM4);
    annotation = new AnnotationData(desc, visible);
    this.callback = callback;
  }

  /**
   * @return the annotation data
   */
  public AnnotationData getAnnotation() {
    return annotation;
  }

  @Override
  public void visit(String name, Object value) {
    annotation.addValue(name, value);
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String name, String desc) {
    // TODO(jat): what should visible be set to?
    return new CollectAnnotationData(desc, true,
        new Callback<CollectAnnotationData.AnnotationData>() {
          @Override
          public void call(CollectAnnotationData.AnnotationData value) {
            annotation.addValue(name, value);
          }
        });
  }

  @Override
  public AnnotationVisitor visitArray(final String name) {
    return new MyAnnotationArrayVisitor(new Callback<Object>() {

      /**
       * Called with an array of values.
       *
       * @param value
       */
      @Override
      public void call(Object value) {
        annotation.addValue(name, value);
      }
    });
  }

  @Override
  public void visitEnd() {
    if (callback != null) {
      callback.call(annotation);
    }
  }

  @Override
  public void visitEnum(String name, String desc, String value) {
    annotation.addValue(name, new AnnotationEnum(desc, value));
  }
}
