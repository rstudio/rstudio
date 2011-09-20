package org.rstudio.studio.client.workbench.views.console.shell.editor;

public class InputEditorLineWithCursorPosition
{
   public InputEditorLineWithCursorPosition(String line, int position)
   {
      line_ = line;
      position_ = position;
   }
   
   public String getLine()
   {
      return line_;
   }
   
   public int getPosition()
   {
      return position_;
   }
   
   private final String line_;
   private final int position_;
}
