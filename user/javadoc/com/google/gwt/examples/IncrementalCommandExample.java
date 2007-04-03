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
package com.google.gwt.examples;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.ui.Label;

public class IncrementalCommandExample implements EntryPoint {

  public void onModuleLoad() {
    final Label label = new Label();
    
    DeferredCommand.addCommand(new IncrementalCommand() {
      private int index = 0;
      
      protected static final int COUNT = 10;
      
      public boolean execute() {
        label.setText("IncrementalCommand - index " + Integer.toString(index));
        
        return ++index < COUNT;
      }
    });
  }
}
