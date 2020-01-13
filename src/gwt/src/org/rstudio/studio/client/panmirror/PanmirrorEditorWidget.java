package org.rstudio.studio.client.panmirror;


import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.promise.PromiseWithProgress;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;


public class PanmirrorEditorWidget extends Composite implements RequiresResize
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
   public void onResize()
   {
      if (this.editor_ != null) {
         this.editor_.resize();
      }
   }
    
   
   private PanmirrorEditor editor_ = null;
}






