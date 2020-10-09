/*
 * DesktopMenuCallback.cpp
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

#include "DesktopMenuCallback.hpp"

#include <core/Algorithm.hpp>

#include <QDebug>
#include <QApplication>
#include <QWindow>

namespace rstudio {
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
   if (label == QString::fromUtf8("&Help"))
   {
      pMainMenu_->addMenu(new WindowMenu(pMainMenu_));
   }
#endif

   auto* pMenu = new SubMenu(label, pMainMenu_);
   pMenu->setSeparatorsCollapsible(true);

   if (menuStack_.count() == 0)
      pMainMenu_->addMenu(pMenu);
   else
      menuStack_.top()->addMenu(pMenu);

   menuStack_.push(pMenu);
}

QAction* MenuCallback::addCustomAction(QString commandId,
                                       QString label,
                                       QString tooltip,
                                       QKeySequence keySequence,
                                       bool checkable)
{

   QAction* pAction = nullptr;

#ifdef Q_OS_MAC
   // On Mac, certain commands will be automatically moved to Application Menu by Qt. If we want them to also
   // appear in RStudio menus, check for them here and return nullptr.
   if (duplicateAppMenuAction(QString::fromUtf8("showAboutDialog"),
                              commandId, label, tooltip, keySequence, checkable))
   {
      return nullptr;
   }
   else if (duplicateAppMenuAction(QString::fromUtf8("quitSession"),
                              commandId, label, tooltip, keySequence, checkable))
   {
      return nullptr;
   }

   // If we want a command to not be automatically moved to Application Menu, include it here and return the
   // created action.
   pAction = duplicateAppMenuAction(QString::fromUtf8("buildToolsProjectSetup"),
                                    commandId, label, tooltip, keySequence, checkable);
   if (pAction)
      return pAction;

#endif // Q_OS_MAC

   // this silly branch exists just so one doesn't have to worry about whether
   // we should be prefixing with 'else' in later conditionals
   if (false)
   {
   }
   
   // on macOS, these bindings are hooked up on the GWT side (mainly to ensure
   // that zoom requests targetted to a GWT window work as expected)
#ifndef Q_OS_MAC
   else if (commandId == QStringLiteral("zoomActualSize"))
   {
      pAction = menuStack_.top()->addAction(
               QIcon(),
               label,
               this,
               SIGNAL(zoomActualSize()),
               QKeySequence(Qt::CTRL + Qt::Key_0));
   }
   else if (commandId == QStringLiteral("zoomIn"))
   {
      pAction = menuStack_.top()->addAction(
               QIcon(),
               label,
               this,
               SIGNAL(zoomIn()),
               QKeySequence::ZoomIn);
   }
   else if (commandId == QStringLiteral("zoomOut"))
   {
      pAction = menuStack_.top()->addAction(
               QIcon(),
               label,
               this,
               SIGNAL(zoomOut()),
               QKeySequence::ZoomOut);
   }
#endif
   
#ifdef Q_OS_MAC
   // NOTE: even though we seem to be using Meta as a modifier key here, Qt
   // will translate that to CTRL (but only for Ctrl+Tab and Ctrl+Shift+Tab)
   // TODO: using actionInvoke() also flashes the menu bar; that feels a little
   // too aggressive for this command?
   else if (commandId == QStringLiteral("nextTab"))
   {
      pAction = menuStack_.top()->addAction(
               QIcon(),
               label,
               this,
               SLOT(actionInvoked()),
               QKeySequence(Qt::META + Qt::Key_Tab));
   }
   else if (commandId == QStringLiteral("previousTab"))
   {
      pAction = menuStack_.top()->addAction(
               QIcon(),
               label,
               this,
               SLOT(actionInvoked()),
               QKeySequence(Qt::SHIFT + Qt::META + Qt::Key_Tab));
   }
#endif
#ifdef Q_OS_LINUX
   else if (commandId == QString::fromUtf8("nextTab"))
   {
      pAction = menuStack_.top()->addAction(QIcon(),
                                            label,
                                            this,
                                            SLOT(actionInvoked()),
                                            QKeySequence(Qt::CTRL +
                                                         Qt::Key_PageDown));
   }
   else if (commandId == QString::fromUtf8("previousTab"))
   {
      pAction = menuStack_.top()->addAction(QIcon(),
                                            label,
                                            this,
                                            SLOT(actionInvoked()),
                                            QKeySequence(Qt::CTRL +
                                                         Qt::Key_PageUp));
   }
#endif

   if (pAction != nullptr)
   {
      pAction->setData(commandId);
      pAction->setToolTip(tooltip);
      return pAction;
   }
   else
   {
      return nullptr;
   }
}

QAction* MenuCallback::duplicateAppMenuAction(QString commandToDuplicate,
                                              QString commandId,
                                              QString label,
                                              QString tooltip,
                                              QKeySequence keySequence,
                                              bool checkable)
{
   QAction* pAction = nullptr;
   if (commandId == commandToDuplicate)
   {
      pAction = new QAction(QIcon(), label);
      pAction->setMenuRole(QAction::NoRole);
      pAction->setData(commandId);
      pAction->setToolTip(tooltip);
      pAction->setShortcut(keySequence);
      if (checkable)
         pAction->setCheckable(true);

      menuStack_.top()->addAction(pAction);

      auto* pBinder = new MenuActionBinder(menuStack_.top(), pAction);
      connect(pBinder, SIGNAL(manageCommand(QString, QAction * )), this, SIGNAL(manageCommand(QString, QAction * )));
      connect(pAction, SIGNAL(triggered()), this, SLOT(actionInvoked()));
   }
   return pAction;
}

void MenuCallback::addCommand(QString commandId,
                              QString label,
                              QString tooltip,
                              QString shortcut,
                              bool checkable)
{

#ifdef Q_OS_MAC
   // on macOS, replace instances of 'Ctrl' with 'Meta'; QKeySequence renders "Ctrl" using the
   // macOS command symbol, but we want the menu to show the literal Ctrl symbol (^)
   shortcut.replace(QStringLiteral("Ctrl"), QStringLiteral("Meta"));

   // on Mac the enter key and return keys have different symbols
   // https://github.com/rstudio/rstudio/issues/6524
   shortcut.replace(QStringLiteral("Enter"), QStringLiteral("Return"));
#endif

   // replace instances of 'Cmd' with 'Ctrl' -- note that on macOS
   // Qt automatically maps that to the Command key
   shortcut.replace(QStringLiteral("Cmd"), QStringLiteral("Ctrl"));
   
   QKeySequence keySequence(shortcut);

   // some shortcuts (namely, the Edit shortcuts) don't have bindings on the client side.
   // populate those here when discovered
   if (commandId == QStringLiteral("cutDummy"))
   {
      keySequence = QKeySequence(QKeySequence::Cut);
   }
   else if (commandId == QStringLiteral("copyDummy"))
   {
      keySequence = QKeySequence(QKeySequence::Copy);
   }
   else if (commandId == QStringLiteral("pasteDummy"))
   {
      keySequence = QKeySequence(QKeySequence::Paste);
   }
   else if (commandId == QStringLiteral("pasteWithIndentDummy"))
   {
      keySequence = QKeySequence(Qt::CTRL + Qt::SHIFT + Qt::Key_V);
   }
   else if (commandId == QStringLiteral("undoDummy"))
   {
      keySequence = QKeySequence(QKeySequence::Undo);
   }
   else if (commandId == QStringLiteral("redoDummy"))
   {
      keySequence = QKeySequence(QKeySequence::Redo);
   }

#ifndef Q_OS_MAC
   if (shortcut.contains(QString::fromUtf8("\n")))
   {
      int value = (keySequence[0] & Qt::MODIFIER_MASK) + Qt::Key_Enter;
      keySequence = QKeySequence(value);
   }
#endif

   // allow custom action handlers first shot
   QPointer<QAction> pAction =
         addCustomAction(commandId, label, tooltip, keySequence, checkable);

   // if there was no custom handler then do stock command-id processing
   if (pAction == nullptr)
   {
      pAction = menuStack_.top()->addAction(QIcon(),
                                            label,
                                            this,
                                            SLOT(actionInvoked()),
                                            keySequence);
      pAction->setData(commandId);
      pAction->setToolTip(tooltip);
      if (checkable)
         pAction->setCheckable(true);

      auto * pBinder = new MenuActionBinder(menuStack_.top(), pAction);
      connect(pBinder, SIGNAL(manageCommand(QString,QAction*)),
              this, SIGNAL(manageCommand(QString,QAction*)));
   }

   // remember action for later
   actions_[commandId].push_back(pAction);
}

void MenuCallback::actionInvoked()
{
   auto * action = qobject_cast<QAction*>(sender());
   QString commandId = action->data().toString();
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

namespace {

template <typename T, typename F>
void setCommandProperty(T& actions, QString commandId, F&& setter)
{
   auto it = actions.find(commandId);
   if (it == actions.end())
       return;

   // NOTE: in some cases actions from a previous RStudio session
   // can leak into the map; we normally prune those each time a
   // new page is loaded but just to be careful we validate that
   // we have non-null pointers before operating on them
   for (auto& pAction : it.value())
      if (pAction)
         setter(pAction);
}

} // end anonymous namespace

void MenuCallback::setCommandEnabled(QString commandId, bool enabled)
{
   setCommandProperty(actions_, commandId, [=](QPointer<QAction> pAction) {
      pAction->setEnabled(enabled);
   });
}

void MenuCallback::setCommandVisible(QString commandId, bool visible)
{
   setCommandProperty(actions_, commandId, [=](QPointer<QAction> pAction) {
      pAction->setVisible(visible);
   });
}

void MenuCallback::setCommandLabel(QString commandId, QString label)
{
   setCommandProperty(actions_, commandId, [=](QPointer<QAction> pAction) {
      pAction->setText(label);
   });
}

void MenuCallback::setCommandChecked(QString commandId, bool checked)
{
   setCommandProperty(actions_, commandId, [=](QPointer<QAction> pAction) {
      pAction->setChecked(checked);
   });
}

void MenuCallback::setMainMenuEnabled(bool enabled)
{
   if (pMainMenu_)
      pMainMenu_->setEnabled(enabled);
}

void MenuCallback::cleanUpActions()
{
   for (auto& actions : actions_.values())
   {
      core::algorithm::expel_if(actions, [](QPointer<QAction> pAction) {
         return pAction.isNull();
      });
   }
}

MenuActionBinder::MenuActionBinder(QMenu* pMenu, QAction* pAction) : QObject(pAction)
{
   connect(pMenu, SIGNAL(aboutToShow()), this, SLOT(onShowMenu()));
   connect(pMenu, SIGNAL(aboutToHide()), this, SLOT(onHideMenu()));
   pAction_ = pAction;
   keySequence_ = pAction->shortcut();
   pAction->setShortcut(QKeySequence());
}

void MenuActionBinder::onShowMenu()
{
   QString commandId = pAction_->data().toString();
   pAction_->setShortcut(keySequence_);
}

void MenuActionBinder::onHideMenu()
{
   pAction_->setShortcut(QKeySequence());
}

WindowMenu::WindowMenu(QWidget *parent) : QMenu(QString::fromUtf8("&Window"), parent)
{
   // NOTE: CTRL means META on macOS
   pMinimize_ = addAction(QString::fromUtf8("Minimize"));
   pMinimize_->setShortcut(Qt::CTRL + Qt::Key_M);
   connect(pMinimize_, SIGNAL(triggered()),
           this, SLOT(onMinimize()));

   pZoom_ = addAction(QString::fromUtf8("Zoom"));
   connect(pZoom_, SIGNAL(triggered()),
           this, SLOT(onZoom()));

   addSeparator();

   pWindowPlaceholder_ = addAction(QString::fromUtf8("__PLACEHOLDER__"));
   pWindowPlaceholder_->setVisible(false);

   addSeparator();

   pBringAllToFront_ = addAction(QString::fromUtf8("Bring All to Front"));
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
#ifdef Q_OS_MAC
   for (QWindow* appWindow : qApp->allWindows())
   {
      appWindow->raise();
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
   for (auto pWindow : topLevels)
   {
      if (!pWindow->isVisible())
         continue;

      // construct with no parent (we free it manually)
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

void WindowMenu::onAboutToHide()
{
}

void WindowMenu::showWindow()
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
