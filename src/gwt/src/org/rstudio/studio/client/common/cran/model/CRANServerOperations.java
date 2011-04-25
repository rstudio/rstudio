package org.rstudio.studio.client.common.cran.model;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

import com.google.gwt.core.client.JsArray;

public interface CRANServerOperations
{
   void setCRANMirror(CRANMirror mirror,
                      ServerRequestCallback<Void> requestCallback);
   
   void getCRANMirrors(
         ServerRequestCallback<JsArray<CRANMirror>> requestCallback);
}
