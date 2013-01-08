/*
 * DesktopPosixApplication.hpp
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

#ifndef DESKTOP_POSIX_APPLICATION_HPP
#define DESKTOP_POSIX_APPLICATION_HPP

#include "3rdparty/qtsingleapplication/QtSingleApplication"

namespace desktop {

class PosixApplication : public QtSingleApplication
{
    Q_OBJECT
public:

   PosixApplication(QString appName, int& argc, char* argv[])
    : QtSingleApplication(appName, argc, argv)
   {
      setApplicationName(appName);
   }

   QString startupOpenFileRequest() const
   {
      return startupOpenFileRequest_;
   }

signals:
    void openFileRequest(QString filename);

protected:
    virtual bool event(QEvent* pEvent);

private:
    QString startupOpenFileRequest_;

};



} // namespace desktop

#endif // DESKTOP_POSIX_APPLICATION_HPP
