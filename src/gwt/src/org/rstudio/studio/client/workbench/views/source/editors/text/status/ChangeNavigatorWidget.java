/*
 * ChangeNavigatorWidget.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.source.editors.text.status;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.studio.client.workbench.views.source.editors.text.EditorsTextConstants;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;

// Previous and Next Change buttons around a count of the document's Git
// changes, for the editor's status bar.
public class ChangeNavigatorWidget extends Composite
{
   public ChangeNavigatorWidget()
   {
      FlowPanel panel = new FlowPanel();
      panel.setStyleName("rstudio-git-change-nav");
      Roles.getGroupRole().set(panel.getElement());
      Roles.getGroupRole().setAriaLabelProperty(panel.getElement(), constants_.gitChangeNavigatorLabel());

      previous_ = createButton(CHEVRON_UP, false);
      next_ = createButton(CHEVRON_DOWN, true);
      count_ = new InlineLabel();
      count_.setStyleName("rstudio-git-change-nav-count");

      panel.add(previous_);
      panel.add(count_);
      panel.add(next_);
      initWidget(panel);
   }

   public interface Handler
   {
      // fromPointer: activated with the mouse rather than from the keyboard
      void onNavigate(boolean next, boolean fromPointer);
   }

   // The buttons are named and described by the commands they stand in for,
   // but go to the handler: a click acts on this document, wherever the
   // active source column is.
   public void setCommands(AppCommand previous, AppCommand next, Handler handler)
   {
      describe(previous_, previous);
      describe(next_, next);
      handler_ = handler;
   }

   // (called as the cursor moves, so it leaves the DOM alone when nothing
   // has changed)
   public void setText(String text, boolean canNavigate)
   {
      if (!text.equals(count_.getText()))
         count_.setText(text);
      if (previous_.isEnabled() != canNavigate)
      {
         previous_.setEnabled(canNavigate);
         next_.setEnabled(canNavigate);
      }
   }

   private Button createButton(String icon, boolean next)
   {
      Button button = new Button(SafeHtmlUtils.fromTrustedString(icon));
      button.setStyleName("rstudio-git-change-nav-button");

      // A click does not take focus to the button (the handler sends it to
      // the editor); from the keyboard, focus stays on it for the next step.
      button.addMouseDownHandler(event -> event.preventDefault());

      // (the status bar runs a handler of its own for clicks on a message)
      button.addClickHandler(event ->
      {
         event.stopPropagation();
         if (handler_ != null)
            handler_.onNavigate(next, clickCount(event.getNativeEvent()) > 0);
      });
      return button;
   }

   // Zero for a click synthesized from the keyboard (Enter or Space).
   private static native int clickCount(NativeEvent event) /*-{
      return event.detail || 0;
   }-*/;

   private static void describe(Button button, AppCommand command)
   {
      button.getElement().setAttribute("aria-label", command.getLabel());
      button.setTitle(command.getTooltip());
   }

   private final Button previous_;
   private final Button next_;
   private final InlineLabel count_;
   private Handler handler_;

   // Chevrons in the text colour, so that they follow the theme.
   private static final String CHEVRON_UP =
         "<svg width=\"10\" height=\"10\" viewBox=\"0 0 10 10\" aria-hidden=\"true\" focusable=\"false\">" +
         "<path d=\"M2 6.5L5 3.5L8 6.5\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\"/>" +
         "</svg>";
   private static final String CHEVRON_DOWN =
         "<svg width=\"10\" height=\"10\" viewBox=\"0 0 10 10\" aria-hidden=\"true\" focusable=\"false\">" +
         "<path d=\"M2 3.5L5 6.5L8 3.5\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\"/>" +
         "</svg>";

   private static final EditorsTextConstants constants_ = GWT.create(EditorsTextConstants.class);
}
