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

import org.rstudio.core.client.DuplicateHelper;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.AppMenuItem;
import org.rstudio.core.client.command.CommandHandler;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;

import java.util.*;

public class MRUList
{
   public MRUList(Commands commands,
                  Session session,
                  String clientStateGroup,
                  AppCommand[] mruCmds,
                  AppCommand clearCommand,
                  boolean includeExt,
                  OperationWithInput<String> operation)
   {
      commands_ = commands;
      session_ = session;
      mruCmds_ = mruCmds;
      includeExt_ = includeExt;
      operation_ = operation;
     

      for (int i = 0; i < mruCmds_.length; i++)
         bindCommand(i);
      
      clearCommand.addHandler(new CommandHandler()
      {
         public void onCommand(AppCommand command)
         {
            clear();
         }
      });

      new JSObjectStateValue(clientStateGroup, "entries", ClientState.PERSISTENT,
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
            if (dirty_)
            {
               dirty_ = false;
               return true;
            }
            return false;
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
               operation_.execute(mruEntries_.get(i));
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

      // optionally transform paths
      ArrayList<String> entries = new ArrayList<String>();
      for (String entry : mruEntries_)
         entries.add(transformMruEntryPath(entry));
      
      // generate labels
      ArrayList<String> labels = DuplicateHelper.getPathLabels(entries,
                                                               includeExt_);

      for (int i = 0; i < mruCmds_.length; i++)
      {
         if (i >= mruEntries_.size())
            mruCmds_[i].setVisible(false);
         else
         {
            mruCmds_[i].setVisible(true);
            mruCmds_[i].setMenuLabel(
                  AppMenuItem.escapeMnemonics(labels.get(i)));
            mruCmds_[i].setDesc(mruEntries_.get(i));
         }
      }
      dirty_ = true;

      if (persistClientState)
         session_.persistClientState();
   }

   
   protected String transformMruEntryPath(String entryPath)
   {
      return entryPath;
   }

   private boolean dirty_;
   private AppCommand[] mruCmds_;
   private final Commands commands_;
   private final Session session_;
   private final boolean includeExt_;
   private final ArrayList<String> mruEntries_ = new ArrayList<String>();
   private final OperationWithInput<String> operation_;
}
