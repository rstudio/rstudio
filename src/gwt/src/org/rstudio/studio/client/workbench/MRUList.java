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
import org.rstudio.core.client.DuplicateHelper.DuplicationInfo;
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
   private static class CaseInsensitiveStringComparator implements Comparator<String>
   {
      public int compare(String s1, String s2)
      {
         return s1.compareToIgnoreCase(s2);
      }
   }

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

      ArrayList<String> labels = getLabels();

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

   /**
    * Use Mac OS X style prettifying of paths. Display the filename,
    * and if there are multiple entries with the same filename, append
    * a disambiguating folder to those filenames. 
    */
   private ArrayList<String> getLabels()
   {
      ArrayList<String> labels = new ArrayList<String>();
      for (String entry : mruEntries_)
         labels.add(FileSystemItem.getNameFromPath(entry));

      DuplicationInfo<String> dupeInfo = DuplicateHelper.detectDupes(
            labels, new CaseInsensitiveStringComparator());

      for (ArrayList<Integer> dupeList : dupeInfo.dupes())
      {
         fixupDupes(mruEntries_, dupeList, labels);
      }

      dupeInfo = DuplicateHelper.detectDupes(
            labels, new CaseInsensitiveStringComparator());

      // There are edge cases where we may still end up with dupes at this
      // point. In that case, just disambiguate using the full path.
      // Example:
      // ~/foo/tmp/README
      // ~/bar/tmp/README
      // ~/foo/README
      // ~/bar/README
      for (ArrayList<Integer> dupeList : dupeInfo.dupes())
      {
         for (Integer index : dupeList)
         {
            FileSystemItem fsi = FileSystemItem.createFile(
                  mruEntries_.get(index));
            labels.set(index, disambiguate(fsi.getName(),
                                           fsi.getParentPathString()));
         }
      }


      return labels;
   }

   private void fixupDupes(ArrayList<String> fullPaths,
                           ArrayList<Integer> indices,
                           ArrayList<String> labels)
   {
      ArrayList<ArrayList<String>> pathElementListList =
            new ArrayList<ArrayList<String>>();

      for (Integer index : indices)
         pathElementListList.add(toPathElements(fullPaths.get(index)));

      while (indices.size() > 0)
      {
         ArrayList<String> lastPathElements = new ArrayList<String>();

         for (int i = 0; i < pathElementListList.size(); i++)
         {
            ArrayList<String> pathElementList = pathElementListList.get(i);

            if (pathElementList.size() == 0)
            {
               int trueIndex = indices.get(i);
               String path = FileSystemItem.createFile(fullPaths.get(trueIndex))
                     .getParentPathString();
               labels.set(trueIndex,
                          disambiguate(labels.get(trueIndex), path));

               indices.remove(i);
               pathElementListList.remove(i);
               i--;
            }
            else
            {
               lastPathElements.add(
                     pathElementList.remove(pathElementList.size() - 1));
            }
         }


         DuplicationInfo<String> dupeInfo = DuplicateHelper.detectDupes(
               lastPathElements,
               new CaseInsensitiveStringComparator());

         for (int i = 0; i < lastPathElements.size(); i++)
         {
            if (1 == dupeInfo.occurrences(lastPathElements.get(i)))
            {
               int trueIndex = indices.get(i);
               labels.set(trueIndex, disambiguate(labels.get(trueIndex),
                                          lastPathElements.get(i)));

               indices.remove(i);
               pathElementListList.remove(i);
               lastPathElements.remove(i);
               i--;
            }
         }

         assert indices.size() == pathElementListList.size();
      }
   }

   private String disambiguate(String filename, String disambiguatingPath)
   {
      return filename + " \u2014 " + disambiguatingPath;
   }

   private ArrayList<String> toPathElements(String path)
   {
      FileSystemItem fsi = FileSystemItem.createFile(path);
      return new ArrayList<String>(
            Arrays.asList(fsi.getParentPathString().split("/")));
   }


   private boolean dirty_;
   private AppCommand[] mruCmds_;
   private final Commands commands_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final Session session_;
   private final ArrayList<String> mruEntries_ = new ArrayList<String>();
}
