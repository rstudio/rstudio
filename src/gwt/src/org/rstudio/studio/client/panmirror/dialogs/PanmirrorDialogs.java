/*
 * PanmirrorDialogs.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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

package org.rstudio.studio.client.panmirror.dialogs;

import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCitationResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertTableResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorRawFormatProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorRawFormatResult;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
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
   
   public Promise<Boolean> alert(String message, String title, AlertType type) 
   {   
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


   
   public Promise<PanmirrorAttrResult> editAttr(PanmirrorAttrProps attr)
   {
      return editPanmirrorAttr("Edit Attributes", false, attr);
   }

   
   public Promise<PanmirrorAttrResult> editSpan(PanmirrorAttrProps attr)
   {
      return editPanmirrorAttr("Span Attributes", true, attr);
   }
   
   public Promise<PanmirrorAttrResult> editDiv(PanmirrorAttrProps attr, boolean removeEnabled)
   {
      return editPanmirrorAttr("Section/Div Attributes", removeEnabled, attr);
   }


   // TODO
   private Promise<PanmirrorAttrResult> editPanmirrorAttr(String caption, boolean removeEnabled, PanmirrorAttrProps attr) 
   {
      return new Promise<PanmirrorAttrResult>(
         (ResolveCallbackFn<PanmirrorAttrResult> resolve, RejectCallbackFn reject) -> {  
            PanmirrorEditAttrDialog dialog = new PanmirrorEditAttrDialog(caption, removeEnabled, attr, 
               (result) -> { resolve.onInvoke(result); }
            );
            dialog.showModal();
         }
      );
   }
   
   
   public Promise<PanmirrorRawFormatResult> editRawInline(PanmirrorRawFormatProps raw) 
   {
      return editRaw(raw, 2);
   }
   
   public Promise<PanmirrorRawFormatResult> editRawBlock(PanmirrorRawFormatProps raw) 
   {
      return editRaw(raw, 10);
   }
   

   private Promise<PanmirrorRawFormatResult> editRaw(PanmirrorRawFormatProps raw, int minLines)
   {
      return new Promise<PanmirrorRawFormatResult>(
         (ResolveCallbackFn<PanmirrorRawFormatResult> resolve, RejectCallbackFn reject) -> {  
            PanmirrorEditRawDialog dialog = new PanmirrorEditRawDialog(raw, minLines, 
               (result) -> { resolve.onInvoke(result); }
            );
            dialog.showModal();
         }
      );
   }
   
   public Promise<PanmirrorInsertTableResult> insertTable()
   {
      return new Promise<PanmirrorInsertTableResult>(
         (ResolveCallbackFn<PanmirrorInsertTableResult> resolve, RejectCallbackFn reject) -> {  
            PanmirrorInsertTableDialog dialog = new PanmirrorInsertTableDialog((result) -> {
               resolve.onInvoke(result);
            });
            dialog.showModal();
         }
      );
   }
   
   
   // TODO
   public Promise<PanmirrorInsertCitationResult> insertCitation()
   {
      return new Promise<PanmirrorInsertCitationResult>(
         (ResolveCallbackFn<PanmirrorInsertCitationResult> resolve, RejectCallbackFn reject) -> {  
            PanmirrorInsertCitationDialog dialog = new PanmirrorInsertCitationDialog((result) -> {
               resolve.onInvoke(result);
            });
            dialog.showModal();
         }
      );
   }
   
   public interface Resources extends ClientBundle
   {
      public interface Styles extends CssResource
      {
         String dialog();
         String spaced();
         String textArea();
         String fullWidthText();
      }

      @Source("PanmirrorDialogsStyles.css")
      Styles styles();
      
   
   }
   
   public static Resources RESOURCES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
   
   
   private GlobalDisplay globalDisplay_; 
}






