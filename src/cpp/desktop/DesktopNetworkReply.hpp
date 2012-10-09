/*
 * DesktopNetworkReply.hpp
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

#ifndef DESKTOPNETWORKREPLY_HPP
#define DESKTOPNETWORKREPLY_HPP

#include <QNetworkReply>

class DesktopNetworkReply : public QNetworkReply
{
   Q_OBJECT
public:
   explicit DesktopNetworkReply(QObject *parent = 0);
   
signals:
   
public slots:
   
};

#endif // DESKTOPNETWORKREPLY_HPP
