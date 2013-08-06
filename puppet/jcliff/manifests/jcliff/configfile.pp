# Utility to lay down generic configuration files, and
# notify jcliff. Use only the name of the file, not full path,
# directory is added.
define jcliff::jcliff::configfile (
  $mode='0644',
  $owner='root',
  $group='root',
  $content = ''
  ) {
  file { "${jcliff::eap6_config_dir}/${name}":
    mode    => $mode,
    owner   => $owner,
    group   => $group,
    content => $content,
    notify  => Exec['configure-eap6'],
  }
}
