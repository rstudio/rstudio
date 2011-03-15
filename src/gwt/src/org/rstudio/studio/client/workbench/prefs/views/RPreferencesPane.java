package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.inject.Inject;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;

public class RPreferencesPane extends PreferencesPane
{
   @Inject
   public RPreferencesPane(PreferencesDialogResources res,
                           WorkbenchServerOperations server)
   {
      res_ = res;
      server_ = server;

      if (Desktop.getFrame().canChooseRVersion())
      {
         rVersion_ = new TextBoxWithButton(
               "R Installation",
               "Change...",
               new ClickHandler()
               {
                  public void onClick(ClickEvent event)
                  {
                     String ver = Desktop.getFrame().chooseRVersion();
                     if (!StringUtil.isNullOrEmpty(ver))
                        rVersion_.setText(ver);
                  }
               });
         rVersion_.setWidth("100%");
         rVersion_.setText(Desktop.getFrame().getRVersion());
         add(rVersion_);         
      }

      cranMirror_ = new TextBoxWithButton(
            "Default CRAN Mirror",
            "Change...",
            new ClickHandler()
            {
               public void onClick(ClickEvent event)
               {
                  String mirror = Desktop.getFrame().chooseCRANmirror();
                  if (!StringUtil.isNullOrEmpty(mirror))
                     cranMirror_.setText(mirror);
               }
            });
      cranMirror_.setWidth("100%");
      cranMirror_.setText(Desktop.getFrame().getCRANMirror());

      saveWorkspace_ = new SelectWidget(
            "Save workspace on exit?",
            new String[] {
                  "Always",
                  "Never",
                  "Ask me every time"
            });
      saveWorkspace_.getListBox().setSelectedIndex(
            Desktop.getFrame().getSaveAction());

      add(cranMirror_);
      add(saveWorkspace_);
   }

   @Override
   public ImageResource getIcon()
   {
      return res_.iconR();
   }

   @Override
   public void onApply()
   {
      String saveAction;
      int index = saveWorkspace_.getListBox().getSelectedIndex();
      switch (index)
      {
         case 0:
            saveAction = "yes";
            break;
         case 1:
            saveAction = "no";
            break;
         case 2:
         default:
            saveAction = "ask";
            break;
      }

      Desktop.getFrame().setSaveAction(index);
      server_.setSaveAction(
            saveAction,
            new SimpleRequestCallback<org.rstudio.studio.client.server.Void>());
   }

   @Override
   public String getName()
   {
      return "R";
   }

   private final PreferencesDialogResources res_;
   private final WorkbenchServerOperations server_;
   private SelectWidget saveWorkspace_;
   private TextBoxWithButton rVersion_;
   private TextBoxWithButton cranMirror_;
}
