/*
 * StatusAndPath.java
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
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JsArray;
import org.rstudio.core.client.StringUtil;

import java.util.ArrayList;
import java.util.Comparator;

public class StatusAndPath
{
   public static class PathComparator implements Comparator<StatusAndPath>
   {
      private String[] splitDirAndName(String path)
      {
         int index = path.lastIndexOf("/");
         if (index < 0)
            index = path.lastIndexOf("\\");
         if (index < 0)
            return new String[] { "", path };
         else
            return new String[] { path.substring(0, index),
                                  path.substring(index + 1) };
      }

      @Override
      public int compare(StatusAndPath a, StatusAndPath b)
      {
         String[] splitA = splitDirAndName(a.getPath());
         String[] splitB = splitDirAndName(b.getPath());
         int result = splitA[0].compareTo(splitB[0]);
         if (result == 0)
            result = splitA[1].compareTo(splitB[1]);
         return result;
      }
   }

   public static ArrayList<StatusAndPath> fromInfos(
         JsArray<StatusAndPathInfo> infos)
   {
      if (infos == null)
         return null;

      ArrayList<StatusAndPath> result = new ArrayList<StatusAndPath>();
      for (int i = 0; i < infos.length(); i++)
      {
         result.add(new StatusAndPath(infos.get(i)));
      }
      return result;
   }

   public static StatusAndPath fromInfo(StatusAndPathInfo info)
   {
      return info == null ? null : new StatusAndPath(info);
   }

   private StatusAndPath(StatusAndPathInfo info)
   {
      status_ = StringUtil.notNull(info.getStatus());
      path_ = info.getPath();
      rawPath_ = info.getRawPath();
      changelist_ = info.getChangelist();
      discardable_ = info.isDiscardable();
      directory_ = info.isDirectory();
   }

   public String getStatus()
   {
      return status_;
   }

   public String getPath()
   {
      return path_;
   }

   public String getRawPath()
   {
      return rawPath_;
   }

   public String getChangelist()
   {
      return changelist_;
   }

   public boolean isDiscardable()
   {
      return discardable_;
   }

   public boolean isDirectory()
   {
      return directory_;
   }

   @Override
   public boolean equals(Object o)
   {
      StatusAndPath that = (StatusAndPath) o;
      return path_ != null ? path_.equals(that.path_) : that.path_ == null;
   }

   @Override
   public int hashCode()
   {
      return path_ != null ? path_.hashCode() : 0;
   }

   public StatusAndPathInfo toInfo()
   {
      return StatusAndPathInfo.create(status_,
                                      path_,
                                      rawPath_,
                                      discardable_,
                                      directory_);
   }

   private final String status_;
   private final String path_;
   private final String rawPath_;
   private final boolean discardable_;
   private final boolean directory_;
   private final String changelist_;
}
