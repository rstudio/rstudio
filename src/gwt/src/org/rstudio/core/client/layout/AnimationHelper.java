/*
 * AnimationHelper.java
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
package org.rstudio.core.client.layout;

import com.google.gwt.dom.client.Document;
import com.google.gwt.layout.client.Layout;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.theme.WindowFrame;

import static org.rstudio.core.client.layout.WindowState.*;

class AnimationHelper
{
   public static AnimationHelper create(BinarySplitLayoutPanel panel,
                                        LogicalWindow top,
                                        LogicalWindow bottom,
                                        int normal,
                                        int splitterHeight,
                                        boolean animate,
                                        boolean skipFocusChange)
   {
      boolean focusGoesOnTop = animate && focusGoesOnTop(top, bottom);
      
      int splitterPos;
      boolean splitterPosFromTop;
      if (bottom.getState() == WindowState.NORMAL)
      {
         splitterPos = normal;
         splitterPosFromTop = false;
      }
      else if (top.getState() == WindowState.HIDE)
      {
         splitterPos = -splitterHeight;
         splitterPosFromTop = true;
      }
      else if (bottom.getState() == WindowState.HIDE)
      {
         splitterPos = -splitterHeight;
         splitterPosFromTop = false;
      }
      else if (top.getState() == WindowState.MINIMIZE)
      {
         splitterPos = top.getMinimized().getDesiredHeight() - splitterHeight / 2;
         splitterPosFromTop = true;
      }
      else if (bottom.getState() == WindowState.MINIMIZE)
      {
         splitterPos = bottom.getMinimized().getDesiredHeight() - splitterHeight / 2;
         splitterPosFromTop = false;
      }
      else
      {
         throw new RuntimeException("Unexpected condition");
      }

      return new AnimationHelper(panel,
                                 getVisible(top),
                                 getVisible(bottom),
                                 top.getNormal(),
                                 bottom.getNormal(),
                                 top.getActiveWidget(),
                                 bottom.getActiveWidget(),
                                 splitterPos,
                                 splitterPosFromTop,
                                 bottom.getState() == WindowState.NORMAL,
                                 animate,
                                 focusGoesOnTop,
                                 skipFocusChange);
   }

   private static Widget getVisible(LogicalWindow window)
   {
      return window.getNormal().isVisible() ? window.getNormal() :
             window.getMinimized().isVisible() ? window.getMinimized() :
             null;
   }

   public AnimationHelper(BinarySplitLayoutPanel panel,
                          Widget startWidgetTop,
                          Widget startWidgetBottom,
                          Widget animWidgetTop,
                          Widget animWidgetBottom,
                          Widget endWidgetTop,
                          Widget endWidgetBottom,
                          int endSplitterPos,
                          boolean splitterPosFromTop,
                          boolean splitterVisible,
                          boolean animate,
                          boolean focusGoesOnTop,
                          boolean skipFocusChange)
   {
      panel_ = panel;
      startWidgetTop_ = startWidgetTop;
      startWidgetBottom_ = startWidgetBottom;
      animWidgetTop_ = animWidgetTop;
      animWidgetBottom_ = animWidgetBottom;
      endWidgetTop_ = endWidgetTop;
      endWidgetBottom_ = endWidgetBottom;
      endSplitterPos_ = endSplitterPos;
      splitterPosFromTop_ = splitterPosFromTop;
      splitterVisible_ = splitterVisible;
      animate_ = animate;
      focusGoesOnTop_ = focusGoesOnTop;
      skipFocusChange_ = skipFocusChange;
   }

   public void animate()
   {
      Document.get().getBody().addClassName("rstudio-animating");
      
      panel_.setSplitterVisible(false);

      if (startWidgetTop_ != animWidgetTop_)
         panel_.setTopWidget(animWidgetTop_, true);
      if (startWidgetBottom_ != animWidgetBottom_)
         panel_.setBottomWidget(animWidgetBottom_, true);

      panel_.forceLayout();

      panel_.setSplitterPos(endSplitterPos_,
                            splitterPosFromTop_);
      if (animate_)
      {
         panel_.animate(250, new Layout.AnimationCallback()
         {
            public void onAnimationComplete()
            {
               finish();
               if (skipFocusChange_)
                  return;
               
               ((WindowFrame)(focusGoesOnTop_
                              ? endWidgetTop_
                              : endWidgetBottom_)).focus();
            }

            public void onLayout(Layout.Layer layer, double progress)
            {
            }
         });
      }
      else
      {
         finish();
      }
   }

   private void finish()
   {
      panel_.setSplitterVisible(splitterVisible_);
      
      if (animWidgetTop_ != endWidgetTop_)
         panel_.setTopWidget(endWidgetTop_, true);
      if (animWidgetBottom_ != endWidgetBottom_)
         panel_.setBottomWidget(endWidgetBottom_, true);

      if (endWidgetTop_ != startWidgetTop_)
         setParentZindex(startWidgetTop_, -10);
      setParentZindex(endWidgetTop_, 0);

      if (endWidgetBottom_ != startWidgetBottom_)
         setParentZindex(startWidgetBottom_, -10);
      setParentZindex(endWidgetBottom_, 0);

      Document.get().getBody().removeClassName("rstudio-animating");
      panel_.onResize();
   }

   private static boolean focusGoesOnTop(LogicalWindow top, LogicalWindow bottom)
   {
      // If one window is maximized and the other is minimized, focus the
      // maximized one.
      WindowState topState = top.getState();
      WindowState bottomState = bottom.getState();
      
      if (topState == WindowState.MAXIMIZE && bottomState == WindowState.MINIMIZE)
         return true;
      else if (topState == WindowState.MINIMIZE && bottomState == WindowState.MAXIMIZE)
         return false;
      
      // If both windows are "normal", focus the one that was previously
      // minimized.

      if (top.getState() == MAXIMIZE || bottom.getState() == MAXIMIZE ||
            top.getState() == EXCLUSIVE || bottom.getState() == EXCLUSIVE)
      {
         assert top.getState() == MINIMIZE || bottom.getState() == MINIMIZE
               || top.getState() == HIDE || bottom.getState() == HIDE;
         // If one of the windows is minimized, focus the other one.
         return top.getState() == MAXIMIZE || top.getState() == EXCLUSIVE;
      }

      assert top.getState() == NORMAL && bottom.getState() == NORMAL;
      assert top.getNormal().isVisible() || bottom.getNormal().isVisible();

      return !top.getNormal().isVisible();
   }

   public static void setParentZindex(Widget widget, int zIndex)
   {
      if (widget != null)
         widget.getElement().getParentElement().getStyle().setZIndex(zIndex);
   }

   BinarySplitLayoutPanel panel_;

   Widget startWidgetTop_;
   Widget startWidgetBottom_;

   Widget animWidgetTop_;
   Widget animWidgetBottom_;

   Widget endWidgetTop_;
   Widget endWidgetBottom_;

   int endSplitterPos_;
   boolean splitterPosFromTop_;
   boolean splitterVisible_;
   private final boolean animate_;
   private final boolean focusGoesOnTop_;
   private final boolean skipFocusChange_;
}
