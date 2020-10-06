/*
 * PanmirrorDialogs.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
import org.rstudio.core.client.jsinterop.JsVoidFunction;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorCodeBlockProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorImageDimensions;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrEditResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorImageProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCiteProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCiteResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertTableResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkCapabilities;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkEditResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkTargets;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorListCapabilities;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorListProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorRawFormatProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorRawFormatResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorTableCapabilities;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIContext;

import com.google.inject.Inject;

import elemental2.promise.Promise;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.RejectCallbackFn;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.ResolveCallbackFn;
import jsinterop.annotations.JsType;



@JsType
public class PanmirrorDialogs {
   
   public static class AlertType
   {
      public static final int Info = 1;
      public static final int Warning = 2;
      public static final int Error = 3;
   }
  
   public PanmirrorDialogs(PanmirrorUIContext uiContext) {
      this.uiContext_ = uiContext;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(GlobalDisplay globalDisplay)
   {
      this.globalDisplay_ = globalDisplay;
   }
   
   public Promise<Boolean> alert(String message, String title, int type) 
   {
      return new Promise<Boolean>((ResolveCallbackFn<Boolean> resolve, RejectCallbackFn reject) -> {
         int alertType = MessageDisplay.MSG_INFO;
         switch(type) {
            case AlertType.Info:
              alertType = MessageDisplay.MSG_INFO;
              break;
            case AlertType.Warning:
              alertType = MessageDisplay.MSG_WARNING;
              break;
            case AlertType.Error:
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
   
   
   public Promise<Boolean> yesNoMessage(String message, String title, int type, String yesLabel, String noLabel) 
   {
      return new Promise<Boolean>((ResolveCallbackFn<Boolean> resolve, RejectCallbackFn reject) -> {
         int alertType = MessageDisplay.MSG_INFO;
         switch(type) {
            case AlertType.Info:
              alertType = MessageDisplay.MSG_INFO;
              break;
            case AlertType.Warning:
              alertType = MessageDisplay.MSG_WARNING;
              break;
            case AlertType.Error:
              alertType = MessageDisplay.MSG_ERROR;
              break;
         }
         
         PanmirrorDialogs.this.globalDisplay_.showYesNoMessage(alertType, 
                                                               title, 
                                                               message, 
                                                               false, 
                                                               new Operation() {
                                                                  @Override
                                                                  public void execute()
                                                                  {
                                                                     resolve.onInvoke(true);    
                                                                  }        
                                                               }, 
                                                               new Operation() {
                                                                  @Override
                                                                  public void execute()
                                                                  {
                                                                     resolve.onInvoke(false);    
                                                                  }
                                                               },
                                                               new Operation() {
                                                                  @Override
                                                                  public void execute()
                                                                  {
                                                                     resolve.onInvoke(false);    
                                                                  }
                                                               }, 
                                                               yesLabel,
                                                               noLabel,
                                                               true
         );    
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
            dialog.showModal(false);
         }   
      );
   }
   
   public Promise<PanmirrorImageProps> editImage(PanmirrorImageProps image, PanmirrorImageDimensions dims, boolean editAttributes)
   {
      return new Promise<PanmirrorImageProps>(
         (ResolveCallbackFn<PanmirrorImageProps> resolve, RejectCallbackFn reject) -> {  
            PanmirrorEditImageDialog dialog = new PanmirrorEditImageDialog(image, dims, editAttributes, uiContext_,
               (result) -> { resolve.onInvoke(result); }
            );
            dialog.showModal(false);
         }
      );      
   }
   
   public Promise<PanmirrorCodeBlockProps> editCodeBlock(PanmirrorCodeBlockProps codeBlock, boolean attributes, String[] languages)
   {
      return new Promise<PanmirrorCodeBlockProps>(
         (ResolveCallbackFn<PanmirrorCodeBlockProps> resolve, RejectCallbackFn reject) -> {  
            PanmirrorEditCodeBlockDialog dialog = new PanmirrorEditCodeBlockDialog(codeBlock, attributes, languages,
               (result) -> { resolve.onInvoke(result); }
            );
            dialog.showModal(false);
         }
      );   
   }
   
   public Promise<PanmirrorListProps> editList(PanmirrorListProps props, 
                                               PanmirrorListCapabilities capabilities)
   {
      return new Promise<PanmirrorListProps>(
         (ResolveCallbackFn<PanmirrorListProps> resolve, RejectCallbackFn reject) -> {  
            PanmirrorEditListDialog dialog = new PanmirrorEditListDialog(props, capabilities,
               (result) -> { resolve.onInvoke(result); }
            );
            dialog.showModal(false);
         }
      );
   }
   
   public Promise<PanmirrorAttrEditResult> editAttr(PanmirrorAttrProps attr, String idHint)
   {
      return editPanmirrorAttr("Edit Attributes", null, idHint, attr);
   }

   
   public Promise<PanmirrorAttrEditResult> editSpan(PanmirrorAttrProps attr)
   {
      return editPanmirrorAttr("Span Attributes", "Unwrap Span", null, attr);
   }
   
   public Promise<PanmirrorAttrEditResult> editDiv(PanmirrorAttrProps attr, boolean removeEnabled)
   {
      return editPanmirrorAttr("Div Attributes", removeEnabled ? "Unwrap Div" : null, null, attr);
   }


   private Promise<PanmirrorAttrEditResult> editPanmirrorAttr(String caption, String removeButtonCaption, String idHint, PanmirrorAttrProps attr) 
   {
      return new Promise<PanmirrorAttrEditResult>(
         (ResolveCallbackFn<PanmirrorAttrEditResult> resolve, RejectCallbackFn reject) -> {  
            PanmirrorEditAttrDialog dialog = new PanmirrorEditAttrDialog(caption, removeButtonCaption, idHint, attr, 
               (result) -> { resolve.onInvoke(result); }
            );
            dialog.showModal(false);
         }
      );
   }
   
   
   public Promise<PanmirrorRawFormatResult> editRawInline(PanmirrorRawFormatProps raw, String[] outputFormats) 
   {
      return editRaw(raw, outputFormats, true);
   }
   
   public Promise<PanmirrorRawFormatResult> editRawBlock(PanmirrorRawFormatProps raw, String[] outputFormats) 
   {
      return editRaw(raw, outputFormats, false);
   }
   

   private Promise<PanmirrorRawFormatResult> editRaw(PanmirrorRawFormatProps raw, String[] outputFormats, boolean inline)
   {
      return new Promise<PanmirrorRawFormatResult>(
         (ResolveCallbackFn<PanmirrorRawFormatResult> resolve, RejectCallbackFn reject) -> {  
            PanmirrorEditRawDialog dialog = new PanmirrorEditRawDialog(raw, outputFormats, inline, 
               (result) -> { resolve.onInvoke(result); }
            );
            dialog.showModal(false);
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
            dialog.showModal(false);
         }
      );
   }
   
   public Promise<PanmirrorInsertCiteResult> insertCite(PanmirrorInsertCiteProps citeProps)
   {
      return new Promise<PanmirrorInsertCiteResult>(
         (ResolveCallbackFn<PanmirrorInsertCiteResult> resolve, RejectCallbackFn reject) -> {  
            PanmirrorInsertCiteDialog dialog = new PanmirrorInsertCiteDialog(citeProps, (result) -> {
               resolve.onInvoke(result);
            });
            dialog.showModal(false);
         }
      );
   }
   
   public Promise<Boolean> htmlDialog(String title, String okText, 
                                      PanmirrorHTMLDialog.CreateFn create,
                                      JsVoidFunction focus,
                                      PanmirrorHTMLDialog.ValidateFn validate)
   {
      return new Promise<Boolean>((ResolveCallbackFn<Boolean> resolve, RejectCallbackFn reject) -> {
         PanmirrorHTMLDialog dialog = new PanmirrorHTMLDialog(title, okText, create, focus, validate, (result) -> {
            resolve.onInvoke(result);
         });
         dialog.showModal();   
      });
   }
   
   private GlobalDisplay globalDisplay_; 
   private PanmirrorUIContext uiContext_;
}






