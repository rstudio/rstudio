/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.requestfactory.client.impl;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.editor.client.impl.DelegateMap;
import com.google.gwt.editor.client.impl.SimpleViolation;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.client.RequestFactoryEditorDriver;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.ValueProxy;
import com.google.gwt.requestfactory.shared.Violation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.validation.ConstraintViolation;

/**
 * Contains utility methods for top-level driver implementations.
 * 
 * @param <R> the type of Record
 * @param <E> the type of Editor
 */
public abstract class AbstractRequestFactoryEditorDriver<R, E extends Editor<R>>
    implements RequestFactoryEditorDriver<R, E> {

  /**
   * Adapts a RequestFactory Violation object to the SimpleViolation interface.
   */
  static class SimpleViolationAdapter extends SimpleViolation {
    private final Violation v;

    /**
     * @param source
     */
    private SimpleViolationAdapter(Violation v) {
      this.v = v;
    }

    public Object getKey() {
      return v.getOriginalProxy();
    }

    public String getMessage() {
      return v.getMessage();
    }

    public String getPath() {
      return v.getPath();
    }

    public Object getUserDataObject() {
      return v;
    }
  }

  /**
   * Provides a source of SimpleViolation objects based on RequestFactory's
   * simplified Violation interface.
   */
  static class ViolationIterable implements Iterable<SimpleViolation> {

    private final Iterable<Violation> violations;

    public ViolationIterable(Iterable<Violation> violations) {
      this.violations = violations;
    }

    public Iterator<SimpleViolation> iterator() {
      final Iterator<Violation> source = violations.iterator();
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
   * Since the ValueProxy is being mutated in-place, we need a way to stabilize
   * its hashcode for future equality checks.
   */
  private static class ValueProxyHolder {
    private final ValueProxy proxy;

    public ValueProxyHolder(ValueProxy proxy) {
      this.proxy = proxy;
    }

    @Override
    public boolean equals(Object o) {
      return proxy.equals(((ValueProxyHolder) o).proxy);
    }

    @Override
    public int hashCode() {
      return proxy.getClass().hashCode();
    }
  }

  private static final DelegateMap.KeyMethod PROXY_ID_KEY = new DelegateMap.KeyMethod() {
    public Object key(final Object object) {
      if (object instanceof EntityProxy) {
        return ((EntityProxy) object).stableId();
      } else if (object instanceof ValueProxy) {
        return new ValueProxyHolder((ValueProxy) object);
      }
      return null;
    }
  };

  private RequestFactoryEditorDelegate<R, E> delegate;
  private DelegateMap delegateMap = new DelegateMap(PROXY_ID_KEY);
  private E editor;
  private EventBus eventBus;
  private List<EditorError> errors;
  private List<String> paths = new ArrayList<String>();
  private RequestFactory requestFactory;
  private RequestContext saveRequest;

  public void display(R object) {
    edit(object, null);
  }

  public void edit(R object, RequestContext saveRequest) {
    checkEditor();
    this.saveRequest = saveRequest;
    delegate = createDelegate();
    delegate.initialize(eventBus, requestFactory, "", object, editor,
        delegateMap, saveRequest);
    delegateMap.put(object, delegate);
  }

  public RequestContext flush() {
    checkDelegate();
    checkSaveRequest();
    errors = new ArrayList<EditorError>();
    delegate.flush(errors);

    return saveRequest;
  }

  public List<EditorError> getErrors() {
    return errors;
  }

  public String[] getPaths() {
    return paths.toArray(new String[paths.size()]);
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public void initialize(E editor) {
    doInitialize(null, null, editor);
  }

  public void initialize(EventBus eventBus, RequestFactory requestFactory,
      E editor) {
    assert eventBus != null : "eventBus must not be null";
    assert requestFactory != null : "requestFactory must not be null";
    doInitialize(eventBus, requestFactory, editor);
  }

  public void initialize(RequestFactory requestFactory, E editor) {
    initialize(requestFactory.getEventBus(), requestFactory, editor);
  }

  public boolean setConstraintViolations(
      Iterable<ConstraintViolation<?>> violations) {
    return doSetViolations(SimpleViolation.iterableFromConstrantViolations(violations));
  }

  public boolean setViolations(Iterable<Violation> violations) {
    return doSetViolations(new ViolationIterable(violations));
  }

  protected abstract RequestFactoryEditorDelegate<R, E> createDelegate();

  protected E getEditor() {
    return editor;
  }

  protected abstract void traverseEditors(List<String> paths);

  private void checkDelegate() {
    if (delegate == null) {
      throw new IllegalStateException("Must call edit() first");
    }
  }

  private void checkEditor() {
    if (editor == null) {
      throw new IllegalStateException("Must call initialize() first");
    }
  }

  private void checkSaveRequest() {
    if (saveRequest == null) {
      throw new IllegalStateException("edit() was called with a null Request");
    }
  }

  private void doInitialize(EventBus eventBus, RequestFactory requestFactory,
      E editor) {
    this.eventBus = eventBus;
    this.requestFactory = requestFactory;
    this.editor = editor;

    traverseEditors(paths);
  }

  private boolean doSetViolations(Iterable<SimpleViolation> violations) {
    checkDelegate();
    SimpleViolation.pushViolations(violations, delegateMap);

    // Flush the errors, which will take care of co-editor chains.
    errors = new ArrayList<EditorError>();
    delegate.flushErrors(errors);
    return hasErrors();
  }
}
