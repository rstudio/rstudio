/*
 * DesktopMenuCallback.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "DesktopMenuCallback.hpp"
#include <QDebug>
#include <QApplication>
#include "DesktopCommandInvoker.hpp"

#ifdef Q_OS_MAC
#include <ApplicationServices/ApplicationServices.h>
#endif

namespace desktop {

MenuCallback::MenuCallback(QObject *parent) :
    QObject(parent)
{
}

void MenuCallback::beginMainMenu()
{
   pMainMenu_ = new QMenuBar();
}

void MenuCallback::beginMenu(QString label)
{
#ifdef Q_OS_MAC
   if (label == "&Help")
   {
      pMainMenu_->addMenu(new WindowMenu(pMainMenu_));
   }
#endif

   QMenu* pMenu = new QMenu(label, pMainMenu_);

   if (menuStack_.count() == 0)
      pMainMenu_->addMenu(pMenu);
   else
      menuStack_.top()->addMenu(pMenu);

   menuStack_.push(pMenu);
   menuActions_[pMenu] = QList<QAction*>();

   connect(pMenu, SIGNAL(aboutToShow()),
           this, SLOT(aboutToShowMenu()));
}

void MenuCallback::addCommand(QString commandId,
                              QString label,
                              QString tooltip,
                              QString shortcut)
{
   shortcut = shortcut.replace("Enter", "\n");

   QKeySequence keySequence(shortcut);
#ifndef Q_WS_MAC
   if (shortcut.contains("\n"))
   {
      int value = (keySequence[0] & Qt::MODIFIER_MASK) + Qt::Key_Enter;
      keySequence = QKeySequence(value);
   }
#endif

   QAction* pAction = menuStack_.top()->addAction(QIcon(),
                                                  label,
                                                  this,
                                                  SLOT(actionInvoked()),
                                                  keySequence);
   pAction->setData(commandId);
   pAction->setToolTip(tooltip);

   menuActions_[menuStack_.first()].append(pAction);
}

void MenuCallback::actionInvoked()
{
   QAction* action = qobject_cast<QAction*>(sender());
   QString commandId = action->data().toString();
   manageCommand(commandId, action);
   if (action->isEnabled())
      commandInvoked(commandId);
}

void MenuCallback::addSeparator()
{
   if (menuStack_.count() > 0)
      menuStack_.top()->addSeparator();
}

void MenuCallback::endMenu()
{
   menuStack_.pop();
}

void MenuCallback::endMainMenu()
{
   menuBarCompleted(pMainMenu_);
}

void MenuCallback::aboutToShowMenu()
{
   QMenu* menu = qobject_cast<QMenu*>(sender());
   if (menuActions_.contains(menu))
   {
      QList<QAction*> list = menuActions_[menu];
      for (int i = 0; i < list.size(); i++)
      {
         QAction* action = list.at(i);
         QString commandId = action->data().toString();
         manageCommand(commandId, action);
      }
   }
}

WindowMenu::WindowMenu(QWidget *parent) : QMenu("&Window", parent)
{
   pMinimize_ = addAction("Minimize");
   pMinimize_->setShortcut(QKeySequence("Meta+M"));
   connect(pMinimize_, SIGNAL(triggered()),
           this, SLOT(onMinimize()));

   pZoom_ = addAction("Zoom");
   connect(pZoom_, SIGNAL(triggered()),
           this, SLOT(onZoom()));

   addSeparator();

   pWindowPlaceholder_ = addAction("__PLACEHOLDER__");
   pWindowPlaceholder_->setVisible(false);

   addSeparator();

   pBringAllToFront_ = addAction("Bring All to Front");
   connect(pBringAllToFront_, SIGNAL(triggered()),
           this, SLOT(onBringAllToFront()));

   connect(this, SIGNAL(aboutToShow()),
           this, SLOT(onAboutToShow()));
   connect(this, SIGNAL(aboutToHide()),
           this, SLOT(onAboutToHide()));
}

void WindowMenu::onMinimize()
{
   QWidget* pWin = QApplication::activeWindow();
   if (pWin)
   {
      pWin->setWindowState(Qt::WindowMinimized);
   }
}

void WindowMenu::onZoom()
{
   QWidget* pWin = QApplication::activeWindow();
   if (pWin)
   {
      pWin->setWindowState(pWin->windowState() ^ Qt::WindowMaximized);
   }
}

void WindowMenu::onBringAllToFront()
{
#ifdef Q_WS_MAC
   CFURLRef appUrlRef = CFBundleCopyBundleURL(CFBundleGetMainBundle());
   if (appUrlRef)
   {
      LSOpenCFURLRef(appUrlRef, NULL);
      CFRelease(appUrlRef);
   }
#endif
}

void WindowMenu::onAboutToShow()
{
   QWidget* win = QApplication::activeWindow();
   pMinimize_->setEnabled(win);
   pZoom_->setEnabled(win && win->maximumSize() != win->minimumSize());
   pBringAllToFront_->setEnabled(win);


   for (int i = windows_.size() - 1; i >= 0; i--)
   {
      QAction* pAction = windows_[i];
      removeAction(pAction);
      windows_.removeAt(i);
      pAction->deleteLater();
   }

   QWidgetList topLevels = QApplication::topLevelWidgets();
   for (int i = 0; i < topLevels.size(); i++)
   {
      QWidget* pWindow = topLevels.at(i);
      if (!pWindow->isVisible())
         continue;

      QAction* pAction = new QAction(pWindow->windowTitle(), pWindow);
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

void WindowMenu::onAboutToHide()
{
}

void WindowMenu::showWindow()
{
   QAction* pAction = qobject_cast<QAction*>(sender());
   if (!pAction)
      return;
   QWidget* pWidget = pAction->data().value<QWidget*>();
   if (!pWidget)
      return;
   if (pWidget->isMinimized())
      pWidget->setWindowState(pWidget->windowState() & ~Qt::WindowMinimized);
   pWidget->activateWindow();
}

} // namespace desktop
