/*
 * FindBar.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget.find;

import org.rstudio.core.client.CoreClientConstants;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.DecorativeImage;
import org.rstudio.core.client.widget.FindTextBox;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.workbench.views.history.view.Shelf;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import elemental2.dom.Element;
import elemental2.dom.KeyboardEvent;
import jsinterop.base.Js;

public abstract class FindBar extends Composite
{
   public enum Direction { NEXT, PREV };

   public abstract void show(boolean focus);
   public abstract void find(Direction dir);
   public abstract void hide();

   interface Resources extends ClientBundle
   {
      @Source("FindBar.css")
      Styles styles();
   }

   interface Styles extends CssResource
   {
      String findBar();
      String findTextBox();
      String findPanel();
      String closeButton();
   }

   public FindBar()
   {
      Shelf shelf = new Shelf(false);
      shelf.setWidth("100%");
      shelf.addStyleName("rstudio-themes-background");

      VerticalPanel panel = new VerticalPanel();
      ElementIds.assignElementId(panel.getElement(), ElementIds.FIND_REPLACE_BAR);

      txtFind_ = new FindTextBox(constants_.findButtonText());
      txtFind_.addStyleName(RES.styles().findTextBox());
      txtFind_.setIconVisible(true);

      btnFindNext_ = new SmallButton(constants_.nextButtonText());
      btnFindPrev_ = new SmallButton(constants_.prevButtonText());

      btnClose_ = new Button();
      btnClose_.addStyleName(RES.styles().closeButton());
      btnClose_.addStyleName(ThemeStyles.INSTANCE.closeTabButton());
      btnClose_.addStyleName(ThemeStyles.INSTANCE.handCursor());
      Roles.getButtonRole().setAriaLabelProperty(btnClose_.getElement(), constants_.closeText());
      btnClose_.getElement().appendChild(
            new DecorativeImage(new ImageResource2x(ThemeResources.INSTANCE.closeTab2x())).getElement());

      HorizontalPanel findReplacePanel = new HorizontalPanel();
      findReplacePanel.addStyleName(RES.styles().findPanel());
      findReplacePanel.add(txtFind_);
      findReplacePanel.add(btnFindNext_);
      findReplacePanel.add(btnFindPrev_);
      panel.add(findReplacePanel);

      shelf.addLeftWidget(panel);
      shelf.setRightVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
      shelf.addRightWidget(btnClose_);

      initWidget(shelf);
      addStyleName(RES.styles().findBar());

      initBehaviors();
   }

   private void initBehaviors()
   {
      Element el;

      el = Js.cast(getElement());
      el.addEventListener("keydown", event ->
      {
         KeyboardEvent keyEvent = Js.cast(event);
         if (keyEvent.key.equals("Escape"))
         {
            event.stopPropagation();
            event.preventDefault();
            hide();
            return;
         }
      }, true);

      el = Js.cast(txtFind_.getElement());
      el.addEventListener("keydown",  event ->
      {
         KeyboardEvent keyEvent = Js.cast(event);
         if (keyEvent.key.equals("Enter"))
         {
            event.stopPropagation();
            event.preventDefault();
            find(keyEvent.shiftKey ? Direction.PREV : Direction.NEXT);
            return;
         }
      });

      btnFindNext_.addClickHandler(event -> find(Direction.NEXT));
      btnFindPrev_.addClickHandler(event -> find(Direction.PREV));
      btnClose_.addClickHandler(event -> hide());
   }

   public String getValue()
   {
      return txtFind_.getValue();
   }

   public void setValue(String value)
   {
      txtFind_.setValue(value);
   }

   public double getHeightPx()
   {
      return 26;
   }

   protected final FindTextBox txtFind_;
   protected final SmallButton btnFindNext_;
   protected final SmallButton btnFindPrev_;
   protected final Button btnClose_;

   private static final Resources RES = GWT.create(Resources.class);
   private static final CoreClientConstants constants_ = GWT.create(CoreClientConstants.class);

   static {
      RES.styles().ensureInjected();
   }
}
