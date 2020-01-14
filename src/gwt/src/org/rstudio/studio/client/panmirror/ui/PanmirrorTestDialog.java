package org.rstudio.studio.client.panmirror.ui;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.studio.client.panmirror.Panmirror;
import org.rstudio.studio.client.panmirror.PanmirrorEditorConfig;
import org.rstudio.studio.client.panmirror.PanmirrorEditorHooks;
import org.rstudio.studio.client.panmirror.PanmirrorEditorOptions;
import org.rstudio.studio.client.panmirror.PanmirrorEditorWidget;
import org.rstudio.studio.client.panmirror.pandoc.PanmirrorPandocExtensions;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.SimplePanel;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;


public class PanmirrorTestDialog extends ModalDialog<String>
{
   public PanmirrorTestDialog()
   {
      super("Panmirror", Roles.getDialogRole(), input -> {});
      
      mainWidget_ = new SimplePanel();
      mainWidget_.setSize("500px", "400px");
      
      PanmirrorEditorConfig config = new PanmirrorEditorConfig();
      config.hooks.isEditable = () -> true;
      
      PanmirrorEditorWidget.create(config, editorWidget -> {
         if (editorWidget != null) {
            
            this.editorWidget_ = editorWidget;
            mainWidget_.add(this.editorWidget_);
            
            
            this.editorWidget_.addChangeHandler(new ChangeHandler()
            {
               @Override
               public void onChange(ChangeEvent event)
               {
                  Debug.logToConsole("Change");
               }
            }); 
            
            this.editorWidget_.addSelectionChangeHandler(new SelectionChangeEvent.Handler()
            {
               @Override
               public void onSelectionChange(SelectionChangeEvent event)
               {
                 Debug.logToConsole("SelectionChange"); 
               }
            }); 
              
            
            
            this.editorWidget_.setMarkdown(
                  "## Heading 1\n\nThe *quick* brown **fox** jumped over the lazy dog\n\n" +
                  "## Heading 2\n\nThis is the `second` section.",
                  true, (success) -> {
               if (success) 
               {  
                  
                  this.editorWidget_.execCommand(Panmirror.Commands.SelectAll);
                  
                  this.editorWidget_.getMarkdown(markdown -> {
                     Debug.logToConsole(markdown);
                  });
                  
                  PanmirrorPandocExtensions extensions = this.editorWidget_.getPandocFormat().extensions;
                  Debug.logObject(extensions);
                  
                  
                  this.editorWidget_.enableDevTools();
               }
            });
         }
      });
     
   }

   @Override
   protected String collectInput()
   {
      return "";
   }

   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }

   private SimplePanel mainWidget_;
   private PanmirrorEditorWidget editorWidget_;
}
