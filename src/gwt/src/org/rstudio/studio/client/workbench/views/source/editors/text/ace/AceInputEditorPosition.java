/*
 * AceInputEditorPosition.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

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

   private int getRow()
   {
      return (Integer)getLine();
   }

   @Override
   public int getLineLength()
   {
      return session_.getLine(getRow()).length();
   }

   public Position getValue()
   {
      return Position.create(getRow(), getPosition());
   }

   private final EditSession session_;
}
