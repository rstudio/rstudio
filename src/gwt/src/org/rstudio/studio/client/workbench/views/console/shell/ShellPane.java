/*
 * ShellPane.java
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
package org.rstudio.studio.client.workbench.views.console.shell;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.shell.ShellWidget;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

public class ShellPane extends ShellWidget implements Shell.Display
{
   @Inject
   public ShellPane(final AceEditor editor, UserPrefs uiPrefs, EventBus events, AriaLiveService ariaLive)
   {
      super(editor, uiPrefs, events, ariaLive, "Console Output");

      editor.setDisableOverwrite(true);

      editor.setFileType(FileTypeRegistry.R, true);
      // Setting file type to R changes the wrap mode to false. We want it to
      // be true so that the console input can wrap.
      editor.setUseWrapMode(true);

      uiPrefs.syntaxColorConsole().bind(new CommandWithArg<Boolean>()
      {
         public void execute(Boolean arg)
         {
            Widget inputWidget = editor.getWidget();
            if (arg)
               inputWidget.removeStyleName("nocolor");
            else
               inputWidget.addStyleName("nocolor");
         }
      });

      uiPrefs.blinkingCursor().bind(new CommandWithArg<Boolean>()
      {
         public void execute(Boolean arg)
         {
            editor.setBlinkingCursor(arg);
         }
      });
   }

   @Override
   public void onBeforeUnselected()
   {
      scrollPanel_.saveScrollPosition();
   }

   @Override
   public void onBeforeSelected()
   {
   }

   @Override
   public void onSelected()
   {
      Scheduler.get().scheduleDeferred(() ->
      {
         doOnLoad();
         scrollPanel_.restoreScrollPosition();
         input_.focus();
      });
   }
}
