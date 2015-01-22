/*
 * DesktopSubMenu.cpp
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <QAction>
#include <QDebug>
#include <QList>

#include "DesktopSubMenu.hpp"
#include "DesktopCommandInvoker.hpp"

namespace rstudio {
namespace desktop {

SubMenu::SubMenu(const QString& title, QWidget* parent):
    QMenu(title, parent)
{
    connect(this, SIGNAL(aboutToShow()),
            this, SLOT(onAboutToShow()));
}

// This algorithm checks each action in the menu to see whether it is a
// submenu that contains only invisible commands; if so, it hides the submenu.
void SubMenu::onAboutToShow()
{
   QList<QAction*> actionList = actions();
   for (QList<QAction*>::const_iterator pAction = actionList.begin();
        pAction != actionList.end();
        pAction++)
   {
      QMenu* menu = (*pAction)->menu();
      if (menu != NULL)
      {
         // Found a submenu; presume that it needs to be hidden until we
         // discover either a non-command or a visible command
         bool hide = true;
         QList<QAction*> subActionList = menu->actions();
         for (QList<QAction*>::const_iterator pSubAction = subActionList.begin();
              pSubAction != subActionList.end();
              pSubAction++)
         {
            QAction* subAction = *pSubAction;

            // Ignore separators
            if (subAction->isSeparator())
               continue;

            // If it's not a command or a separator, stop checking this menu
            QString cmdId = subAction->data().toString();
            if (cmdId.length() == 0)
            {
               hide = false;
               break;
            }

            // It's a command, check visibility state
            manageCommandVisibility(cmdId, subAction);
            if (subAction->isVisible())
            {
               hide = false;
               break;
            }
         }
         (*pAction)->setVisible(!hide);
      }
   }
}

} // namespace desktop
} // namespace rstudio
