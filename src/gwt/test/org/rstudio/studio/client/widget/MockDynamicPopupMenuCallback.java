package org.rstudio.studio.client.widget;

import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.core.client.widget.ToolbarPopupMenu.DynamicPopupMenuCallback;

public class MockDynamicPopupMenuCallback implements DynamicPopupMenuCallback
{
    public ToolbarPopupMenu _menu;

    @Override
    public void onPopupMenu(ToolbarPopupMenu menu)
    {
        _menu = menu;
    }

}
