/*
 * DesktopApplicationLaunch.hpp
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

#ifndef DESKTOPAPPLICATIONLAUNCH_HPP
#define DESKTOPAPPLICATIONLAUNCH_HPP

#include <QObject>
#include <QWidget>
#include <QApplication>
#include <boost/scoped_ptr.hpp>

namespace desktop {

class ApplicationLaunch : public QWidget
{
    Q_OBJECT
public:
   static void init(QString appname,
                    int& argc,
                    char* argv[],
                    boost::scoped_ptr<QApplication>* ppApp,
                    boost::scoped_ptr<ApplicationLaunch>* ppAppLaunch);

   void setActivationWindow(QWidget* pWindow);

   void activateWindow();

   void attemptToRegisterPeer();

   QString startupOpenFileRequest() const;

protected:
    explicit ApplicationLaunch();
#ifdef _WIN32
    bool nativeEvent(const QByteArray & eventType,
                     void * message,
                     long * result);
#endif

signals:
    void openFileRequest(QString filename);

public slots:
    bool sendMessage(QString filename);

private:
    QWidget* pMainWindow_;
};

} // namespace desktop

#endif // DESKTOPAPPLICATIONLAUNCH_HPP
