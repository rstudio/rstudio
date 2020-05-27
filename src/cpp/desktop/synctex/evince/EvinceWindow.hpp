/*
 * EvinceWindow.hpp
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

#ifndef DESKTOP_SYNCTEX_EVINCEWINDOW_HPP
#define DESKTOP_SYNCTEX_EVINCEWINDOW_HPP

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

#include <DesktopSynctex.hpp>

namespace rstudio {
namespace desktop {
namespace synctex {

class EvinceWindow : public QDBusAbstractInterface
{
    Q_OBJECT
public:
    static inline const char *staticInterfaceName()
    { return "org.gnome.evince.Window"; }

public:
    EvinceWindow(const SynctexViewerInfo& viewerInfo,
                 const QString &service,
                 QObject *parent = 0);

    ~EvinceWindow();

public Q_SLOTS: // METHODS
    inline QDBusPendingReply<> SyncView(const QString &source_file, const QPoint &source_point, uint timestamp)
    {
       // get source file path
       QString srcFilePath = source_file;

       // fixup the source file to have a "/./" (required by Evince < 3.3.2
       // because it hadn't yet updated to version 1.17 of the synctex parser
       // which is much more liberal in path parsing)
       if (viewerInfo_.version() < QT_VERSION_CHECK(3,3,20))
       {
          QFileInfo srcFileInfo(srcFilePath);
          srcFilePath = srcFileInfo.canonicalPath() +
                        QString::fromUtf8("/./") +
                        srcFileInfo.fileName();
       }

       // invoke SyncView
       QList<QVariant> argumentList;
       argumentList << QVariant::fromValue(srcFilePath) << QVariant::fromValue(source_point) << QVariant::fromValue(timestamp);
       return asyncCallWithArgumentList(QLatin1String("SyncView"), argumentList);
    }

Q_SIGNALS: // SIGNALS
    void Closed();
    void DocumentLoaded(const QString &uri);
    void SyncSource(const QString &source_file, const QPoint &source_point, uint timestamp);

private:
    SynctexViewerInfo viewerInfo_;
};

} // namespace synctex
} // namespace desktop
} // namespace rstudio

namespace org {
  namespace gnome {
    namespace evince {
      typedef rstudio::desktop::synctex::EvinceWindow Window;
    }
  }
}

#endif // DESKTOP_SYNCTEX_EVINCEWINDOW_HPP
