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
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrEditResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorImageProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCitationResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertTableResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkCapabilities;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkEditResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkTargets;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorListCapabilities;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorOrderedListProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorRawFormatProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorRawFormatResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorTableCapabilities;

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

   public Promise<PanmirrorLinkEditResult> editLink(
      PanmirrorLinkProps link, PanmirrorLinkTargets targets, PanmirrorLinkCapabilities capabilities)
   {
      return new Promise<PanmirrorLinkEditResult>(
         (ResolveCallbackFn<PanmirrorLinkEditResult> resolve, RejectCallbackFn reject) -> {  
            PanmirrorEditLinkDialog dialog = new PanmirrorEditLinkDialog(link, targets, capabilities,
               (result) -> { resolve.onInvoke(result); }
            );
            dialog.showModal();
         }   
      );
   }
   
   public Promise<PanmirrorImageProps> editImage(PanmirrorImageProps image, boolean editAttributes)
   {
      return new Promise<PanmirrorImageProps>(
         (ResolveCallbackFn<PanmirrorImageProps> resolve, RejectCallbackFn reject) -> {  
            PanmirrorEditImageDialog dialog = new PanmirrorEditImageDialog(image, editAttributes,
               (result) -> { resolve.onInvoke(result); }
            );
            dialog.showModal();
         }
      );      
   }
   
   public Promise<PanmirrorOrderedListProps> editOrderedList(PanmirrorOrderedListProps props, 
                                                             PanmirrorListCapabilities capabilities)
   {
      return new Promise<PanmirrorOrderedListProps>(
         (ResolveCallbackFn<PanmirrorOrderedListProps> resolve, RejectCallbackFn reject) -> {  
            PanmirrorEditOrderedListDialog dialog = new PanmirrorEditOrderedListDialog(props, capabilities,
               (result) -> { resolve.onInvoke(result); }
            );
            dialog.showModal();
         }
      );
   }
   
   public Promise<PanmirrorAttrEditResult> editAttr(PanmirrorAttrProps attr)
   {
      return editPanmirrorAttr("Edit Attributes", false, attr);
   }

   
   public Promise<PanmirrorAttrEditResult> editSpan(PanmirrorAttrProps attr)
   {
      return editPanmirrorAttr("Span Attributes", true, attr);
   }
   
   public Promise<PanmirrorAttrEditResult> editDiv(PanmirrorAttrProps attr, boolean removeEnabled)
   {
      return editPanmirrorAttr("Section/Div Attributes", removeEnabled, attr);
   }


   private Promise<PanmirrorAttrEditResult> editPanmirrorAttr(String caption, boolean removeEnabled, PanmirrorAttrProps attr) 
   {
      return new Promise<PanmirrorAttrEditResult>(
         (ResolveCallbackFn<PanmirrorAttrEditResult> resolve, RejectCallbackFn reject) -> {  
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
   
   public Promise<PanmirrorInsertTableResult> insertTable(PanmirrorTableCapabilities capabilities)
   {
      return new Promise<PanmirrorInsertTableResult>(
         (ResolveCallbackFn<PanmirrorInsertTableResult> resolve, RejectCallbackFn reject) -> {  
            PanmirrorInsertTableDialog dialog = new PanmirrorInsertTableDialog(capabilities, (result) -> {
               resolve.onInvoke(result);
            });
            dialog.showModal();
         }
      );
   }
   
   
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

   
   private GlobalDisplay globalDisplay_; 
}






