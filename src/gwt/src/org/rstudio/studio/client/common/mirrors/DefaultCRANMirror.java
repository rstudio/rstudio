/*
 * DefaultCRANMirror.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.mirrors;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.StudioClientCommonConstants;
import org.rstudio.studio.client.common.mirrors.model.CRANMirror;
import org.rstudio.studio.client.common.mirrors.model.MirrorsServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class DefaultCRANMirror 
{
   @Inject
   public DefaultCRANMirror(MirrorsServerOperations server,
                            GlobalDisplay globalDisplay,
                            Provider<UserPrefs> prefs)
   {
      server_ = server;
      globalDisplay_ = globalDisplay;
      pUserPrefs_ = prefs;
   }
   
   public void choose(OperationWithInput<CRANMirror> onChosen)
   {
      new ChooseMirrorDialog(globalDisplay_, 
                             mirrorDS_, 
                             onChosen,
                             server_).showModal();
   }
   
   public void configure(final Command onConfigured)
   {
      // show dialog
      new ChooseMirrorDialog(
         globalDisplay_,  
         mirrorDS_,
         new OperationWithInput<CRANMirror>() {
            @Override
            public void execute(final CRANMirror mirror)
            {
               pUserPrefs_.get().cranMirror().setGlobalValue(mirror);
               pUserPrefs_.get().writeUserPrefs(
                  (Boolean succeeded) ->
                  {
                     if (succeeded)
                     {
                         // successfully set, call onConfigured
                         onConfigured.execute();
                     }
                     else
                     {
                        globalDisplay_.showErrorMessage(constants_.errorSettingCranMirror(),
                              constants_.cranMirrorCannotChange());
                     }
                  });
             }
           },
         server_).showModal();
   }
   
   private final MirrorsServerOperations server_;
   
   private final GlobalDisplay globalDisplay_;
   
   private final Provider<UserPrefs> pUserPrefs_;
   
   private final ChooseMirrorDialog.Source mirrorDS_ = 
      new ChooseMirrorDialog.Source() {
         
         @Override
         public String getLabel(CRANMirror mirror)
         {
            return mirror.getName() + " - " + mirror.getHost();
         }

         @Override
         public String getURL(CRANMirror mirror)
         {
            return mirror.getURL();
         }

         @Override
         public void requestData(
               ServerRequestCallback<JsArray<CRANMirror>> requestCallback)
         {
            server_.getCRANMirrors(requestCallback);
         }
    };
    private static final StudioClientCommonConstants constants_ = GWT.create(StudioClientCommonConstants.class);
}
