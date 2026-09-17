/*
 * ConfigFileBacked.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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

import java.util.ArrayList;
import java.util.List;

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
                  executePending();
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  loaded_ = true;
                  loading_ = false;
                  executePending();
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

      // Queue the command until the read settles; onResponseReceived and
      // onError both mark the object loaded and flush the queue, so no
      // command is dropped however long read_config_json takes.
      pendingCommands_.add(command);
      load();
   }

   private void executePending()
   {
      List<CommandWithArg<T>> pending = new ArrayList<>(pendingCommands_);
      pendingCommands_.clear();
      for (CommandWithArg<T> command : pending)
      {
         // One loader throwing must not suppress the commands behind it.
         try
         {
            command.execute(object_);
         }
         catch (Exception e)
         {
            Debug.logException(e);
         }
      }
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
   private final List<CommandWithArg<T>> pendingCommands_ = new ArrayList<>();

   // Injected ----
   private FilesServerOperations server_;
}
