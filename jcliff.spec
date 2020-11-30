Name: 		jcliff
Version: 	2.12.6
Release: 	1%{?dist}
Summary: 	JBoss configuration client front-end
License: 	2013, Red Hat, Inc. and/or its affiliates.
Group: 		Applications/File

URL:           	https://github.com/bserdar/jcliff
Source0:       	%{url}/archive/v%{version}.tar.gz

BuildRequires:  maven-local
BuildRequires:  mvn(com.google.code.cookcc:cookcc)
BuildRequires:  mvn(org.jboss:jboss-dmr)
BuildRequires:  mvn(junit:junit)
BuildRequires:  mvn(org.codehaus.mojo:exec-maven-plugin)
BuildRequires:  mvn(org.codehaus.mojo:rpm-maven-plugin)

BuildArch:      noarch


%description
Jcliff configures a running instance of EAP6/JBoss7 using modular configuration files.

%prep
%setup -q -n %{name}-%{name}-%{version}

sed -i 's/\r//' LICENSE
cp -p LICENSE

%mvn_file : %{name}

%build
%mvn_build

%install
%mvn_install


%files
%defattr(-,root,root,755)
%dir %attr(755,root,root) "/usr/share/jcliff-2.12.6"
  "/usr/share/jcliff-2.12.6/cookcc-0.3.3.jar"
  "/usr/share/jcliff-2.12.6/jboss-dmr-1.1.1.Final.jar"
  "/usr/share/jcliff-2.12.6/jcliff-2.12.6.jar"
  "/usr/share/jcliff-2.12.6/junit-4.4.jar"
%dir %attr(755,root,root) "/usr/share/jcliff-2.12.6/rules"
 "/usr/share/jcliff-2.12.6/rules"
%attr(755,root,root)  "/usr/share/jcliff-2.12.6/jcliff"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/mail"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/rules"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/security"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/scanner"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/osgi"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/jgroups"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/ee"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/ejb3"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/jmx"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/extension"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/standard-sockets"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/messaging"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/infinispan"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/teiid"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/system-properties"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/datasource"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/interface"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/undertow"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/web"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/webservices"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/path"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/security-realms"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/threads"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/resource-adapter"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/xadatasource"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/remoting"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/jdbc-driver"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/logging"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/naming"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/rbac"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/transactions"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/rules/subsystem"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/cookcc-0.3.3.jar"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/jboss-dmr-1.1.1.Final.jar"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/jcliff"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/jcliff-2.12.6.jar"
%attr(755,root,root)  "/usr/share//jcliff-2.12.6/junit-4.4.jar"
%attr(755,root,root)  "/usr/share//jcliff"
  "/usr/bin/jcliff"

%changelog
* Mon Nov 30 2020 Harsha Cherukuri <hcheruku@redhat.com> 2.12.6-1
- initial rpm
