# Registers a JDBC driver with EAP6
define jcliff::jcliff::jdbcdriver(
  $driver_name = '',
  $module_name = '',
  $xa_datasource_class_name= ''
  ) {
  jcliff::jcliff::configfile { "jdbc-driver-${driver_name}.conf":
    content => template('jcliff/jdbc-driver.conf.erb'),
  }
}
