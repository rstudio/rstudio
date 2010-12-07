/*
 * DesktopMenuCallback.hpp
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

#ifndef DESKTOP_MENU_CALLBACK_HPP
#define DESKTOP_MENU_CALLBACK_HPP

#include <QObject>
#include <QHash>
#include <QList>
#include <QMenu>
#include <QMenuBar>
#include <QStack>
#include <QKeyEvent>

namespace desktop {

class MenuCallback : public QObject
{
    Q_OBJECT
public:
    explicit MenuCallback(QObject *parent = 0);

public slots:
    void beginMainMenu();
    void beginMenu(QString label);
    void addCommand(QString commandId,
                    QString label,
                    QString tooltip,
                    QString shortcut);
    void addSeparator();
    void endMenu();
    void endMainMenu();
    void actionInvoked();

    void aboutToShowMenu();

signals:
    void menuBarCompleted(QMenuBar* menuBar);
    void manageCommand(QString commandId, QAction* action);
    void commandInvoked(QString commandId);

private:
    QMenuBar* pMainMenu_;
    QStack<QMenu*> menuStack_;
    QHash<QMenu*, QList<QAction*> > menuActions_;
};

class WindowMenu : public QMenu
{
   Q_OBJECT
public:
   explicit WindowMenu(QWidget *parent = 0);

protected slots:
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

#endif // DESKTOP_MENU_CALLBACK_HPP
