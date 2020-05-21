/*
 * DesktopWindowTracker.hpp
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

#ifndef DESKTOP_WINDOW_TRACKER_HPP
#define DESKTOP_WINDOW_TRACKER_HPP

#include <QtCore>
#include <QMainWindow>
#include <QMap>

#include "DesktopBrowserWindow.hpp"

namespace rstudio {
namespace desktop {

class WindowTracker : public QObject
{
    Q_OBJECT
public:
    explicit WindowTracker(QObject *parent = nullptr);

    BrowserWindow* getWindow(QString key);
    void addWindow(QString key, BrowserWindow* window);

protected Q_SLOTS:
    void onWindowDestroyed(QString key);

private:
    QMap<QString, BrowserWindow*> map_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_WINDOW_TRACKER_HPP
