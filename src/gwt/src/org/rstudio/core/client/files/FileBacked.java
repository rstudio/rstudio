/*
 * FileBacked.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.files;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;

public class FileBacked<T extends JavaScriptObject>
{
   public FileBacked(String filePath,
                     boolean logErrorIfNotFound,
                     T defaultValue)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      filePath_ = filePath;
      logErrorIfNotFound_ = logErrorIfNotFound;
      object_ = defaultValue;
      
      loaded_ = false;
      loading_ = false;
   }
   
   @Inject
   private void initialize(FilesServerOperations server)
   {
      server_ = server;
   }
   
   public boolean isLoaded()
   {
      return loaded_;
   }
   
   public void load()
   {
      if (loading_)
         return;
      loading_ = true;
      
      server_.readJSON(
            filePath_,
            logErrorIfNotFound_,
            new ServerRequestCallback<JavaScriptObject>()
            {
               @Override
               @SuppressWarnings("unchecked")
               public void onResponseReceived(JavaScriptObject object)
               {
                  // object.cast() is sufficient on JDK 1.7, but on 1.6 
                  // the compiler doesn't like to cast from <T> (cast()) to 
                  // <T extends JavaScriptObject> (this class's template)
                  object_ = (T)object;
                  loaded_ = true;
                  loading_ = false;
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  loaded_ = true;
                  loading_ = false;
               }
            });
   }
   
   public void execute(final CommandWithArg<T> command)
   {
      if (loaded_)
      {
         command.execute(object_);
         return;
      }
      
      final Timer executionTimer = new Timer()
      {
         private int retryCount = 0;
         
         @Override
         public void run()
         {
            if (retryCount > 100)
               return;
            
            if (loading_)
            {
               retryCount++;
               schedule(DELAY_MS);
               return;
            }
            
            if (!loaded_)
            {
               load();
               schedule(DELAY_MS * 2);
               return;
            }
            
            command.execute(object_);
         }
      };
      
      executionTimer.schedule(0);
   }
   
   public void set(final T object, final Command command)
   {
      server_.writeJSON(
            filePath_,
            object,
            new ServerRequestCallback<Boolean>()
            {
               @Override
               public void onResponseReceived(Boolean success)
               {
                  object_ = object;
                  if (command != null)
                     command.execute();
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   public void set(final T object)
   {
      set(object, null);
   }
   
   private final String filePath_;
   private final boolean logErrorIfNotFound_;
   
   private boolean loaded_;
   private boolean loading_;
   private T object_;
   
   private static final int DELAY_MS = 20;
   
   // Injected ----
   private FilesServerOperations server_;
}
