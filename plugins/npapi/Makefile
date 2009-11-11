include ../config.mk

INC=-I. -I../common -Inpapi
CFLAGS=-Wall -g -O2 -fPIC $(INC) -rdynamic -m$(FLAG32BIT) -DLINUX -DBROWSER_NPAPI
CXXFLAGS=$(CFLAGS)

INSTDIR := extension/platform/Linux_$(ARCH)-gcc3/plugins

HDRS= LocalObjectTable.h NPVariantWrapper.h Plugin.h ScriptableInstance.h mozincludes.h \
	JavaObject.h ScriptableInstance.h

COMMON= ../common/libcommon$(FLAG32BIT).a
OBJS=	main.o Plugin.o LocalObjectTable.o JavaObject.o npn_bindings.o \
	ScriptableInstance.o

SRCS=	main.cpp Plugin.cpp LocalObjectTable.cpp JavaObject.cpp \
	npn_bindings.cc ScriptableInstance.cpp

VERSION ?= 0.9.$(shell ./getversion).$(shell date +%Y%m%d)

all:: versioned-files

signable: prebuilt/gwt-dev-plugin/manifest.json
	-rm -rf signable
	mkdir signable
	cd prebuilt; find gwt-dev-plugin -path */.svn -prune -o -print | cpio -pmdua ../signable

oophm.xpi: extension $(INSTDIR)/liboophm.so liboophm.so extension/install.rdf
	-rm -f oophm.xpi
	cd extension; zip -r -D -9 -o ../$@ * -x '*/.svn/*' -x 'META-INF/*'

oophm-signed.xpi: $(INSTDIR)/liboophm.so extension/install.rdf \
  extension/META-INF/zigbert.sf
	-rm -f $@
	cd extension; zip -9 ../$@ META-INF/zigbert.rsa; zip -r -D -9 -o ../$@ * -x '*/.svn/*' -x META-INF/zigbert.rsa

versioned-files::
	sed -e s/GWT_DEV_PLUGIN_VERSION/$(VERSION)/ manifest-template.json >prebuilt/gwt-dev-plugin/manifest.json
	sed -e s/GWT_DEV_PLUGIN_VERSION/$(VERSION)/ updates-template.xml >prebuilt/updates.xml

extension: prebuilt/extension
	-rm -rf extension
	cp -r --preserve=mode $< $@

$(INSTDIR):
	-mkdir -p $@

extension/META-INF/zigbert.sf: $(INSTDIR)/liboophm.so
	signtool -d certdb -k 'GWT Plugin' -x .svn extension/

extension/install.rdf: install-template.rdf version
	sed s/GWT_OOPHM_VERSION/`cat version`/ install-template.rdf >$@

version: computeversion $(HDRS) $(SRCS) $(COMMON)
	./computeversion >$@

$(INSTDIR)/liboophm.so: liboophm.so
	cp $< $@

liboophm.so: $(OBJS) $(COMMON) $(INSTDIR)
	g++ -m$(FLAG32BIT) -shared -o $@ $(OBJS) $(COMMON)

$(COMMON): common
	cd ../common; make

$(OBJS): 

.PHONY: all realclean clean depend common install install-platform versioned-files

install:: oophm.xpi
	-cp --preserve=mode $< prebuilt

install-platform:: liboophm.so
	-mkdir -p $(subst extension,prebuilt/extension,$(INSTDIR))
	-cp --preserve=mode $< $(subst extension,prebuilt/extension,$(INSTDIR))

realclean:: clean

clean::
	-rm -f $(OBJS)
	-rm -f extension/platform/*/plugins/*.{so,dll}

depend::
	g++ -MM $(CFLAGS) $(SRCS) >>Makefile
#	makedepend -- $(CFLAGS) -- $(SRCS)

# DO NOT DELETE
main.o: main.cpp ../common/Debug.h ../common/Platform.h \
  ../common/DebugLevel.h mozincludes.h npapi/npapi.h npapi/nphostapi.h \
  npapi/npapi.h npapi/npruntime.h npapi/npapi.h npapi/npruntime.h \
  NPObjectWrapper.h Plugin.h ScriptableInstance.h ../common/HostChannel.h ../common/Message.h \
  ../common/ReturnMessage.h ../common/BrowserChannel.h ../common/Value.h \
  ../common/Debug.h ../common/SessionHandler.h \
  ../common/LoadModuleMessage.h ../common/HostChannel.h \
  LocalObjectTable.h ../common/SessionHandler.h \
  ../common/scoped_ptr/scoped_ptr.h ../common/AllowedConnections.h
Plugin.o: Plugin.cpp Plugin.h ScriptableInstance.h ../common/Debug.h ../common/Platform.h \
  ../common/DebugLevel.h mozincludes.h npapi/npapi.h npapi/nphostapi.h \
  npapi/npapi.h npapi/npruntime.h npapi/npapi.h npapi/npruntime.h \
  NPObjectWrapper.h ../common/HostChannel.h ../common/Message.h \
  ../common/ReturnMessage.h ../common/BrowserChannel.h ../common/Value.h \
  ../common/Debug.h ../common/SessionHandler.h ../common/AllowedConnections.h \
  ../common/LoadModuleMessage.h ../common/HostChannel.h \
  LocalObjectTable.h ../common/SessionHandler.h ../common/InvokeMessage.h \
  ../common/ReturnMessage.h ../common/ServerMethods.h \
  ../common/scoped_ptr/scoped_ptr.h NPVariantWrapper.h \
  ../common/Platform.h ../common/Value.h JavaObject.h ../common/HashMap.h
ScriptableInstance.o: ScriptableInstance.cpp Plugin.h ScriptableInstance.h ../common/Debug.h \
  ../common/Platform.h \
  ../common/DebugLevel.h mozincludes.h npapi/npapi.h npapi/nphostapi.h \
  npapi/npapi.h npapi/npruntime.h npapi/npapi.h npapi/npruntime.h \
  NPObjectWrapper.h ../common/HostChannel.h ../common/Message.h \
  ../common/ReturnMessage.h ../common/BrowserChannel.h ../common/Value.h \
  ../common/Debug.h ../common/SessionHandler.h ../common/AllowedConnections.h \
  ../common/LoadModuleMessage.h ../common/HostChannel.h \
  LocalObjectTable.h ../common/SessionHandler.h ../common/InvokeMessage.h \
  ../common/ReturnMessage.h ../common/ServerMethods.h \
  ../common/scoped_ptr/scoped_ptr.h NPVariantWrapper.h \
  ../common/Platform.h ../common/Value.h JavaObject.h ../common/HashMap.h
LocalObjectTable.o: LocalObjectTable.cpp mozincludes.h npapi/npapi.h \
  npapi/nphostapi.h npapi/npapi.h npapi/npruntime.h npapi/npapi.h \
  npapi/npruntime.h NPObjectWrapper.h LocalObjectTable.h \
  ../common/Debug.h ../common/Platform.h ../common/DebugLevel.h
JavaObject.o: JavaObject.cpp JavaObject.h mozincludes.h npapi/npapi.h \
  npapi/nphostapi.h npapi/npapi.h npapi/npruntime.h npapi/npapi.h \
  npapi/npruntime.h NPObjectWrapper.h Plugin.h ScriptableInstance.h ../common/Debug.h \
  ../common/Platform.h ../common/DebugLevel.h ../common/HostChannel.h \
  ../common/Message.h ../common/ReturnMessage.h ../common/AllowedConnections.h \
  ../common/BrowserChannel.h ../common/Value.h ../common/Debug.h \
  ../common/SessionHandler.h ../common/LoadModuleMessage.h \
  ../common/HostChannel.h LocalObjectTable.h ../common/SessionHandler.h \
  ../common/HashMap.h NPVariantWrapper.h ../common/Platform.h \
  ../common/Value.h
npn_bindings.o: npn_bindings.cc mozincludes.h npapi/npapi.h \
  npapi/nphostapi.h npapi/npapi.h npapi/npruntime.h npapi/npapi.h \
  npapi/npruntime.h NPObjectWrapper.h
