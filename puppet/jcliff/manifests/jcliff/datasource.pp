define jcliff::jcliff::datasource(
  $template_name,
  $jndi_name='',
  $url='',
  $driver_name='',
  $username='',
  $password='',
  $transaction_isolation='',
  $min_conn='',
  $max_conn='',
  $new_connection_sql='',
  $check_valid_connection_sql='',
  $valid_connection_checker_class_name='',
  $exception_sorter_class_name='',
  $config_file_name='',
  $validate_on_match='',
  $background_validation='',
  $background_validation_millis='') {

  jcliff::jcliff::configfile { $config_file_name:
    content => template($template_name),
  }
}
