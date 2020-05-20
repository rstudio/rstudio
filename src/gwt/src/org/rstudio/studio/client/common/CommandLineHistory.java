/*
 * CommandLineHistory.java
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
package org.rstudio.studio.client.common;

import com.google.gwt.user.client.ui.HasText;
import org.rstudio.core.client.StringUtil;

import java.util.ArrayList;

public class CommandLineHistory
{
   public CommandLineHistory(HasText input)
   {
      input_ = input;
   }

   public void setHistory(ArrayList<String> history)
   {
      history_.clear();
      for (String entry : history)
         addToHistory(entry);
      resetPosition();
   }

   public void addToHistory(String command)
   {
      if (StringUtil.isNullOrEmpty(command))
         return;

      if (history_.size() > 0
          && command.equals(history_.get(history_.size() - 1)))
      {
         // do not allow dupes
         return;
      }

      history_.add(command);
   }

   public void navigateHistory(int offset)
   {
      int newPos = getPositionAtOffset(offset);

      if (newPos == historyPos_)
         return; // no-op due to boundary limits

      if (historyPos_ == history_.size())
      {
         historyTail_ = input_.getText();
      }

      input_.setText(
            newPos < history_.size() ? history_.get(newPos) :
            historyTail_ != null ? historyTail_ :
            "");
      historyPos_ = newPos;
   }
   
   public String getHistoryEntry(int offset)
   {
      int pos = getPositionAtOffset(offset);
      return history_.get(pos);
   }

   public void resetPosition()
   {
      historyPos_ = history_.size();
      historyTail_ = "";
   }
   
   private int getPositionAtOffset(int offset)
   {
      int pos = historyPos_ + offset;
      return Math.max(0, Math.min(pos, history_.size()));
   }

   private final ArrayList<String> history_ = new ArrayList<String>();
   private int historyPos_;
   // If you start typing a command, then go up in history, then go down,
   // then what you had previously typed should still be there. This is
   // that value--it is loaded/saved whenever history navigation takes you
   // into/out of that final history position (history_.size()).
   private String historyTail_;
   private final HasText input_;
}
