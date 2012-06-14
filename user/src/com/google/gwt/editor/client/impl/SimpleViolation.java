/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.editor.client.impl;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorDriver;
import com.google.gwt.editor.client.impl.DelegateMap.KeyMethod;

import java.util.Iterator;
import java.util.List;

import javax.validation.ConstraintViolation;

/**
 * Abstraction of a ConstraintViolation or a RequestFactory Violation object.
 * Also contains a factory method to create SimpleViolation instances from
 * {@link ConstraintViolation} objects.
 */
public abstract class SimpleViolation {
  /**
   * Provides a source of SimpleViolation objects based on ConstraintViolations.
   * This is re-used by the RequestFactoryEditorDriver implementation, which
   * does not share a type hierarchy with the SimpleBeanEditorDriver.
   */
  static class ConstraintViolationIterable implements Iterable<SimpleViolation> {

    private final Iterable<ConstraintViolation<?>> violations;

    public ConstraintViolationIterable(
        Iterable<ConstraintViolation<?>> violations) {
      this.violations = violations;
    }

    public Iterator<SimpleViolation> iterator() {
      // Use a fresh source iterator each time
      final Iterator<ConstraintViolation<?>> source = violations.iterator();
      return new Iterator<SimpleViolation>() {
        public boolean hasNext() {
          return source.hasNext();
        }

        public SimpleViolation next() {
          return new SimpleViolationAdapter(source.next());
        }

        public void remove() {
          source.remove();
        }
      };
    }
  }

  /**
   * Adapts the ConstraintViolation interface to the SimpleViolation interface.
   */
  static class SimpleViolationAdapter extends SimpleViolation {
    private final ConstraintViolation<?> v;

    public SimpleViolationAdapter(ConstraintViolation<?> v) {
      this.v = v;
    }

    @Override
    public Object getKey() {
      return v.getRootBean();
    }

    @Override
    public String getMessage() {
      return v.getMessage();
    }

    @Override
    public String getPath() {
      /*
       * TODO(bobv,nchalko): Determine the correct way to extract this
       * information from the ConstraintViolation.
       */
      return v.getPropertyPath().toString();
    }

    @Override
    public Object getUserDataObject() {
      return v;
    }
  }

  public static Iterable<SimpleViolation> iterableFromConstrantViolations(
      Iterable<ConstraintViolation<?>> violations) {
    return new ConstraintViolationIterable(violations);
  }

  /**
   * Maps an abstract representation of a violation into the appropriate
   * EditorDelegate.
   */
  public static void pushViolations(Iterable<SimpleViolation> violations,
      EditorDriver<?> driver, KeyMethod keyMethod) {
    DelegateMap delegateMap = DelegateMap.of(driver, keyMethod);

    // For each violation
    for (SimpleViolation error : violations) {
      Object key = error.getKey();
      List<AbstractEditorDelegate<?, ?>> delegateList = delegateMap.get(key);
      if (delegateList != null) {

        // For each delegate editing some record...
        for (AbstractEditorDelegate<?, ?> baseDelegate : delegateList) {

          // compute its base path in the hierarchy...
          String basePath = baseDelegate.getPath();

          // and the absolute path of the leaf editor receiving the error.
          String absolutePath = (basePath.length() > 0 ? basePath + "." : "")
              + error.getPath();
          final String originalAbsolutePath = absolutePath;
          while (true) {
            if (processLeafDelegates(
                delegateMap, originalAbsolutePath, absolutePath, error)) {
              break;
            } else if (processEditors(
                delegateMap, baseDelegate, absolutePath, error)) {
              break;
            } else {
              // This is guaranteed to never happen because we should always
              // process a delegate/editor if the absolutePath is empty.
              // Still, we have the check here to prevent an infinite
              // loop if something goes wrong.
              if (absolutePath.isEmpty()) {
                throw new IllegalStateException(
                    "No editor: " + originalAbsolutePath);
              }
              absolutePath = getParentPath(absolutePath);
            }
          }
        }
      }
    }
  }

  /**
   * Returns the path with everything after the last '.' stripped off,
   * or "" if there was no '.' in the path.
   */
  private static String getParentPath(String absolutePath) {
    // Traverse upwards in the path to the parents in order
    // to report the error to the nearest valid parent.
    int dotIdx = absolutePath.lastIndexOf('.');
    if (dotIdx > 0) {
      return absolutePath.substring(0, dotIdx);
    }
    return "";
  }

  /**
   * Records an error in any editors that match the path, returning true
   * if any editors matched.
   */
  private static boolean processEditors(DelegateMap delegateMap,
      AbstractEditorDelegate<?, ?> baseDelegate,
      String absolutePath,
      SimpleViolation error) {
    List<Editor<?>> editors = delegateMap.getEditorByPath(absolutePath);
    if (editors == null) {
      return false;
    }
    // No EditorDelegate to attach it to, so record on the baseDelegate
    // with the appropriate editor & path.
    for (Editor<?> editor : editors) {
      baseDelegate.recordError(error.getMessage(), null,
          error.getUserDataObject(), error.getPath(), editor);
    }
    return true;
  }

  /**
   * Records an error in any delegates that match the {@code absolutePath},
   * returning true if any matched.  ({@code originalAbsolutePath}
   * is not used for finding delegates, but is instead used to determine
   * how to record the error.)
   */
  private static boolean processLeafDelegates(DelegateMap delegateMap,
      String originalAbsolutePath,
      String absolutePath,
      SimpleViolation error) {
    // Find the leaf editor's delegate.
    List<AbstractEditorDelegate<?, ?>> leafDelegates =
      delegateMap.getDelegatesByPath(absolutePath);
    if (leafDelegates == null) {
      return false;
    }
    String addlPath = originalAbsolutePath.substring(absolutePath.length());
    for (AbstractEditorDelegate<?, ?> delegate : leafDelegates) {
      // If this is the original path value, don't record the additional path.
      if (addlPath.isEmpty()) {
        delegate.recordError(error.getMessage(), null,
            error.getUserDataObject());
      } else {
        // Otherwise, include the additional path.
        delegate.recordError(error.getMessage(), null,
            error.getUserDataObject(), addlPath,
            delegate.getEditor());
      }
    }
    return true;
  }

  /**
   * Typically constructed via factory methods.
   */
  protected SimpleViolation() {
  }

  /**
   * Return the object that the violation is about.
   */
  public abstract Object getKey();

  /**
   * Return a user-facing message describing the violation.
   */
  public abstract String getMessage();

  /**
   * Return a dotted path describing the property.
   */
  public abstract String getPath();

  /**
   * An object that should be available from
   * {@link com.google.gwt.editor.client.EditorError#getUserData()}.
   */
  public abstract Object getUserDataObject();
}
