/*
 * DesktopWindowTracker.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef DESKTOP_WINDOW_TRACKER_HPP
#define DESKTOP_WINDOW_TRACKER_HPP

#include <QtCore>
#include <QMainWindow>
#include <QMap>

#include "DesktopBrowserWindow.hpp"

namespace desktop {

class WindowTracker : public QObject
{
    Q_OBJECT
public:
    explicit WindowTracker(QObject *parent = 0);

    BrowserWindow* getWindow(QString key);
    void addWindow(QString key, BrowserWindow* window);

protected slots:
    void onWindowDestroyed(QString key);

private:
    QMap<QString, BrowserWindow*> map_;
};

} // namespace desktop

#endif // DESKTOP_WINDOW_TRACKER_HPP
