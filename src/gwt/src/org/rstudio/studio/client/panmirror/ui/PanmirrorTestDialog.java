package org.rstudio.studio.client.panmirror.ui;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.studio.client.panmirror.PanmirrorEditorOptions;
import org.rstudio.studio.client.panmirror.PanmirrorEditorWidget;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.SimplePanel;

import com.google.gwt.user.client.ui.Widget;

// TODO: how to handle error conditions? confirm all the flow of control actually works

public class PanmirrorTestDialog extends ModalDialog<String>
{
   public PanmirrorTestDialog()
   {
      super("Panmirror", Roles.getDialogRole(), input -> {});
      
      mainWidget_ = new SimplePanel();
      mainWidget_.setSize("500px", "400px");
      
      PanmirrorEditorOptions options = new PanmirrorEditorOptions();
      PanmirrorEditorWidget.create("markdown", options, editorWidget -> {
         if (editorWidget != null) {
            this.editorWidget_ = editorWidget;
            mainWidget_.add(this.editorWidget_);
            this.editorWidget_.setMarkdown("The *quick* brown **fox** jumped over the lazy dog", true, (success) -> {
               if (success) 
               {  
                  this.editorWidget_.getMarkdown(markdown -> {
                     Debug.logToConsole(markdown);
                  });
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
