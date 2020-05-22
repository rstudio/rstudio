/*
 * ImportFileSettings.java
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
package org.rstudio.studio.client.workbench.views.environment.dataimport;

import org.rstudio.core.client.files.FileSystemItem;

public class ImportFileSettings
{
   public ImportFileSettings(FileSystemItem file,
                             String varname,
                             String encoding,
                             boolean header,
                             String rowNames,
                             String sep,
                             String decimal,
                             String quote,
                             String comment,
                             String naStrings,
                             boolean stringsAsFactors)
   {
      file_ = file;
      varname_ = varname;
      encoding_ = encoding;
      header_ = header;
      rowNames_ = rowNames;
      sep_ = sep;
      decimal_ = decimal;
      quote_ = quote;
      comment_ = comment;
      naStrings_ = naStrings;
      stringsAsFactors_ = stringsAsFactors;
   }

   public FileSystemItem getFile()
   {
      return file_;
   }

   public String getVarname()
   {
      return varname_;
   }
   
   public String getEncoding()
   {
      return encoding_;
   }

   public boolean isHeader()
   {
      return header_;
   }
   
   public String getRowNames()
   {
      return rowNames_;
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
   
   public String getComment()
   {
      return comment_;
   }
   
   public String getNAStrings()
   {
      return naStrings_;
   }
   
   public boolean getStringsAsFactors()
   {
      return stringsAsFactors_;
   }

   public int calculateSimilarity(ImportFileSettings other)
   {
      int score = 0;
      if (isHeader() == other.isHeader())
         score++;
      if (getRowNames() == other.getRowNames())
         score++;
      if (getSep() == other.getSep())
         score += 2;
      if (getDec() == other.getDec())
         score += 2;
      if (getQuote() == other.getQuote())
         score++;
      if (getComment() == other.getComment())
         score++;
      return score;
   }

   private final FileSystemItem file_;
   private final String varname_;
   private final String encoding_;
   private final boolean header_;
   private final String rowNames_;
   private final String sep_;
   private final String decimal_;
   private final String quote_;
   private final String comment_;
   private final String naStrings_;
   private final boolean stringsAsFactors_;
}
