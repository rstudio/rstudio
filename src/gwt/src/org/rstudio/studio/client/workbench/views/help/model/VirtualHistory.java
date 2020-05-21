/*
 * VirtualHistory.java
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
package org.rstudio.studio.client.workbench.views.help.model;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Point;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;

import com.google.gwt.user.client.ui.Frame;

public class VirtualHistory
{
   public static class Data
   {
      public Data(String url)
      {
         url_ = url;
         scrollPos_ = Point.create(0, 0);
      }
      
      public String getUrl()
      {
         return url_;
      }
      
      public Point getScrollPosition()
      {
         return scrollPos_;
      }
      
      public void setScrollPosition(Point scrollPos)
      {
         scrollPos_ = scrollPos;
      }
      
      private final String url_;
      private Point scrollPos_;
   }
   
   public VirtualHistory(Frame frame)
   {
      frame_ = frame;
   }
   
   public void navigate(String url)
   {
      // truncate the stack to the current pos
      while (stack_.size() > pos_ + 1)
         stack_.remove(stack_.size() - 1);
      
      saveScrollPosition();
      stack_.add(new Data(url));
      pos_ = stack_.size() - 1;
   }

   public Data back()
   {
      if (pos_ <= 0)
         return null;
      
      saveScrollPosition();
      pos_--;

      return stack_.get(pos_);
   }
   
   public Data forward()
   {
      if (pos_ >= stack_.size() - 1)
         return null;
      
      saveScrollPosition();
      pos_++;

      return stack_.get(pos_);
   }
   
   private void saveScrollPosition()
   {
      if (pos_ < 0 || pos_ >= stack_.size())
         return;
      
      WindowEx contentWindow =
            ((IFrameElementEx) frame_.getElement().cast()).getContentWindow();
      
      Data data = stack_.get(pos_);
      data.setScrollPosition(contentWindow.getScrollPosition());
   }
   
   private final Frame frame_;
   private List<Data> stack_ = new ArrayList<Data>();
   private int pos_ = -1;
}
