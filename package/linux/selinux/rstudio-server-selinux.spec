# vim: sw=4:ts=4:et

%define relabel_files() \
restorecon -R %{_libdir}/rstudio-server/bin/*; \

%define selinux_policyver 38.1.45-3

Name:   rstudio-server-selinux
Version:	1.0
Release:	1%{?dist}
Summary:	SELinux policy module for rstudio

Group:	Development/Tools
Vendor:		Posit Software
License:	AGPLv2
URL:		https://posit.co
Source0:	rstudio.pp
Source1:	rstudio.if
Source2:	rstudio_selinux.8


Requires: policycoreutils-python-utils, libselinux-utils, rstudio-server
Requires(post): selinux-policy-base >= %{selinux_policyver}, policycoreutils-python-utils
Requires(postun): policycoreutils-python-utils
BuildArch: noarch

%description
This package installs and sets up the SELinux policy security module for rstudio_server.

%install
install -d %{buildroot}%{_datadir}/selinux/packages
install -m 644 %{SOURCE0} %{buildroot}%{_datadir}/selinux/packages
install -d %{buildroot}%{_datadir}/selinux/devel/include/contrib
install -m 644 %{SOURCE1} %{buildroot}%{_datadir}/selinux/devel/include/contrib/
install -d %{buildroot}%{_mandir}/man8/
install -m 644 %{SOURCE2} %{buildroot}%{_mandir}/man8/rstudio_selinux.8
install -d %{buildroot}/etc/selinux/targeted/contexts/users/


%post
semodule -n -i %{_datadir}/selinux/packages/rstudio.pp

if /usr/sbin/selinuxenabled ; then
    /usr/sbin/load_policy
    %relabel_files
fi
exit 0

%postun
if [ $1 -eq 0 ]; then
    semodule -n -r rstudio
    if /usr/sbin/selinuxenabled ; then
       /usr/sbin/load_policy
       %relabel_files
    fi
fi
exit 0

%files
%attr(0600,root,root) %{_datadir}/selinux/packages/rstudio.pp
%{_mandir}/man8/rstudio_selinux.8
%{_mandir}/man8/rstudio_server_selinux.8


%changelog
* Wed Nov 20 2024 Adam Higerd <adam.higerd@posit.co> 1.0-1
- Initial version
