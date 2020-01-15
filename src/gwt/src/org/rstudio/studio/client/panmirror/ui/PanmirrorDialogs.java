/*
 * PanmirrorDialogs.java
 *
 * Copyright (C) 2009-20 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.panmirror.ui;

import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;

import com.google.inject.Inject;

import elemental2.promise.Promise;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.RejectCallbackFn;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.ResolveCallbackFn;

import jsinterop.annotations.JsType;


@JsType
enum AlertType {
   Info,
   Warning,
   Error
}

@JsType
public class PanmirrorDialogs {
   
  
   public PanmirrorDialogs() {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(GlobalDisplay globalDisplay)
   {
      this.globalDisplay_ = globalDisplay;
   }
   
   public Promise<Boolean> alert(String message, String title, AlertType type) {
      
      return new Promise<Boolean>((ResolveCallbackFn<Boolean> resolve, RejectCallbackFn reject) -> {
         
         int alertType = MessageDisplay.MSG_INFO;
         switch(type) {
            case Info:
              alertType = MessageDisplay.MSG_INFO;
              break;
            case Warning:
              alertType = MessageDisplay.MSG_WARNING;
              break;
            case Error:
              alertType = MessageDisplay.MSG_ERROR;
              break;
         }
         PanmirrorDialogs.this.globalDisplay_.showMessage(alertType, title, message, new Operation() {
            @Override
            public void execute()
            {
               resolve.onInvoke(true);    
            }        
         });
       
      });
      
   }
   
   private GlobalDisplay globalDisplay_;

  
}