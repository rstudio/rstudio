/*
 * PanmirrorEditorWidget.java
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

package org.rstudio.studio.client.panmirror;


import java.util.ArrayList;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.jsinterop.JsVoidFunction;
import org.rstudio.core.client.promise.PromiseWithProgress;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.HasSelectionChangedHandlers;


public class PanmirrorEditorWidget extends Composite implements 
   RequiresResize, 
   HasChangeHandlers, 
   HasSelectionChangedHandlers
{
   public static void create(String format, 
                             PanmirrorEditorOptions options, 
                             CommandWithArg<PanmirrorEditorWidget> completed) {
      
      PanmirrorEditorWidget editorWidget = new PanmirrorEditorWidget();
      PanmirrorEditorConfig editorConfig = new PanmirrorEditorConfig(editorWidget.getElement(), format, options);
      
      Panmirror.load(() -> {
         new PromiseWithProgress<PanmirrorEditor>(
            PanmirrorEditor.create(editorConfig),
            null,
            editor -> {
               editorWidget.attachEditor(editor);
               completed.execute(editorWidget);
            }
         );
       });
     
   }
   
 
   private PanmirrorEditorWidget()
   {
      initWidget(new HTML()); 
      this.setSize("100%", "100%");
   }
   
   private void attachEditor(PanmirrorEditor editor) {
      
      this.editor_ = editor;
      
      editorEventUnsubscribe_.add(this.editor_.subscribe(Panmirror.kEventUpdate, () -> {
         DomEvent.fireNativeEvent(Document.get().createChangeEvent(), this.handlers_);
      }));
      
     
      editorEventUnsubscribe_.add(this.editor_.subscribe(Panmirror.kEventSelectionChange, () -> {
         SelectionChangeEvent.fire(this);
      }));
   }
   
   @Override
   public void onDetach()
   {
      try 
      {
         if (this.editor_ != null) 
         {
            // unsubscribe from editor events
            for (JsVoidFunction unsubscribe : editorEventUnsubscribe_) 
               unsubscribe.call();
            
            // destroy editor
            this.editor_.destroy();
            this.editor_ = null;
         }
      }
      finally
      {
         super.onDetach();
      }
   }
   
   
   public void setMarkdown(String markdown, boolean emitUpdate, CommandWithArg<Boolean> completed) {
      new PromiseWithProgress<Boolean>(
         this.editor_.setMarkdown(markdown, emitUpdate),
         false,
         completed
      );
   }
   
   public void getMarkdown(CommandWithArg<String> completed) {
      new PromiseWithProgress<String>(
         this.editor_.getMarkdown(),
         null,
         completed   
      );
   }
   
   
   @Override
   public HandlerRegistration addChangeHandler(ChangeHandler handler)
   {
      return handlers_.addHandler(ChangeEvent.getType(), handler);
   }
   
   @Override
   public HandlerRegistration addSelectionChangeHandler(SelectionChangeEvent.Handler handler)
   {
      return handlers_.addHandler(SelectionChangeEvent.getType(), handler);
   }
   
   
   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }
  

   @Override
   public void onResize()
   {
      if (this.editor_ != null) {
         this.editor_.resize();
      }
   }
    
   
   private PanmirrorEditor editor_ = null;
   private final HandlerManager handlers_ = new HandlerManager(this);
   private final ArrayList<JsVoidFunction> editorEventUnsubscribe_ = new ArrayList<JsVoidFunction>();
}






