package org.rstudio.core.client.cellview;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

// In the version of GWT currently used, the splitter doesn't hide when the
// associated child is hidden. This is fixed in later releases of GWT.
// http://gwt-code-reviews.appspot.com/1880804/patch/9001/10001
//
// This class extends the stock GWT SplitLayoutPanel such that it hides the
// splitter associated with a child widget when that widget is hidden.
public class AutoHidingSplitLayoutPanel
   extends SplitLayoutPanel
{
   public AutoHidingSplitLayoutPanel(int splitterSize)
   {
      super(splitterSize);
   }

   public AutoHidingSplitLayoutPanel(Style.Unit unit)
   {
   }

   @Override
   public void setWidgetHidden(Widget widget, boolean hidden) {
      LayoutData layoutData = (LayoutData)widget.getLayoutData();

      if (layoutData.direction != Direction.CENTER) {
         Widget splitter = getAssociatedSplitter(widget);
         super.setWidgetHidden(splitter, hidden);
      }

      super.setWidgetHidden(widget, hidden);
   }

   // adapted from the private method of the same name in the parent class
   private Widget getAssociatedSplitter(Widget child) {
      int idx = getWidgetIndex(child);
      if (idx > -1 && idx < getWidgetCount() - 1) {
         return getWidget(idx + 1);
      }
      return null;
   }
}
