package org.rstudio.studio.client.common.cran;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.cran.model.CRANMirror;
import org.rstudio.studio.client.common.cran.model.CRANServerOperations;
import org.rstudio.studio.client.server.ServerDataSource;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DefaultCRANMirror 
{
   @Inject
   public DefaultCRANMirror(CRANServerOperations server,
                            GlobalDisplay globalDisplay)
   {
      server_ = server;
      globalDisplay_ = globalDisplay;
   }
   
   public void choose(OperationWithInput<CRANMirror> onChosen)
   {
      new ChooseCRANMirrorDialog(globalDisplay_, 
                                 mirrorDS_, 
                                 onChosen).showModal();
   }
   
   public void configure(final OperationWithInput<CRANMirror> onConfigured)
   {
      // show dialog
      new ChooseCRANMirrorDialog(
         globalDisplay_,  
         mirrorDS_,
         new OperationWithInput<CRANMirror>() {
            @Override
            public void execute(final CRANMirror mirror)
            {
               server_.setCRANMirror(
                  mirror,
                  new SimpleRequestCallback<Void>("Error Setting CRAN Mirror") {
                      @Override
                      public void onResponseReceived(Void response)
                      {
                         // successfully set, call onConfigured
                         onConfigured.execute(mirror);
                      }
                  });             
             }
           }).showModal();
   }
   
   
   public void ensureConfigured(final Command onConfigured)
   {
      server_.isCRANConfigured(new SimpleRequestCallback<Boolean>()
      {
         @Override
         public void onResponseReceived(Boolean response)
         {
            if (!response)
            {
               configure(new OperationWithInput<CRANMirror>() {
                  public void execute(CRANMirror mirror)
                  {
                     onConfigured.execute();       
                  }
                  
               });
            }
            else
            {
               onConfigured.execute();
            }
         }
      }); 
   }
   
   private final CRANServerOperations server_;
   
   private final GlobalDisplay globalDisplay_;
   
   private final ServerDataSource<JsArray<CRANMirror>> mirrorDS_ = 
      new ServerDataSource<JsArray<CRANMirror>>()
      {
         @Override
         public void requestData(
               ServerRequestCallback<JsArray<CRANMirror>> requestCallback)
         {
            server_.getCRANMirrors(requestCallback);
         }
    };
   
}
