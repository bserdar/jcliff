# Defines a web connector
define jcliff::jcliff::thread_pool (
  $pool_type='',
  $core_thread_count='',
  $handoff_executor='',
  $keepalive_time='',
  $max_thread_count='',
  $queue_length='',
  $thread_factory='',
) {
  jcliff::jcliff::configfile { "thread-pool-${name}.conf":
    content => template('jcliff/thread-pool.conf.erb'),
  }
}
