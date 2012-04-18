/*
 * DesktopSecondaryWindow.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef DESKTOP_SECONDARY_WINDOW_HPP
#define DESKTOP_SECONDARY_WINDOW_HPP

#include <QMainWindow>
#include <QtWebKit>
#include "DesktopBrowserWindow.hpp"

namespace desktop {

class SecondaryWindow : public BrowserWindow
{
    Q_OBJECT
public:
    explicit SecondaryWindow(QUrl baseUrl, QWidget* pParent = NULL);

signals:

public slots:
    void print();

 protected slots:
    virtual void manageCommandState();

private:
    QAction* back_;
    QAction* forward_;
    QAction* reload_;
    QAction* print_;

    QWebHistory* history_;
};

} // namespace desktop

#endif // DESKTOP_SECONDARY_WINDOW_HPP
