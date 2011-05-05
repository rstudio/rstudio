package org.rstudio.studio.client.workbench.views.source.editors.text.status;

import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
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
   protected Widget wrapMenuBar(MenuBar menuBar)
   {
      SimplePanel simplePanel = new SimplePanel(menuBar);
      simplePanel.getElement().getStyle().setOverflowY(Overflow.AUTO);
      simplePanel.getElement().getStyle().setOverflowX(Overflow.HIDDEN);
      simplePanel.getElement().getStyle().setProperty("maxHeight", "300px");
      return simplePanel;
   }


}
