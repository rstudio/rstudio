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
                               XRef xref)
   {
      file_ = file;
      pos_  = pos;
      xref_ = xref;
   }

   public CodeNavigationTarget(String file)
   {
      this(file, null, XRef.create());
   }

   public CodeNavigationTarget(String file, FilePosition pos)
   {
      this(file, pos, XRef.create());
   }

   public final String getFile()
   {
      return file_;
   }

   public final FilePosition getPosition()
   {
      return pos_;
   }

   public final XRef getXRef()
   {
      return xref_;
   }

   private final String file_;
   private final FilePosition pos_;
   private final XRef xref_;
}
