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

import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.user.client.TakesValue;
import com.google.gwt.user.client.ui.IsWidget;

import java.util.Map;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Implemented by views that edit {@link EntityProxy}s.
 * 
 * @param <P> the type of the proxy
 */
public interface ProxyEditView<P extends EntityProxy> extends TakesValue<P>,
    IsWidget, PropertyView<P> {
  
  /**
   * Implemented by the owner of the view.
   */
  interface Delegate {
    void cancelClicked();
    void saveClicked();
  }
  
  boolean isChanged();
  void setCreating(boolean b);
  void setDelegate(Delegate delegate);
  void setEnabled(boolean b);
  
  // TODO needs to be Map<Property<?>, String> errors
  void showErrors(Map<String, String> errors);
}
