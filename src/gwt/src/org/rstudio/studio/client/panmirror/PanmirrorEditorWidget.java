package org.rstudio.studio.client.panmirror;


import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;

import elemental2.promise.IThenable;
import elemental2.promise.IThenable.ThenOnFulfilledCallbackFn;
import elemental2.promise.Promise;
import elemental2.promise.Promise.CatchOnRejectedCallbackFn;


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
                  command.execute(editorWidget);
                  return Promise.resolve(editor);
               }
            })
            .catch_(new CatchOnRejectedCallbackFn<PanmirrorEditor>() {
   
               @Override
               public IThenable<PanmirrorEditor> onInvoke(Object error)
               {
                  Debug.logToConsole("Error creating Panmirror widget: " + error.toString());
                  command.execute(editorWidget);
                  return null;
               }
            });
       });
     
   }
   
   
   private PanmirrorEditorWidget()
   {
      initWidget(new HTML());
      setSize("100%", "100%");
   }
   
   public PanmirrorEditor getEditor() {
      return this.editor_;
   }

   @Override
   public void onResize()
   {
            
   }
   
   private PanmirrorEditor editor_ = null;
}


