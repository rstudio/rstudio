/*
 * DesktopPosixApplication.hpp
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

protected:
    virtual bool event(QEvent* pEvent);
};



} // namespace desktop

#endif // DESKTOP_POSIX_APPLICATION_HPP
