QT      +=  webkit network
HEADERS =   mainwindow.h \
    rswebview.h \
    secondarywindow.h \
    browserwindow.h \
    downloadhelper.h
SOURCES =   main.cpp \
            mainwindow.cpp \
    rswebview.cpp \
    secondarywindow.cpp \
    browserwindow.cpp \
    downloadhelper.cpp
RESOURCES = \
    assets.qrc

# install
target.path = $$[QT_INSTALL_EXAMPLES]/webkit/fancybrowser
sources.files = $$SOURCES $$HEADERS $$RESOURCES *.pro
sources.path = $$[QT_INSTALL_EXAMPLES]/webkit/fancybrowser
INSTALLS += target sources

symbian {
    TARGET.UID3 = 0xA000CF6C
    include($$QT_SOURCE_TREE/examples/symbianpkgrules.pri)
}

FORMS +=
