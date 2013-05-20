/*
 * ImportFileSettings.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.environment.dataimport;

import org.rstudio.core.client.files.FileSystemItem;

public class ImportFileSettings
{
   public ImportFileSettings(FileSystemItem file,
                             String varname,
                             boolean header,
                             String sep,
                             String decimal,
                             String quote)
   {
      file_ = file;
      varname_ = varname;
      header_ = header;
      sep_ = sep;
      decimal_ = decimal;
      quote_ = quote;
   }

   public FileSystemItem getFile()
   {
      return file_;
   }

   public String getVarname()
   {
      return varname_;
   }

   public boolean isHeader()
   {
      return header_;
   }

   public String getSep()
   {
      return sep_;
   }

   public String getDec()
   {
      return decimal_;
   }

   public String getQuote()
   {
      return quote_;
   }

   public int calculateSimilarity(ImportFileSettings other)
   {
      int score = 0;
      if (isHeader() == other.isHeader())
         score++;
      if (getSep().equals(other.getSep()))
         score += 2;
      if (getDec().equals(other.getDec()))
         score += 2;
      if (getQuote().equals(other.getQuote()))
         score++;
      return score;
   }

   private final FileSystemItem file_;
   private final String varname_;
   private final boolean header_;
   private final String sep_;
   private final String decimal_;
   private final String quote_;
}
