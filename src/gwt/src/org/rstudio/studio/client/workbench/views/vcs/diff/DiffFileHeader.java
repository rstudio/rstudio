package org.rstudio.studio.client.workbench.views.vcs.diff;

import java.util.ArrayList;

public class DiffFileHeader
{
   public DiffFileHeader(ArrayList<String> headerLines,
                         String oldFile,
                         String newFile)
   {
      headerLines_ = headerLines;
      oldFile_ = oldFile.replaceFirst("^a/", "");
      newFile_ = newFile.replaceFirst("^b/", "");
   }

   public String getDescription()
   {
      if (oldFile_.equals(newFile_))
         return oldFile_;

      if (oldFile_.equals("/dev/null"))
         return newFile_;
      if (newFile_.equals("/dev/null"))
         return oldFile_;
      return oldFile_ + " => " + newFile_;
   }

   private final ArrayList<String> headerLines_;
   private final String oldFile_;
   private final String newFile_;
}
