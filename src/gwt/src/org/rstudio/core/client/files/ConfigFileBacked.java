/*
 * ConfigFileBacked.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;

public class ConfigFileBacked<T extends JavaScriptObject>
{
   /**
    * Constructs a new config file-backed object.
    * 
    * @param server The remote server.
    * @param relativePath The path to the config file, relative to the root
    *   configuration directory.
    * @param logErrorIfNotFound Whether to log an error if the config file 
    *   isn't found.
    * @param defaultValue The default value if no file is located.
    */
   public ConfigFileBacked(FilesServerOperations server,
                           String relativePath,
                           boolean logErrorIfNotFound,
                           T defaultValue)
   {
      server_ = server;
      relativePath_ = relativePath;
      logErrorIfNotFound_ = logErrorIfNotFound;
      object_ = defaultValue;
      
      loaded_ = false;
      loading_ = false;
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
      
      server_.readConfigJSON(
            relativePath_,
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
      server_.writeConfigJSON(
            relativePath_,
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
   
   private final String relativePath_;
   private final boolean logErrorIfNotFound_;
   
   private boolean loaded_;
   private boolean loading_;
   private T object_;
   
   private static final int DELAY_MS = 20;
   
   // Injected ----
   private FilesServerOperations server_;
}
