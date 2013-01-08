/*
 * DesktopCommandInvoker.hpp
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

#ifndef DESKTOP_COMMAND_INVOKER_HPP
#define DESKTOP_COMMAND_INVOKER_HPP

#include <QObject>

namespace desktop {

class CommandInvoker : public QObject
{
    Q_OBJECT
public:
    explicit CommandInvoker(QString commandId, QObject *parent = 0);

signals:
    void commandInvoked(QString commandId);

public slots:
    void invoke();

private:
    QString commandId_;
};

} // namespace desktop

#endif // DESKTOP_COMMAND_INVOKER_HPP
