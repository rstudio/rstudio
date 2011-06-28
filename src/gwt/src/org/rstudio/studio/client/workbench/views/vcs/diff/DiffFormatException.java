package org.rstudio.studio.client.workbench.views.vcs.diff;

public class DiffFormatException extends IllegalArgumentException
{
   public DiffFormatException(String tag)
   {
      super("Invalid diff format [" + tag + "]");
   }
}
