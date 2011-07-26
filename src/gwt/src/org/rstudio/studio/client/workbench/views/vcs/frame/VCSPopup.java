package org.rstudio.studio.client.workbench.views.vcs.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.widget.NineUpBorder;
import org.rstudio.studio.client.workbench.views.vcs.review.ReviewPresenter;

public class VCSPopup
{
   interface Resources extends NineUpBorder.Resources
   {
      @Override
      @Source("TopLeft.png")
      ImageResource topLeft();

      @Override
      @Source("Top.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource top();

      @Override
      @Source("TopRight.png")
      ImageResource topRight();

      @Override
      @Source("Left.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource left();

      @Override
      @Source("Right.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource right();

      @Override
      @Source("BottomLeft.png")
      ImageResource bottomLeft();

      @Override
      @Source("Bottom.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource bottom();

      @Override
      @Source("BottomRight.png")
      ImageResource bottomRight();

      @Source("Close.png")
      ImageResource close();
   }

   public static void show(ReviewPresenter presenter)
   {
      Widget w = presenter.asWidget();
      w.setSize("100%", "100%");

      PopupPanel popup = new PopupPanel(false, false);
      NineUpBorder border = new NineUpBorder(
            GWT.<Resources>create(Resources.class),
            32, 20, 20, 20);
      addCloseButton(popup, border);
      border.setSize("1300px", "900px");
      border.setFillColor("white");
      border.setWidget(w);
      popup.setWidget(border);
      popup.setGlassEnabled(true);
      popup.getElement().getStyle().setZIndex(1001);
      popup.center();
   }

   private static void addCloseButton(final PopupPanel popupPanel,
                                      NineUpBorder border)
   {
      Resources res = GWT.create(Resources.class);
      Image closeIcon = new Image(res.close());
      closeIcon.getElement().getStyle().setCursor(Style.Cursor.POINTER);
      closeIcon.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            popupPanel.hide();
            popupPanel.removeFromParent();
         }
      });

      LayoutPanel layoutPanel = border.getLayoutPanel();
      layoutPanel.add(closeIcon);
      layoutPanel.setWidgetTopHeight(closeIcon,
                                     15, Unit.PX,
                                     closeIcon.getHeight(), Unit.PX);
      layoutPanel.setWidgetRightWidth(closeIcon,
                                      27, Unit.PX,
                                      closeIcon.getWidth(), Unit.PX);
   }
}
