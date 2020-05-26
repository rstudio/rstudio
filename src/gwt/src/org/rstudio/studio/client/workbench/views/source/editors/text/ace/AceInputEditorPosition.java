/*
 * AceInputEditorPosition.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;

public class AceInputEditorPosition extends InputEditorPosition
{
   public AceInputEditorPosition(EditSession session, Position position)
   {
      super(position.getRow(), position.getColumn());
      session_ = session;
   }

   @Override
   protected int compareLineTo(Object other)
   {
      return getRow() - (Integer)other;
   }

   @Override
   public InputEditorPosition movePosition(int position, boolean relative)
   {
      return new AceInputEditorPosition(
            session_,
            Position.create(getRow(),
                            relative ? getPosition() + position : position));
   }
   
   @Override
   public InputEditorPosition moveToNextLine()
   {
      return new AceInputEditorPosition(session_,
                                        Position.create(getRow() + 1, 0));
   }
   
   @Override
   public InputEditorPosition moveToPreviousLine()
   {
      int newRow = Math.max(getRow() - 1, 0);
      return new AceInputEditorPosition(session_, Position.create(newRow, 0));
   }

   private int getRow()
   {
      return (Integer)getLine();
   }

   @Override
   public int getLineLength()
   {
      return session_.getLine(getRow()).length();
   }

   /**
    *
    * @param upwards True if the position should be moved upwards. If true, the
    *    resulting position (if non-null) will be at the end of a non-empty
    *    line. If false, the resulting position (if non-null) will be at the
    *    beginning of a non-empty line.
    * @param boundary If non-null, provides a boundary point beyond which the
    *    skipping may not pass.
    * @return A position that's on a non-empty line, or else, null if such a
    *    position couldn't be found before hitting the beginning/end of the
    *    document or a boundary position.
    */
   @Override
   public InputEditorPosition skipEmptyLines(boolean upwards,
                                             InputEditorPosition boundary)
   {
      Position position = Position.create(getRow(), getPosition());
      while (isLineEmpty(position, upwards))
      {
         if (upwards)
         {
            if (position.getRow() <= 0)
               return null;

            position = Position.create(
                  position.getRow() - 1,
                  session_.getLine(position.getRow() - 1).length());
         }
         else
         {
            if (position.getRow() >= session_.getLength()-1)
               return null;

            position = Position.create(position.getRow() + 1, 0);
         }
      }

      InputEditorPosition pos = new AceInputEditorPosition(session_, position);
      return boundary == null ? pos :
             (upwards && pos.compareTo(boundary) >= 0) ? pos :
             (!upwards && pos.compareTo(boundary) <= 0) ? pos :
             null;
   }

   @Override
   public InputEditorPosition growToIncludeLines(String pattern, boolean upwards)
   {
      int rowNum = getRow();
      String line = session_.getLine(rowNum);
      if (!line.matches(pattern))
         return this;

      while (true)
      {
         if (upwards)
         {
            if (rowNum == 0)
               break;
            if (!session_.getLine(rowNum-1).matches(pattern))
               break;
            rowNum--;
         }
         else
         {
            if (rowNum == session_.getLength()-1)
               break;
            if (!session_.getLine(rowNum+1).matches(pattern))
               break;
            rowNum++;
         }
      }

      int col = upwards ? 0 : session_.getLine(rowNum).length();

      return new AceInputEditorPosition(session_, Position.create(rowNum, col));
   }

   private boolean isLineEmpty(Position position, boolean leftwards)
   {
      String line = session_.getLine(position.getRow());
      int column = position.getColumn();
      if (leftwards)
         line = line.substring(0, Math.min(line.length(), column));
      else
         line = line.substring(Math.min(line.length(), column));

      return StringUtil.notNull(line).trim().length() == 0;
   }

   public Position getValue()
   {
      return Position.create(getRow(), getPosition());
   }

   private final EditSession session_;
}
