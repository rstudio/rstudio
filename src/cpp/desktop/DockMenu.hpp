/*
 * DockMenu.hpp
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

#ifndef DOCK_MENU_HPP
#define DOCK_MENU_HPP

#include <QObject>
#include <QMenu>

namespace rstudio {
namespace desktop {

class MainWindow;

// Customize macOS dock menu
class DockMenu: public QMenu
{
   Q_OBJECT
public:
   explicit DockMenu(MainWindow* pMainWindow);

   void setMainWindow(MainWindow* pMainWindow);

protected Q_SLOTS:
   void onAboutToShow();
   void showWindow();

private:
   QAction* pWindowPlaceholder_ = nullptr;
   QList<QAction*> windows_;

   MainWindow* pMainWindow_;
};

} // namespace desktop
} // namespace rstudio

#endif // DOCK_MENU_HPP
