/*
 * CodeNavigationTarget.java
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
package org.rstudio.core.client;

public class CodeNavigationTarget
{
   public CodeNavigationTarget(String file,
                               FilePosition pos,
                               String xref)
   {
      file_ = file;
      pos_  = pos;
      xref_ = xref;
   }
   
   public CodeNavigationTarget(String file)
   {
      this(file, null, null);
   }
   
   public CodeNavigationTarget(String file, String xref)
   {
      this(file, null, xref);
   }
   
   public CodeNavigationTarget(String file, FilePosition pos)
   {
      this(file, pos, null);
   }
   
   public String getFile()
   {
      return file_;
   }
   
   public FilePosition getPosition()
   {
      return pos_;
   }
   
   public String getXref()
   {
      return xref_;
   }
   
   private final String file_;
   private final FilePosition pos_;
   private final String xref_;
}
