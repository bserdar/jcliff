Name: 		jcliff
Version: 	2.12.6
Release: 	1%{?dist}
Summary: 	JBoss configuration client front-end
License: 	2013, Red Hat, Inc. and/or its affiliates.
Group: 		Applications/File
URL:           	https://github.com/bserdar/jcliff
Source0:       	https://github.com/bserdar/jcliff/releases/download/v%{version}/%{name}-%{version}-dist.tar.gz
BuildArch:      noarch
BuildRoot: 	%{_tmppath}/%{name}-%{namedversion}-%{release}-root
BuildRequires:  unzip


%description
Jcliff configures a running instance of EAP6/JBoss7 using modular configuration files.

%prep
tar -xf %{_sourcedir}/%{name}-%{version}-dist.tar.gz -C %{_sourcedir}

%install

mv %{_sourcedir}/%{name}-%{version} %{buildroot}
ln -s %{_datadir}/%{name}-%{verison} %{buildroot}/%{_datadir}/jcliff
ln -s %{_datadir}/%{name}-%{verison}/jcliff %{buildroot}/%{_datadir}/jcliff

sudo mkdir -p %{_datadir}/%{name}-%{version}
cp %{_sourcedir}/%{name}-%{version}/* %{_datadir}/%{name}-%{version}/

%files
%defattr(-,root,root,755)
%doc LICENSE
%dir %attr(755,root,root) "%{_datadir}/%{name}-%{version}"
  "%{_datadir}/%{name}-%{version}/cookcc-0.3.3.jar"
  "%{_datadir}/%{name}-%{version}/jboss-dmr-1.1.1.Final.jar"
  "%{_datadir}/%{name}-%{version}/jcliff-2.12.6.jar"
  "%{_datadir}/%{name}-%{version}/junit-4.4.jar"
%dir %attr(755,root,root) "%{_datadir}/%{name}-%{version}/rules"
 "%{_datadir}/%{name}-%{version}/rules"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/jcliff"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/mail"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/rules"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/security"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/scanner"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/osgi"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/jgroups"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/ee"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/ejb3"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/jmx"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/extension"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/standard-sockets"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/messaging"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/infinispan"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/teiid"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/system-properties"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/datasource"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/interface"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/undertow"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/web"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/webservices"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/path"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/security-realms"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/threads"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/resource-adapter"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/xadatasource"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/remoting"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/jdbc-driver"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/logging"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/naming"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/rbac"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/transactions"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/rules/subsystem"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/cookcc-0.3.3.jar"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/jboss-dmr-1.1.1.Final.jar"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/jcliff"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/jcliff-2.12.6.jar"
%attr(755,root,root)  "%{_datadir}/%{name}-%{version}/junit-4.4.jar"
%attr(755,root,root)  "%{_datadir}/jcliff"
  "{_datadir}/jcliff"
