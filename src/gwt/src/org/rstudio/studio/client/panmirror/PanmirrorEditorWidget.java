package org.rstudio.studio.client.panmirror;


import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;

import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;

import elemental2.promise.IThenable;
import elemental2.promise.IThenable.ThenOnFulfilledCallbackFn;
import elemental2.promise.IThenable.ThenOnRejectedCallbackFn;
import elemental2.promise.Promise;


public class PanmirrorEditorWidget extends Composite implements RequiresResize
{
   public static void create(String format, 
                             PanmirrorEditorOptions options, 
                             CommandWithArg<PanmirrorEditorWidget> command) {
      
      PanmirrorEditorWidget editorWidget = new PanmirrorEditorWidget();
      PanmirrorEditorConfig editorConfig = new PanmirrorEditorConfig(editorWidget.getElement(), format, options);
      
      Panmirror.load(() -> {
         PanmirrorEditor.create(editorConfig)
            .then(new ThenOnFulfilledCallbackFn<PanmirrorEditor, PanmirrorEditor>() {
               @Override
               public IThenable<PanmirrorEditor> onInvoke(PanmirrorEditor editor)
               {
                  editorWidget.attachEditor(editor);
                  command.execute(editorWidget);
                  return Promise.resolve(editor);
               }
            }, new ThenOnRejectedCallbackFn<PanmirrorEditor>() {
               @Override
               public IThenable<PanmirrorEditor> onInvoke(Object error)
               {
                  Debug.logToConsole("Error creating Panmirror widget: " + error.toString());
                  command.execute(null);
                  return null;
               }
               
            });
       });
     
   }
   
   
   private PanmirrorEditorWidget()
   {
      initWidget(new HTML());
      setSize("100%", "100%");
      this.getElement().getStyle().setPosition(Position.RELATIVE);
      
   }
   
   private void attachEditor(PanmirrorEditor editor) {
      this.editor_ = editor;
   }
   
   public void setMarkdown(String markdown, boolean emitUpdate, Command completed) {
      
      this.editor_.setMarkdown(markdown, emitUpdate)
         .then(new ThenOnFulfilledCallbackFn<Object,Object>() {
            @Override
            public IThenable<Object> onInvoke(Object v)
            {
               completed.execute();
               return null;
              
            }
         },new ThenOnRejectedCallbackFn<String>() {
         
            @Override
            public IThenable<String> onInvoke(Object error)
            {
               Debug.logToConsole("Error setting Panmirorr markdown: " + error.toString());
               completed.execute();
               return null;
            }
         });
        
         
   }
   
   public void getMarkdown(CommandWithArg<String> command) {
      this.editor_.getMarkdown()
         .then(new ThenOnFulfilledCallbackFn<String, String>() {
            @Override
            public IThenable<String> onInvoke(String markdown)
            {
               command.execute(markdown);
               return Promise.resolve(markdown);
            }
         },new ThenOnRejectedCallbackFn<String>() {
         
            @Override
            public IThenable<String> onInvoke(Object error)
            {
               Debug.logToConsole("Error getting Panmirror markdown: " + error.toString());
               command.execute("");
               return null;
            }
         });
   }
   
   

   @Override
   public void onResize()
   {
            
   }
   
   private PanmirrorEditor editor_ = null;
}


