/*
 * PreferencesDialogPaneBase.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.prefs;

import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.core.client.widget.ProgressIndicator;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class PreferencesDialogPaneBase<T> extends VerticalPanel
implements HasEnsureVisibleHandlers
{
   public abstract ImageResource getIcon();

   public boolean validate()
   {
      return true;
   }

   public abstract String getName();

   protected abstract void initialize(T prefs);

   /**
    * @return True if reload of the browser UI is required
    */
   public abstract boolean onApply(T prefs);
   
   
   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   public void registerEnsureVisibleHandler(HasEnsureVisibleHandlers widget)
   {
      widget.addEnsureVisibleHandler(new EnsureVisibleHandler()
      {
         public void onEnsureVisible(EnsureVisibleEvent event)
         {
            fireEvent(new EnsureVisibleEvent());
         }
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

   protected Widget spaced(Widget widget)
   {
      widget.addStyleName(res_.styles().spaced());
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

   protected Widget textBoxWithChooser(Widget widget)
   {
      widget.addStyleName(res_.styles().textBoxWithChooser());
      return widget;
   }
   
   protected void forceClosed(Command onClosed)
   {
      dialog_.forceClosed(onClosed);
   }
   
   void setDialog(PreferencesDialogBase<T> dialog)
   {
      dialog_ = dialog;
   }
   
   private ProgressIndicator progressIndicator_;
   private final PreferencesDialogBaseResources res_ =
                                 PreferencesDialogBaseResources.INSTANCE;
   
   private PreferencesDialogBase<T> dialog_;
}