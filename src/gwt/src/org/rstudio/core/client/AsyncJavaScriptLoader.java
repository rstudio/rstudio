/*
 * AsyncJavaScriptLoader.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.core.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.Command;

/**
 * Loads a collection of JavaScript files, organized into stages: the stages
 * load one after another, and the scripts within a stage load in parallel.
 * Each call to add() contributes one stage.
 *
 * The underlying ExternalJavaScriptLoaders cache their result, so a loader
 * can be executed any number of times (and different AsyncJavaScriptLoaders
 * can share ExternalJavaScriptLoaders); scripts that have already loaded
 * complete immediately. The onFinished handlers run on every successful
 * execution, before that execution's own completion command.
 *
 * NOTE: as with ExternalJavaScriptLoader, a script that fails to load stalls
 * the execution silently: neither the onFinished handlers nor the completion
 * command run.
 */
public class AsyncJavaScriptLoader
{
   public AsyncJavaScriptLoader add(ExternalJavaScriptLoader... loaders)
   {
      List<ExternalJavaScriptLoader> stage = new ArrayList<>();
      for (ExternalJavaScriptLoader loader : loaders)
         stage.add(loader);

      if (!stage.isEmpty())
         stages_.add(stage);

      return this;
   }

   public AsyncJavaScriptLoader add(String... urls)
   {
      ExternalJavaScriptLoader[] loaders = new ExternalJavaScriptLoader[urls.length];
      for (int i = 0; i < urls.length; i++)
         loaders[i] = new ExternalJavaScriptLoader(urls[i]);

      return add(loaders);
   }

   public AsyncJavaScriptLoader onFinished(Command command)
   {
      onFinished_.add(command);
      return this;
   }

   public boolean isLoaded()
   {
      for (List<ExternalJavaScriptLoader> stage : stages_)
         for (ExternalJavaScriptLoader loader : stage)
            if (!loader.isLoaded())
               return false;

      return true;
   }

   public void execute()
   {
      execute(null);
   }

   public void execute(Command onCompleted)
   {
      executeStage(0, onCompleted);
   }

   private void executeStage(int index, Command onCompleted)
   {
      if (index == stages_.size())
      {
         for (Command command : onFinished_)
            command.execute();

         if (onCompleted != null)
            onCompleted.execute();

         return;
      }

      List<ExternalJavaScriptLoader> stage = stages_.get(index);

      // the counter is local so that concurrent executions don't share state;
      // the underlying loaders coalesce the actual script loads
      final int[] pending = { stage.size() };
      ExternalJavaScriptLoader.Callback onLoaded = () ->
      {
         pending[0] -= 1;
         if (pending[0] == 0)
            executeStage(index + 1, onCompleted);
      };

      for (ExternalJavaScriptLoader loader : stage)
         loader.addCallback(onLoaded);
   }

   private final List<List<ExternalJavaScriptLoader>> stages_ = new ArrayList<>();
   private final List<Command> onFinished_ = new ArrayList<>();
}
