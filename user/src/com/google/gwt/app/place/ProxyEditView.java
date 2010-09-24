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
package com.google.gwt.app.place;

import com.google.gwt.editor.client.HasEditorErrors;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.client.RequestFactoryEditorDriver;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.user.client.ui.IsWidget;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Implemented by views that edit {@link EntityProxy}s.
 * 
 * @param <P> the type of the proxy
 * @param <V> the type of this ProxyEditView, required to allow
 *          {@link #createEditorDriver(EventBus, RequestFactory)} to be
 *          correctly typed
 */
public interface ProxyEditView<P extends EntityProxy, V extends ProxyEditView<P, V>>
    extends IsWidget, HasEditorErrors<P> {

  /**
   * @return a {@link RequestFactoryEditorDriver} initialized to run this editor
   */
  RequestFactoryEditorDriver<P, V> createEditorDriver(EventBus eventBus,
      RequestFactory requestFactory);

  /**
   * Implemented by the owner of the view.
   */
  interface Delegate {
    void cancelClicked();

    void saveClicked();
  }

  void setCreating(boolean b);

  void setDelegate(Delegate delegate);

  void setEnabled(boolean b);
}
