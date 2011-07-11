/*
 * MRUList.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench;

import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.rstudio.core.client.DuplicateHelper;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;

import java.util.*;

@Singleton
public class MRUList
{
   @Inject
   public MRUList(Commands commands,
                  FileTypeRegistry fileTypeRegistry,
                  Session session)
   {
      commands_ = commands;
      fileTypeRegistry_ = fileTypeRegistry;
      session_ = session;
      mruCmds_ = new AppCommand[] {
            commands.mru0(),
            commands.mru1(),
            commands.mru2(),
            commands.mru3(),
            commands.mru4(),
            commands.mru5(),
            commands.mru6(),
            commands.mru7(),
            commands.mru8(),
            commands.mru9()
      };

      for (int i = 0; i < mruCmds_.length; i++)
         bindCommand(i);
      
      commands.clearRecentFiles().addHandler(new CommandHandler()
      {
         public void onCommand(AppCommand command)
         {
            clear();
         }
      });

      new JSObjectStateValue("mru", "entries", true,
                             session.getSessionInfo().getClientState(),
                             false) {
         @Override
         protected void onInit(JsObject value)
         {
            if (value != null)
            {
               JsArrayString array = value.cast();
               for (int i = 0; i < array.length(); i++)
               {
                  mruEntries_.add(array.get(i));
               }
            }
            updateCommands(false);
            dirty_ = false;
         }

         @Override
         protected JsObject getValue()
         {
            JsArrayString value = JsArrayString.createArray().cast();
            for (String entry : mruEntries_)
               value.push(entry);
            return value.cast();
         }

         @Override
         protected boolean hasChanged()
         {
            return dirty_;
         }
      };
   }

   private void bindCommand(final int i)
   {
      mruCmds_[i].addHandler(new CommandHandler()
      {
         public void onCommand(AppCommand command)
         {
            if (i < mruEntries_.size())
            {
               fileTypeRegistry_.editFile(
                     FileSystemItem.createFile(mruEntries_.get(i)));
            }
         }
      });
   }

   public void add(String entry)
   {
      assert entry.indexOf("\n") < 0;
      
      mruEntries_.remove(entry);
      mruEntries_.add(0, entry);
      updateCommands(true);
   }

   public void remove(String entry)
   {
      mruEntries_.remove(entry);
      updateCommands(true);
   }

   public void clear()
   {
      mruEntries_.clear();
      updateCommands(true);
   }

   private void updateCommands(boolean persistClientState)
   {
      while (mruEntries_.size() > mruCmds_.length)
         mruEntries_.remove(mruEntries_.size() - 1);

      commands_.clearRecentFiles().setEnabled(mruEntries_.size() > 0);

      ArrayList<String> labels = DuplicateHelper.getPathLabels(mruEntries_);

      for (int i = 0; i < mruCmds_.length; i++)
      {
         if (i >= mruEntries_.size())
            mruCmds_[i].setVisible(false);
         else
         {
            mruCmds_[i].setVisible(true);
            mruCmds_[i].setMenuLabel(labels.get(i));
            mruCmds_[i].setDesc(mruEntries_.get(i));
         }
      }
      dirty_ = true;

      if (persistClientState)
         session_.persistClientState();
   }


   private boolean dirty_;
   private AppCommand[] mruCmds_;
   private final Commands commands_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final Session session_;
   private final ArrayList<String> mruEntries_ = new ArrayList<String>();
}
