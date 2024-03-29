Name: 		      jcliff
Version: 	      2.12.8
Release: 	      1%{?dist}
Summary: 	      JBoss configuration client front-end
License: 	      2013, Red Hat, Inc. and/or its affiliates.
Group: 		      Applications/File
URL:           	https://github.com/bserdar/jcliff
Source0:       	https://github.com/bserdar/jcliff/releases/download/v%{version}/%{name}-%{version}-dist.tar.gz
BuildArch:      noarch
BuildRoot: 	    %{_tmppath}/%{name}-%{namedversion}-%{release}-root
BuildRequires:  unzip

%description
Jcliff configures a running instance of EAP6/JBoss7 using modular configuration files.

%prep
%autosetup -n jcliff-%{version}
tar -xf %{_sourcedir}/%{name}-%{version}-dist.tar.gz -C %{_sourcedir}

%install
mkdir -p %{buildroot}/%{_datadir}/%{name}-%{version}
mkdir -p %{buildroot}/%{_bindir}
cp -R %{_sourcedir}/%{name}-%{version}/* %{buildroot}/%{_datadir}/%{name}-%{version}/
ln -sf %{_datadir}/%{name}-%{version}  %{buildroot}%{_datadir}/jcliff
ln -sf %{_datadir}/%{name}-%{version}/jcliff  %{buildroot}%{_bindir}/jcliff
%clean
rm -rf %{buildroot}

%files
%{_datadir}/%{name}-%{version}/*
%{_datadir}/jcliff
%{_bindir}/jcliff

%changelog
* Wed Dec 02 2020 Harsha Cherukuri <hcheruku@redhat.com> - 2.12.7-1
- Initial release for Fedora COPR
