package org.rstudio.studio.client.common.shell;

public interface ShellOutputWriter 
{
   void consoleWriteError(String string) ;
   void consoleWriteOutput(String output) ;
}
