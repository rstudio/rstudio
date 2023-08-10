/*
 * PreferencesDialogPaneBase.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.core.client.prefs;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.ProgressIndicator;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class PreferencesDialogPaneBase<T> extends VerticalPanel
                                                   implements HasEnsureVisibleHandlers
{
   public abstract ImageResource getIcon();

   public PreferencesDialogPaneBase()
   {
      super();
      Roles.getTabpanelRole().set(getElement());
   }

   public boolean validate()
   {
      return true;
   }

   public abstract String getName();

   protected abstract void initialize(T prefs);

   public abstract RestartRequirement onApply(T prefs);

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleEvent.Handler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   public void registerEnsureVisibleHandler(HasEnsureVisibleHandlers widget)
   {
      widget.addEnsureVisibleHandler(event ->
      {
         fireEvent(new EnsureVisibleEvent());
      });
   }

   public void setProgressIndicator(ProgressIndicator progressIndicator)
   {
      progressIndicator_ = progressIndicator;
   }

   protected ProgressIndicator getProgressIndicator()
   {
      return progressIndicator_;
   }


   protected Widget indent(Widget widget)
   {
      widget.addStyleName(res_.styles().indent());
      return widget;
   }

   protected Widget tight(Widget widget)
   {
      widget.addStyleName(res_.styles().tight());
      return widget;
   }

   protected Widget lessSpaced(Widget widget)
   {
      if (!BrowseCap.isLinuxDesktop())
      {
         widget.addStyleName(res_.styles().lessSpaced());
         return widget;
      }
      else
      {
         return widget;
      }
   }

   protected Widget spacedBefore(Widget widget)
   {
      widget.addStyleName(res_.styles().spacedBefore());
      return widget;
   }

   protected Widget spaced(Widget widget)
   {
      widget.addStyleName(res_.styles().spaced());
      return widget;
   }

   protected Widget mediumSpaced(Widget widget)
   {
      widget.addStyleName(res_.styles().mediumSpaced());
      return widget;
   }

   protected Widget extraSpaced(Widget widget)
   {
      widget.addStyleName(res_.styles().extraSpaced());
      return widget;
   }

   protected Widget nudgeRight(Widget widget)
   {
      widget.addStyleName(res_.styles().nudgeRight());
      return widget;
   }

   protected Widget nudgeRightPlus(Widget widget)
   {
      widget.addStyleName(res_.styles().nudgeRightPlus());
      return widget;
   }

   protected Widget textBoxWithChooser(Widget widget)
   {
      widget.addStyleName(res_.styles().textBoxWithChooser());
      return widget;
   }
   
   protected Label headerLabel(String caption)
   {
      Label headerLabel = new Label(caption);
      headerLabel.addStyleName(res().styles().headerLabel());
      nudgeRight(headerLabel);
      return headerLabel;
   }

   protected FormLabel headerLabel(String caption, Widget labeledWidget)
   {
      return headerLabel(caption, labeledWidget.getElement());
   }

   protected FormLabel headerLabel(String caption, Element labeledElement)
   {
      FormLabel headerLabel = new FormLabel(caption, labeledElement);
      headerLabel.addStyleName(res().styles().headerLabel());
      nudgeRight(headerLabel);
      return headerLabel;
   }

   protected void forceClosed(Command onClosed)
   {
      dialog_.forceClosed(onClosed);
   }

   protected void setEnterDisabled(boolean enterDisabled)
   {
      dialog_.setEnterDisabled(enterDisabled);
   }

   protected PreferencesDialogBaseResources res()
   {
      return res_;
   }

   void setDialog(PreferencesDialogBase<T> dialog)
   {
      dialog_ = dialog;
   }

   protected void setPaneVisible(boolean visible)
   {
      getElement().getStyle().setDisplay(visible
                                              ? Display.BLOCK
                                              : Display.NONE);

   }
   
   public void setTabPanelSize(DialogTabLayoutPanel panel)
   {
      panel.setSize("448px", "570px");
   }

   private ProgressIndicator progressIndicator_;
   private final PreferencesDialogBaseResources res_ =
                                 PreferencesDialogBaseResources.INSTANCE;

   private PreferencesDialogBase<T> dialog_;
}
