/*
 * DesktopSubMenu.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include "DesktopSubMenu.hpp"

namespace rstudio {
namespace desktop {

namespace {

void hideCommandsWithNoLabel(QMenu* pMenu)
{
   for (auto* pAction : pMenu->actions())
   {
      if (pAction->menu())
      {
         hideCommandsWithNoLabel(pAction->menu());
      }
      else if (pAction->isSeparator())
      {
         // no action to take
      }
      else
      {
         if (pAction->isVisible())
         {
            pAction->setVisible(!pAction->text().isEmpty());
         }
      }
   }
}

} // end anonymous namespace

SubMenu::SubMenu(const QString& title, QWidget* parent):
    QMenu(title, parent)
{
   setSeparatorsCollapsible(true);
   connect(this, SIGNAL(aboutToShow()), this, SLOT(onAboutToShow()));
}

void SubMenu::onAboutToShow()
{
   // This algorithm checks each action in the menu to see whether it is a
   // submenu that contains only invisible commands; if so, it hides the submenu.
   for (auto* pAction : actions())
   {
      QMenu* menu = pAction->menu();
      if (menu != nullptr)
      {
         // Found a submenu; presume that it needs to be hidden until we
         // discover either a non-command or a visible command
         bool hide = true;
         for (auto* pSubAction : menu->actions())
         {
            // Ignore separators
            if (pSubAction->isSeparator())
               continue;

            // If it's not a command or a separator, stop checking this menu
            QString cmdId = pSubAction->data().toString();
            if (cmdId.length() == 0)
            {
               hide = false;
               break;
            }

            // It's a command, check visibility state
            if (pSubAction->isVisible())
            {
               hide = false;
               break;
            }
         }

         pAction->setVisible(!hide);
      }
   }

   // Hide commands with no text (e.g. MRU commands with no associated file)
   hideCommandsWithNoLabel(this);

   // Clean up duplicated separators.
   // TODO: Qt is supposed to do this for us; perhaps we're
   // not managing commands in the menu in the way Qt expects
   // us to?
   bool lastActionWasSeparator = true;
   for (auto* pAction : actions())
   {
      if (pAction->isSeparator())
      {
         pAction->setVisible(!lastActionWasSeparator);
      }

      if (!pAction->isVisible())
         continue;

      lastActionWasSeparator = pAction->isSeparator();
   }
}

} // namespace desktop
} // namespace rstudio
