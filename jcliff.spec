Name: 		      jcliff
Version: 	      2.12.6
Release: 	      1%{?dist}
Summary: 	      JBoss configuration client front-end
License: 	      2013, Red Hat, Inc. and/or its affiliates.
Group: 		      Applications/File
URL:           	https://github.com/bserdar/jcliff
Source0:       	https://github.com/bserdar/jcliff/archive/v%{version}.tar.gz
BuildArch:      noarch
BuildRoot: 	    %{_tmppath}/%{name}-%{namedversion}-%{release}-root
BuildRequires:  unzip

%description
Jcliff configures a running instance of EAP6/JBoss7 using modular configuration files.

%prep
tar -xf %{_sourcedir}/%{name}-%{version}-dist.tar.gz -C %{_sourcedir}

%install
mkdir -p %{buildroot}/%{_datadir}/jcliff
mkdir -p %{buildroot}/%{_bindir}/jcliff
mv %{_sourcedir}/%{name}-%{version}/* %{buildroot}/%{_datadir}/jcliff
ls %{buildroot}/%{_datadir}/jcliff
ln -s %{_datadir}/%{name}-%{version} %{buildroot}%{_datadir}/jcliff
ln -s %{_datadir}/%{name}-%{version}/jcliff %{buildroot}%{_bindir}/jcliff

%files
%defattr(0644,root,root,0755)
%{_bindir}/jcliff/jcliff
%{_datadir}/jcliff/
%{_datadir}/jcliff/rules/

%changelog
* Wed Dec 02 2020 Harsha Cherukuri <hcheruku@redhat.com> - 2.12.6-1
- Initial release for Fedora COPR
