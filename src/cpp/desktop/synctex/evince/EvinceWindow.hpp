/*
 * EvinceWindow.hpp
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

#ifndef DESKTOP_SYNCTEX_EVINCEWINDOW_HPP
#define DESKTOP_SYNCTEX_EVINCEWINDOW_HPP

#include <QtCore/QObject>
#include <QtCore/QByteArray>
#include <QtCore/QList>
#include <QtCore/QMap>
#include <QtCore/QString>
#include <QtCore/QStringList>
#include <QtCore/QVariant>
#include <QtDBus/QtDBus>

namespace desktop {
namespace synctex {

class EvinceWindow : public QDBusAbstractInterface
{
    Q_OBJECT
public:
    static inline const char *staticInterfaceName()
    { return "org.gnome.evince.Window"; }

public:
    EvinceWindow(const QString &service, QObject *parent = 0);

    ~EvinceWindow();

public Q_SLOTS: // METHODS
    inline QDBusPendingReply<> SyncView(const QString &source_file, const QPoint &source_point, uint timestamp)
    {
        QList<QVariant> argumentList;
        argumentList << QVariant::fromValue(source_file) << QVariant::fromValue(source_point) << QVariant::fromValue(timestamp);
        return asyncCallWithArgumentList(QLatin1String("SyncView"), argumentList);
    }

Q_SIGNALS: // SIGNALS
    void Closed();
    void DocumentLoaded(const QString &uri);
    void SyncSource(const QString &source_file, const QPoint &source_point, uint timestamp);
};

} // namespace synctex
} // namespace desktop

namespace org {
  namespace gnome {
    namespace evince {
      typedef desktop::synctex::EvinceWindow Window;
    }
  }
}

#endif // DESKTOP_SYNCTEX_EVINCEWINDOW_HPP
