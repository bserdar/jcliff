Name: 		      jcliff
Version: 	      2.12.6
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
%{_datadir}/jcliff/COPYING
%{_datadir}/jcliff/README.markdown
%{_datadir}/jcliff/cookcc-0.3.3.jar
%{_datadir}/jcliff/jboss-dmr-1.1.1.Final.jar
%{_datadir}/jcliff/jcliff
%{_datadir}/jcliff/jcliff-2.12.6
%{_datadir}/jcliff/jcliff-2.12.6.jar
%{_datadir}/jcliff/jcliff.bat
%{_datadir}/jcliff/junit-4.4.jar
%{_datadir}/jcliff/rules/datasource
%{_datadir}/jcliff/rules/ee
%{_datadir}/jcliff/rules/ejb3
%{_datadir}/jcliff/rules/extension
%{_datadir}/jcliff/rules/infinispan
%{_datadir}/jcliff/rules/interface
%{_datadir}/jcliff/rules/jdbc-driver
%{_datadir}/jcliff/rules/jgroups
%{_datadir}/jcliff/rules/jmx
%{_datadir}/jcliff/rules/logging
%{_datadir}/jcliff/rules/mail
%{_datadir}/jcliff/rules/messaging
%{_datadir}/jcliff/rules/naming
%{_datadir}/jcliff/rules/osgi
%{_datadir}/jcliff/rules/path
%{_datadir}/jcliff/rules/rbac
%{_datadir}/jcliff/rules/remoting
%{_datadir}/jcliff/rules/resource-adapter
%{_datadir}/jcliff/rules/rules
%{_datadir}/jcliff/rules/scanner
%{_datadir}/jcliff/rules/security
%{_datadir}/jcliff/rules/security-realms
%{_datadir}/jcliff/rules/standard-sockets
%{_datadir}/jcliff/rules/subsystem
%{_datadir}/jcliff/rules/system-properties
%{_datadir}/jcliff/rules/teiid
%{_datadir}/jcliff/rules/threads
%{_datadir}/jcliff/rules/transactions
%{_datadir}/jcliff/rules/undertow
%{_datadir}/jcliff/rules/web
%{_datadir}/jcliff/rules/webservices
%{_datadir}/jcliff/rules/xadatasource

%changelog
* Wed Dec 02 2020 Harsha Cherukuri <hcheruku@redhat.com> - 2.12.6-1
- Initial release for Fedora COPR
