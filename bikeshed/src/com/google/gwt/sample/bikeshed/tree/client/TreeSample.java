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
package com.google.gwt.sample.bikeshed.tree.client;

import com.google.gwt.bikeshed.tree.client.SideBySideTreeView;
import com.google.gwt.bikeshed.tree.client.StandardTreeView;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * A demo of the asynchronous Tree model.
 */
public class TreeSample implements EntryPoint {

  public void onModuleLoad() {
    StandardTreeView tree = new StandardTreeView(new MyTreeViewModel(), "...");
    tree.setAnimationEnabled(true);
    RootPanel.get().add(tree);

    RootPanel.get().add(new HTML("<hr>"));
    
    SideBySideTreeView sstree = new SideBySideTreeView(new MyTreeViewModel(), "...", 100, 200);
    RootPanel.get().add(sstree);
  }
}
