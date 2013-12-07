class jcliff::jcliff {

  $eap6_home = '/usr/share/jbossas'
  $eap6_config_dir = '/etc/jcliff'
  $configuration = $::configuration ? {
    ''      => 'standalone',
    default => $::configuration,
  }
  $management_host = $::management_host ? {
    ''      => 'localhost',
    default => $::management_host,
  }
  $management_port = $::management_port ? {
    ''      => '9999',
    default => $::management_port,
  }
  $log_dir = '/var/log'

  #Create directory for jcliff configurations to be put
  file { $eap6_config_dir:
    ensure  => 'directory',
    owner   => 'jboss',
    group   => 'jboss',
    mode    => '0755',
  }

  exec { 'configure-eap6' :
    command   => "/usr/bin/jcliff --cli=${eap6_home}/bin/jboss-cli.sh -v --controller=${management_host}:${management_port} --output=${log_dir}/jcliff.log ${eap6_config_dir}/*",
    onlyif    => "[ $(/usr/bin/find ${eap6_config_dir} -type f | /usr/bin/wc -l) -gt 0 ]",
    logoutput => true,
    timeout   => 0,
    require   => [ Package['jcliff'], Package["jbossas-${config}"], Service['jbossas'] ],
    notify    => Exec['reload-check']
  }

  exec { 'reload-check' :
    onlyif  => "${eap6_home}/bin/jboss-cli.sh --controller=${management_host}:${management_port} --connect \":read-attribute(name=server-state)\" | grep reload-required",
    command => 'service jbossas restart',
    logoutput     => true
  }
}
