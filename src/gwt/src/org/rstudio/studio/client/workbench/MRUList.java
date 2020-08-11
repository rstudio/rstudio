/*
 * MRUList.java
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
package org.rstudio.studio.client.workbench;


import org.rstudio.core.client.DuplicateHelper;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.AppMenuItem;
import org.rstudio.core.client.command.CommandHandler;
import org.rstudio.core.client.widget.OperationWithInput;

import java.util.*;

public class MRUList
{
   public MRUList(WorkbenchList mruList,
                  AppCommand[] mruCmds,
                  AppCommand clearCommand,
                  boolean hideClearOnEmpty,
                  boolean includeExt,
                  OperationWithInput<String> operation)
   {
      clearCommand_ = clearCommand;
      mruList_ = mruList;
      mruCmds_ = mruCmds;
      hideClearOnEmpty_ = hideClearOnEmpty;
      includeExt_ = includeExt;
      operation_ = operation;


      for (int i = 0; i < mruCmds_.length; i++)
         bindCommand(i);

      clearCommand_.addHandler(new CommandHandler()
      {
         public void onCommand(AppCommand command)
         {
            clear();
         }
      });


      mruList_.addListChangedHandler(listChangedEvent ->
      {
         mruEntries_.clear();
         mruEntries_.addAll(listChangedEvent.getList());
         updateCommands();
      });
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

      mruList_.prepend(entry);
   }

   public void remove(String entry)
   {
      mruList_.remove(entry);
   }

   public void clear()
   {
      mruList_.clear();
   }

   protected ArrayList<String> generateLabels(
         ArrayList<String> entries, boolean includeExt)
   {
      return DuplicateHelper.getPathLabels(entries, includeExt);
   }

   public String getQualifiedLabel(String mruEntry)
   {
      // make a copy of the existing mru entries and prepend the specified
      // entry if it doesn't exist. we need to do this because at startup
      // the most recently loaded project may not be in the list yet
      @SuppressWarnings("unchecked")
      ArrayList<String> mruEntries = (ArrayList<String>)mruEntries_.clone();
      if (!mruEntries.contains(mruEntry))
         mruEntries.add(mruEntry);

      // save the index of the entry
      int index = mruEntries.indexOf(mruEntry);

      // transform paths
      for (int i=0; i<mruEntries.size(); i++)
         mruEntries.set(i, transformMruEntryPath(mruEntries.get(i)));

      // generate labels
      mruEntries = generateLabels(mruEntries, includeExt_);

      // return the label
      return mruEntries.get(index);
   }

   private void updateCommands()
   {
      while (mruEntries_.size() > mruCmds_.length)
         mruEntries_.remove(mruEntries_.size() - 1);

      clearCommand_.setEnabled(mruEntries_.size() > 0);
      if (hideClearOnEmpty_)
         clearCommand_.setVisible(clearCommand_.isEnabled());
      manageCommands(mruEntries_, mruCmds_);
   }

   protected void manageCommands(List<String> entries, AppCommand[] commands)
   {
      // optionally transform paths
      ArrayList<String> transformed = new ArrayList<String>();
      for (String entry : entries)
         transformed.add(transformMruEntryPath(entry));

      // generate labels
      ArrayList<String> labels = generateLabels(transformed, includeExt_);

      for (int i = 0; i < commands.length; i++)
      {
         if (i >= entries.size())
            commands[i].setVisible(false);
         else
         {
            commands[i].setVisible(true);
            commands[i].setMenuLabel(
                  AppMenuItem.escapeMnemonics(labels.get(i)));
            commands[i].setDesc(entries.get(i));
         }
      }
   }

   protected String transformMruEntryPath(String entryPath)
   {
      return entryPath;
   }

   protected ArrayList<String> getMruEntries()
   {
      return mruEntries_;
   }

   protected AppCommand[] getMruCommands()
   {
      return mruCmds_;
   }

   private final ArrayList<String> mruEntries_ = new ArrayList<String>();
   private final AppCommand[] mruCmds_;
   private final AppCommand clearCommand_;
   private final WorkbenchList mruList_;
   private final boolean hideClearOnEmpty_;
   private final boolean includeExt_;
   private final OperationWithInput<String> operation_;
}
