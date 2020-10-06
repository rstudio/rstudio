/*
 * DockMenu.cpp
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

#include "DockMenu.hpp"

#include "DesktopMainWindow.hpp"

namespace rstudio {
namespace desktop {

DockMenu::DockMenu(MainWindow *pMainWindow) :
   pMainWindow_(pMainWindow)
{
   setAsDockMenu();
   
   pWindowPlaceholder_ = addSeparator();

   QAction* pNewWindow = addAction(QObject::tr("New RStudio Window"));

   // We hold a raw pointer to MainWindow. When the main window
   // goes away, so does the dock icon, thus the menu and the possibility of the
   // user clicking it, so this is safe (though unpleasant).
   QObject::connect(pNewWindow, &QAction::triggered, [this] () {
      std::vector<std::string> args;
      pMainWindow_->launchRStudio(args);
   });

   connect(this, &QMenu::aboutToShow,
           this, &DockMenu::onAboutToShow);
}

void DockMenu::setMainWindow(MainWindow* pMainWindow)
{
   pMainWindow_ = pMainWindow;
}

void DockMenu::onAboutToShow()
{
   // remove actions from last time the menu was shown
   for (auto i = windows_.size() - 1; i >= 0; i--)
   {
      QAction* pAction = windows_[i];
      removeAction(pAction);
      windows_.removeAt(i);
      pAction->deleteLater();
   }
   
   // populate menu with windows
   QWidgetList topLevels = QApplication::topLevelWidgets();
   for (auto pWindow : topLevels)
   {
      if (!pWindow->isVisible())
         continue;

      QAction* pAction = new QAction(pWindow->windowTitle(), nullptr);
      pAction->setData(QVariant::fromValue(pWindow));
      pAction->setCheckable(true);
      if (pWindow->isActiveWindow())
         pAction->setChecked(true);
      insertAction(pWindowPlaceholder_, pAction);
      connect(pAction, SIGNAL(triggered()),
              this, SLOT(showWindow()));

      windows_.append(pAction);
   }
}

void DockMenu::showWindow()
{
   auto* pAction = qobject_cast<QAction*>(sender());
   if (!pAction)
      return;
   auto* pWidget = pAction->data().value<QWidget*>();
   if (!pWidget)
      return;
   if (pWidget->isMinimized())
      pWidget->setWindowState(pWidget->windowState() & ~Qt::WindowMinimized);
   pWidget->activateWindow();
   pWidget->raise();
}

} // namespace desktop
} // namespace rstudio
