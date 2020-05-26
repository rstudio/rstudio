/*
 * EvinceDaemon.hpp
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

#ifndef DESKTOP_SYNCTEX_EVINCEDAEMON_HPP
#define DESKTOP_SYNCTEX_EVINCEDAEMON_HPP

#undef QT_NO_CAST_FROM_ASCII
#include <QtCore/QObject>
#include <QtCore/QByteArray>
#include <QtCore/QList>
#include <QtCore/QMap>
#include <QtCore/QString>
#include <QtCore/QStringList>
#include <QtCore/QVariant>
#include <QtDBus/QtDBus>
#define QT_NO_CAST_FROM_ASCII

namespace rstudio {
namespace desktop {
namespace synctex {

class EvinceDaemon : public QDBusAbstractInterface
{
    Q_OBJECT
public:
    static inline const char *staticInterfaceName()
    { return "org.gnome.evince.Daemon"; }

public:
    EvinceDaemon(QObject *parent = 0);

    ~EvinceDaemon();

public Q_SLOTS: // METHODS
    inline QDBusPendingReply<QString> FindDocument(const QString &uri, bool spawn)
    {
        QList<QVariant> argumentList;
        argumentList << QVariant::fromValue(uri) << QVariant::fromValue(spawn);
        return asyncCallWithArgumentList(QLatin1String("FindDocument"), argumentList);
    }

Q_SIGNALS: // SIGNALS
};

} // namespace synctex
} // namespace desktop
} // namespace rstudio

namespace org {
  namespace gnome {
    namespace evince {
      typedef rstudio::desktop::synctex::EvinceDaemon Daemon;
    }
  }
}

#endif // DESKTOP_SYNCTEX_EVINCEDAEMON_HPP
