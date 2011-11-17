package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;

public interface SVNServerOperations extends CryptoServerOperations
{
   void svnAdd(JsArrayString paths,
               ServerRequestCallback<Void> requestCallback);

   void svnDelete(JsArrayString paths,
                  ServerRequestCallback<Void> requestCallback);

   void svnRevert(JsArrayString paths,
                  ServerRequestCallback<Void> requestCallback);

   void svnStatus(
         ServerRequestCallback<JsArray<StatusAndPath>> requestCallback);

   void svnDiff(String path,
                ServerRequestCallback<String> requestCallback);
}
