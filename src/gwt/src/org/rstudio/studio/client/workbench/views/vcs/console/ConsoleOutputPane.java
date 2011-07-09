package org.rstudio.studio.client.workbench.views.vcs.console;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.events.EnsureHiddenEvent;
import org.rstudio.core.client.events.EnsureHiddenHandler;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.widget.BottomScrollPanel;
import org.rstudio.core.client.widget.ClickImage;
import org.rstudio.core.client.widget.NineUpBorder;
import org.rstudio.studio.client.workbench.views.vcs.console.ConsoleBarPresenter.OutputDisplay;

public class ConsoleOutputPane extends ResizeComposite implements OutputDisplay
{
   interface Resources extends NineUpBorder.Resources
   {
      @Source("ConsoleOutputPane.css")
      Styles styles();

      @Override
      @Source("GitCommandOutputTopLeft.png")
      ImageResource topLeft();

      @Override
      @Source("GitCommandOutputTop.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource top();

      @Override
      @Source("GitCommandOutputTopRight.png")
      ImageResource topRight();

      @Override
      @Source("GitCommandOutputLeft.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource left();

      @Override
      @Source("GitCommandOutputRight.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource right();

      @Override
      @Source("GitCommandOutputLeft.png")
      ImageResource bottomLeft();

      @Override
      @Source("GitCommandOutputFill.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource bottom();

      @Override
      @Source("GitCommandOutputRight.png")
      ImageResource bottomRight();

      @Source("GitCommandCloseIcon.png")
      ImageResource closeIcon();
   }

   interface Styles extends CssResource
   {
      String outer();
      String command();
      String output();
   }

   public ConsoleOutputPane()
   {
      html_ = new HTML();
      html_.setStyleName(styles_.outer());
      html_.addStyleName("ace_text-layer ace_line");

      scrollPanel_ = new BottomScrollPanel(html_);
      scrollPanel_.setSize("100%", "100%");

      NineUpBorder nineUpBorder = new NineUpBorder(res_, 15, 3, 0, 3);
      nineUpBorder.setWidget(scrollPanel_);
      nineUpBorder.setFillColor("#fff");

      Image closeIcon = new ClickImage(res_.closeIcon());
      LayoutPanel borderLayoutPanel = nineUpBorder.getLayoutPanel();
      borderLayoutPanel.add(closeIcon);
      borderLayoutPanel.setWidgetTopHeight(closeIcon,
                                           5, Unit.PX,
                                           closeIcon.getHeight(), Unit.PX);
      borderLayoutPanel.setWidgetRightWidth(closeIcon,
                                            7, Unit.PX,
                                            closeIcon.getWidth(), Unit.PX);
      closeIcon.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            ensureHidden();
         }
      });

      initWidget(nineUpBorder);
   }

   @Override
   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   @Override
   public HandlerRegistration addEnsureHiddenHandler(EnsureHiddenHandler handler)
   {
      return addHandler(handler, EnsureHiddenEvent.TYPE);
   }

   @Override
   public void addCommand(String command)
   {
      addText("> " + command).setClassName(styles_.command() + " ace_keyword");
      scrollPanel_.onContentSizeChanged();
   }

   @Override
   public void addOutput(String output)
   {
      addText(output).setClassName(styles_.output());
      scrollPanel_.onContentSizeChanged();
   }

   @Override
   public void clearOutput()
   {
      html_.getElement().setInnerHTML("");
   }

   private Element addText(String command)
   {
      DivElement child = Document.get().createDivElement();
      child.setInnerText(command);
      html_.getElement().appendChild(child);
      scrollPanel_.onContentSizeChanged();
      ensureVisible();
      return child;
   }

   private void ensureVisible()
   {
      fireEvent(new EnsureVisibleEvent());
   }

   private void ensureHidden()
   {
      fireEvent(new EnsureHiddenEvent());
   }

   private final HTML html_;
   private final BottomScrollPanel scrollPanel_;

   private static final Resources res_ = GWT.create(Resources.class);
   private static final Styles styles_ = res_.styles();
   static {
      styles_.ensureInjected();
   }
}
