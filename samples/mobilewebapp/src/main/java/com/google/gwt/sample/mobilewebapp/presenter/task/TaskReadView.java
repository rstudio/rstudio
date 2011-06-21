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
package com.google.gwt.sample.mobilewebapp.presenter.task;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.sample.ui.client.PresentsWidgets;
import com.google.gwt.user.client.ui.IsWidget;

/**
 * A readonly view of a task.
 */
public interface TaskReadView extends Editor<TaskProxy>, IsWidget {

  /**
   * The presenter for this view.
   */
  public interface Presenter extends PresentsWidgets {
    /**
     * Switch to an edit view of this task.
     */
    void editTask();
  }

  /**
   * Get the driver used to edit tasks in the view.
   */
  SimpleBeanEditorDriver<TaskProxy, ?> getEditorDriver();
  
  /**
   * Set the {@link Presenter} for this view.
   * @param presenter the presenter
   */
  void setPresenter(Presenter presenter);
}
