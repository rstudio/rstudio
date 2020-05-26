/*
 * CodeSearchLauncher.java
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

package org.rstudio.studio.client.application.ui;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.workbench.codesearch.CodeSearch;
import org.rstudio.studio.client.workbench.codesearch.ui.CodeSearchDialog;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class CodeSearchLauncher
{
   public interface Binder
           extends CommandBinder<Commands, CodeSearchLauncher> {}
   
   @Inject 
   public CodeSearchLauncher(Provider<CodeSearch> pCodeSearch,
         Commands commands,
         Binder binder)
   {
      pCodeSearch_ = pCodeSearch;
      binder.bind(commands, this);
   }
    
   @Handler
   public void onGoToFileFunction()
   {
      if (dialog_ != null)
         dialog_.closeDialog();
      dialog_ = new CodeSearchDialog(pCodeSearch_);
      dialog_.showModal();
   }
   
   private final Provider<CodeSearch> pCodeSearch_;
   private CodeSearchDialog dialog_;
}
