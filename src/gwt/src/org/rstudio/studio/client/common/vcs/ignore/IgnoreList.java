/*
 * IgnoreList.java
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
package org.rstudio.studio.client.common.vcs.ignore;

import java.util.ArrayList;

public class IgnoreList
{
   public IgnoreList(String path, ArrayList<String> files)
   {
      path_ = path;
      files_ = files;
   }
   
   public String getPath() { return path_; }
   public ArrayList<String> getFiles() { return files_; }
   
   private final String path_;
   private final ArrayList<String> files_;
}
