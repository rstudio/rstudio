/*
 * FindResultContext.java
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
package org.rstudio.studio.client.workbench.views.output.find;

import com.google.gwt.view.client.AbstractDataProvider;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import org.rstudio.studio.client.workbench.views.output.find.model.FindResult;

import java.util.HashMap;
import java.util.ArrayList;

public class FindResultContext
{
   public class File
   {
      public File(String path)
      {
         path_ = path;
      }

      public String getPath()
      {
         return path_;
      }

      public void addMatch(int line, int column, String value)
      {
         int lineWidth = (line + "").length();
         maxLineWidth_ = Math.max(lineWidth, maxLineWidth_);
         matchData_.getList().add(new Match(this, line, column, value));
      }

      public int getCount()
      {
         return matchData_.getList().size();
      }

      public AbstractDataProvider<Match> getDataProvider()
      {
         return matchData_;
      }

      public void refresh()
      {
         matchData_.refresh();
      }

      public void clear()
      {
         matchData_.getList().clear();
      }

      private final String path_;
      private final ListDataProvider<Match> matchData_ =
            new ListDataProvider<>(new ProvidesKey<Match>()
            {
               @Override
               public Object getKey(Match item)
               {
                  return item.getParent().getPath() + ":" + item.getLine();
               }
            });
   }

   public class Match
   {
      public Match(File parent, int line, int column, String value)
      {
         parent_ = parent;
         line_ = line;
         column_ = column;
         value_ = value;
      }

      public File getParent()
      {
         return parent_;
      }

      public int getLine()
      {
         return line_;
      }

      public int getColumn()
      {
         return column_;
      }

      public String getValue()
      {
         return value_;
      }

      public void setReplace(String value)
      {
         replace_ = value;
      }

      public String getReplace()
      {
         return replace_;
      }

      private final File parent_;
      private final int line_;
      private final int column_;
      private final String value_;
      private String replace_;
   }

   FindResultContext()
   {
      findResults_ = new ArrayList<>();
   }

   private File getFile(String path)
   {
      File file = filesByName_.get(path);
      if (file == null)
      {
         file = new File(path);
         data_.getList().add(file);
         filesByName_.put(path, file);
      }
      return file;
   }

   public int getMaxLineWidth()
   {
      return maxLineWidth_;
   }

   public AbstractDataProvider<File> getDataProvider()
   {
      return data_;
   }

   public void reset()
   {
      data_.getList().clear();
      findResults_.clear();
      filesByName_.clear();
      maxLineWidth_ = 0;
   }

   public void addMatches(Iterable<FindResult> findResults)
   {
      int origMaxLineWidth = maxLineWidth_;

      for (FindResult fr : findResults)
      {
         File file = getFile(fr.getFile());

         file.addMatch(fr.getLine(), 0, fr.getLineValue());

         int index = data_.getList().indexOf(file);
         if (index >= 0) // not that we are expecting otherwise...
            data_.getList().set(index, file);
         findResults_.add(fr.clone());
      }

      if (maxLineWidth_ != origMaxLineWidth)
      {
         for (File aFile : data_.getList())
            aFile.refresh();
      }
   }

   public void updateFileMatches(String replace)
   {
      for (FindResult fr : findResults_)
         fr.setReplace(replace);
   }

   public ArrayList<FindResult> getFindResults()
   {
      return findResults_;
   }

   private final ListDataProvider<File> data_ = new ListDataProvider<>(new ProvidesKey<File>()
   {
      @Override
      public Object getKey(File item)
      {
         return item.getPath();
      }
   });
   private final ArrayList<FindResult> findResults_;
   private final HashMap<String, File> filesByName_ = new HashMap<>();
   private int maxLineWidth_;
}
