#!/bin/bash

# Write Synctex. Note that this version of the Synctex interface applies to
# Evince >= 2.9.1 (the one bundled with Gnome 3). If we want to support the
# Synctex API for Gnome 2 then we need a different defintion (I believe the
# only change was the addition of the timestamp parameter)
#
# Another consideration -- Evince 3.3.2 included an update to Synctex 1.17
# which eliminated problems with requiring /./ in paths. This version of
# Evince is only now shipping with Ubuntu so we probably need to also feed
# Evince the old-school style paths
#
# From: git://git.gnome.org/evince - df0bee73615912475d318c4772581af760dc833a
#
# Note we changed the type source_point from '(ii)' to 's' because we 
# couldn't figure out how to get the annotation element to work to specify
# QPoint as the type for '(ii)'. As a result we then needed to hand-edit
# the generated EvinceWindow.h file to use QPoint rather than QString as
# the type for the source_point arguments.
#

# utility for generating qt proxy classes
QDBUSXML2CPP=/opt/RStudio-QtSDK/Desktop/Qt/4.8.0/gcc/bin/qdbusxml2cpp

# daemon api
EVINCE_DAEMON_XML=org.gnome.evince.Daemon.xml
cat <<"EOF" > ${EVINCE_DAEMON_XML}
<?xml version="1.0" encoding="UTF-8" ?>
<node>
  <interface name="org.gnome.evince.Daemon">
    <method name="FindDocument">
      <arg type="s" name="uri" direction="in">
      </arg>
      <arg type="b" name="spawn" direction="in">
      </arg>
      <arg type="s" name="owner" direction="out">
      </arg>
    </method>
  </interface>
</node>
EOF
${QDBUSXML2CPP} -v -c EvinceDaemon -p EvinceDaemon.h:EvinceDaemon.cpp ${EVINCE_DAEMON_XML}

# window api
EVINCE_WINDOW_XML=org.gnome.evince.Window.xml
cat <<"EOF" > ${EVINCE_WINDOW_XML}
<?xml version="1.0" encoding="UTF-8" ?>
<node>
  <interface name="org.gnome.evince.Window">
    <method name="SyncView">
      <arg type="s" name="source_file" direction="in"/>
      <!-- NOTE: this arg requires hand editing to QPoint in the generated file (see comment above) -->
      <arg type="s" name="source_point" direction="in"/>
      <arg type="u" name="timestamp" direction="in"/>
    </method>
    <signal name="SyncSource">
      <arg type="s" name="source_file" direction="out"/>
      <!-- NOTE: this arg requires hand editing to QPoint in the generated file (see comment above) -->
      <arg type="s" name="source_point" direction="out"/>
      <arg type="u" name="timestamp" direction="out"/>
    </signal>
    <signal name="Closed"/>
    <signal name="DocumentLoaded">
      <arg type="s" name="uri" direction="out"/>
    </signal>
  </interface>
</node>
EOF
${QDBUSXML2CPP} -c EvinceWindow -p EvinceWindow.h:EvinceWindow.cpp ${EVINCE_WINDOW_XML}



