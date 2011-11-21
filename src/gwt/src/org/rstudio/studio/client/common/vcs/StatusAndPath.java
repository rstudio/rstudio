/*
 * StatusAndPath.java
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
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JsArray;

import java.util.ArrayList;

public class StatusAndPath
{
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
      status_ = info.getStatus();
      path_ = info.getPath();
      rawPath_ = info.getRawPath();
      this.discardable_ = info.isDiscardable();
      this.directory_ = info.isDirectory();
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

   public boolean isDiscardable()
   {
      return discardable_;
   }

   public boolean isDirectory()
   {
      return directory_;
   }

   public boolean isFineGrainedActionable()
   {
      return !"??".equals(getStatus());
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

   String status_;
   String path_;
   String rawPath_;
   boolean discardable_;
   boolean directory_;
}