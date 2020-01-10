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
public class PanmirrorEditorDialogs {
   
  
   public PanmirrorEditorDialogs() {
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
         PanmirrorEditorDialogs.this.globalDisplay_.showMessage(alertType, title, message, new Operation() {
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