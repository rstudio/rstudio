package org.rstudio.studio.client.workbench.views.source.editors.text.status;

import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.ToolbarPopupMenu;

public class StatusBarPopupMenu extends ToolbarPopupMenu
{
   public StatusBarPopupMenu()
   {
      addStyleName(ThemeStyles.INSTANCE.statusBarMenu());
   }

   public void showRelativeToUpward(final UIObject target)
   {
      setPopupPositionAndShow(new PositionCallback()
      {
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            setPopupPosition(target.getAbsoluteLeft(),
                             target.getAbsoluteTop() - offsetHeight);
         }
      });
   }

   @Override
   protected ToolbarMenuBar createMenuBar()
   {
      final StatusBarMenuBar menuBar = new StatusBarMenuBar(true);
      menuBar.addSelectionHandler(new SelectionHandler<MenuItem>()
      {
         public void onSelection(SelectionEvent<MenuItem> event)
         {
            if (event.getSelectedItem() != null)
            {
               DomUtils.ensureVisibleVert(scrollPanel_.getElement(),
                                          event.getSelectedItem().getElement(),
                                          0);
            }
         }
      });
      return menuBar;
   }

   @Override
   protected Widget wrapMenuBar(ToolbarMenuBar menuBar)
   {
      scrollPanel_ = new ScrollPanel(menuBar);
      scrollPanel_.getElement().getStyle().setOverflowY(Overflow.AUTO);
      scrollPanel_.getElement().getStyle().setOverflowX(Overflow.HIDDEN);
      scrollPanel_.getElement().getStyle().setProperty("maxHeight", "300px");
      return scrollPanel_;
   }

   protected class StatusBarMenuBar extends ToolbarMenuBar
      implements HasSelectionHandlers<MenuItem>
   {
      public StatusBarMenuBar(boolean vertical)
      {
         super(vertical);
      }

      public HandlerRegistration addSelectionHandler(
            SelectionHandler<MenuItem> handler)
      {
         return addHandler(handler, SelectionEvent.getType());
      }

      @Override
      public void selectItem(MenuItem item)
      {
         super.selectItem(item);
         SelectionEvent.fire(this, item);
      }
   }

   private ScrollPanel scrollPanel_;
}
