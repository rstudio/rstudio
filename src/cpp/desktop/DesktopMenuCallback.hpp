/*
 * DesktopMenuCallback.hpp
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

#ifndef DESKTOP_MENU_CALLBACK_HPP
#define DESKTOP_MENU_CALLBACK_HPP

#include <QObject>
#include <QHash>
#include <QList>
#include <QMenu>
#include <QMenuBar>
#include <QPointer>
#include <QStack>
#include <QKeyEvent>
#include <DesktopSubMenu.hpp>

namespace rstudio {
namespace desktop {

class MenuCallback : public QObject
{
    Q_OBJECT
public:
    explicit MenuCallback(QObject *parent = nullptr);

public Q_SLOTS:
    // menu-building commands
    void beginMainMenu();
    void beginMenu(QString label);
    void addCommand(QString commandId,
                    QString label,
                    QString tooltip,
                    QString shortcut,
                    bool isCheckable);
    void addSeparator();
    void endMenu();
    void endMainMenu();
    void actionInvoked();

    // runtime command state drivers
    void setCommandEnabled(QString commandId, bool enabled);
    void setCommandVisible(QString commandId, bool visible);
    void setCommandLabel(QString commandId, QString label);
    void setCommandChecked(QString commandId, bool checked);
    void setMainMenuEnabled(bool enabled);

    // other slots
    void cleanUpActions();

Q_SIGNALS:
    void menuBarCompleted(QMenuBar* menuBar);
    void manageCommand(QString commandId, QAction* action);
    void manageCommandVisibility(QString commandId, QAction* action);
    void commandInvoked(QString commandId);

    void zoomActualSize();
    void zoomIn();
    void zoomOut();

private:
    QAction* addCustomAction(QString commandId,
                             QString label,
                             QString tooltip,
                             QKeySequence keySequence,
                             bool checkable);

    QAction* duplicateAppMenuAction(QString commandToDuplicate,
                                    QString commandId,
                                    QString label,
                                    QString tooltip,
                                    QKeySequence keySequence,
                                    bool checkable);
private:
    QMenuBar* pMainMenu_ = nullptr;
    QStack<SubMenu*> menuStack_;
    QMap<QString, QVector<QPointer<QAction>>> actions_;
};

/* Previously, in desktop mode, many keyboard shortcuts were handled by Qt,
 * by way of the keyboard shortcuts being added to QAction objects which
 * appear on menus. This is problematic because the keyboard shortcut handling
 * is thus not managed using the same code as web mode; most seriously, when
 * modal dialogs are shown, the keyboard shortcuts still work.
 *
 * Rather than try to keep the QActions in sync with commands, which seems
 * like it would be very chatty, this class ensures the QActions don't have
 * shortcuts assigned to them--except while their menus are showing. It also
 * ensures that commands are "managed" right before they are shown.
 */
class MenuActionBinder : public QObject
{
   Q_OBJECT
public:
   MenuActionBinder(QMenu* pMenu, QAction* action);

public Q_SLOTS:
   void onShowMenu();
   void onHideMenu();

Q_SIGNALS:
   void manageCommand(QString commandId, QAction* action);

private:
   QAction* pAction_;
   QKeySequence keySequence_;
};

class WindowMenu : public QMenu
{
   Q_OBJECT
public:
   explicit WindowMenu(QWidget *parent = nullptr);

protected Q_SLOTS:
   void onMinimize();
   void onZoom();
   void onBringAllToFront();
   void onAboutToShow();
   void onAboutToHide();
   void showWindow();

private:
   QAction* pMinimize_;
   QAction* pZoom_;
   QAction* pBringAllToFront_;
   QAction* pWindowPlaceholder_;
   QList<QAction*> windows_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_MENU_CALLBACK_HPP
