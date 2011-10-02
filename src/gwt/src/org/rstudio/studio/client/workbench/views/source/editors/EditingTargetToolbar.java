package org.rstudio.studio.client.workbench.views.source.editors;

import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Widget;

public class EditingTargetToolbar extends Toolbar
{
   public EditingTargetToolbar(Commands commands)
   {
      addLeftWidget(commands.sourceNavigateBack().createToolbarButton());
      Widget forwardButton = commands.sourceNavigateForward().createToolbarButton();
      forwardButton.getElement().getStyle().setMarginLeft(-6, Unit.PX);
      addLeftWidget(forwardButton);
      addLeftSeparator();
   }

}
