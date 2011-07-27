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
package com.google.web.bindery.requestfactory.gwt.client.impl;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorVisitor;
import com.google.gwt.editor.client.impl.AbstractEditorDelegate;
import com.google.gwt.editor.client.impl.BaseEditorDriver;
import com.google.gwt.editor.client.impl.DelegateMap;
import com.google.gwt.editor.client.impl.DelegateMap.KeyMethod;
import com.google.gwt.editor.client.impl.SimpleViolation;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;
import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.ValueProxy;
import com.google.web.bindery.requestfactory.shared.impl.Constants;

import java.util.Iterator;
import java.util.List;

/**
 * Contains utility methods for top-level driver implementations.
 * 
 * @param <R> the type being edited
 * @param <E> the type of Editor
 */
public abstract class AbstractRequestFactoryEditorDriver<R, E extends Editor<R>> extends
    BaseEditorDriver<R, E> implements RequestFactoryEditorDriver<R, E> {

  /**
   * Adapts a RequestFactory Violation object to the SimpleViolation interface.
   */
  @SuppressWarnings("deprecation")
  static class SimpleViolationAdapter extends SimpleViolation {
    private final com.google.web.bindery.requestfactory.shared.Violation v;

    /**
     * @param v
     */
    private SimpleViolationAdapter(com.google.web.bindery.requestfactory.shared.Violation v) {
      this.v = v;
    }

    @Override
    public Object getKey() {
      return v.getOriginalProxy();
    }

    @Override
    public String getMessage() {
      return v.getMessage();
    }

    @Override
    public String getPath() {
      return v.getPath();
    }

    @Override
    public Object getUserDataObject() {
      return v;
    }
  }
  /**
   * Provides a source of SimpleViolation objects based on RequestFactory's
   * simplified Violation interface.
   */
  @SuppressWarnings("deprecation")
  static class ViolationIterable implements Iterable<SimpleViolation> {

    private final Iterable<com.google.web.bindery.requestfactory.shared.Violation> violations;

    public ViolationIterable(
        Iterable<com.google.web.bindery.requestfactory.shared.Violation> violations) {
      this.violations = violations;
    }

    public Iterator<SimpleViolation> iterator() {
      final Iterator<com.google.web.bindery.requestfactory.shared.Violation> source =
          violations.iterator();
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
    public Object key(Object object) {
      if (object instanceof EntityProxy) {
        return ((EntityProxy) object).stableId();
      } else if (object instanceof ValueProxy) {
        AutoBean<?> bean = AutoBeanUtils.getAutoBean(object);
        // Possibly replace an editable ValueProxy with its immutable base
        AutoBean<?> parent = bean.getTag(Constants.PARENT_OBJECT);
        if (parent != null) {
          object = parent.as();
        }
        return new ValueProxyHolder((ValueProxy) object);
      }
      return null;
    }
  };

  private EventBus eventBus;
  private List<String> paths;
  private RequestFactory factory;
  private RequestContext saveRequest;

  public void display(R object) {
    edit(object, null);
  }

  public void edit(R object, RequestContext saveRequest) {
    this.saveRequest = saveRequest;
    /*
     * Provide the delegate with the RequestContext so ensureMutable works as
     * expected Editor will be provided the delegate by the Initializer visitor.
     */
    ((RequestFactoryEditorDelegate<R, E>) getDelegate()).setRequestContext(saveRequest);
    doEdit(object);
  }

  public RequestContext flush() {
    checkSaveRequest();
    doFlush();
    return saveRequest;
  }

  public String[] getPaths() {
    return paths.toArray(new String[paths.size()]);
  }

  public void initialize(E editor) {
    doInitialize(null, null, editor);
  }

  public void initialize(EventBus eventBus, RequestFactory requestFactory, E editor) {
    assert eventBus != null : "eventBus must not be null";
    assert requestFactory != null : "requestFactory must not be null";
    doInitialize(eventBus, requestFactory, editor);
  }

  public void initialize(RequestFactory requestFactory, E editor) {
    initialize(requestFactory.getEventBus(), requestFactory, editor);
  }

  @SuppressWarnings("deprecation")
  public boolean setViolations(
      Iterable<com.google.web.bindery.requestfactory.shared.Violation> violations) {
    return doSetViolations(new ViolationIterable(violations));
  }

  protected void checkSaveRequest() {
    if (saveRequest == null) {
      throw new IllegalStateException("edit() was called with a null Request");
    }
  }

  @Override
  protected void configureDelegate(AbstractEditorDelegate<R, E> rootDelegate) {
    ((RequestFactoryEditorDelegate<R, E>) rootDelegate).initialize(eventBus, factory, "",
        getEditor());
  }

  @Override
  protected EditorVisitor createInitializerVisitor() {
    return new Initializer(saveRequest);
  }

  protected void doInitialize(EventBus eventBus, RequestFactory requestFactory, E editor) {
    this.eventBus = eventBus;
    this.factory = requestFactory;
    super.doInitialize(editor);
    PathCollector c = new PathCollector();
    accept(c);
    this.paths = c.getPaths();
  }

  @Override
  protected KeyMethod getViolationKeyMethod() {
    return PROXY_ID_KEY;
  }
}
