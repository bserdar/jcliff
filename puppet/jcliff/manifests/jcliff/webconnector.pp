# Defines a web connector
# Define the web connectors to be used in the node in the client
# module, something like this:
#
#  jcliff::jcliff::thread_pool { 'httpexecutor' :
#    pool_type         => 'blocking-bounded-queue-thread-pool',
#    max_thread_count  => $::variables::http_thread_max,
#    queue_length      => $::variables::http_thread_queue,
#    keepalive_time    => $::variables::http_thread_keepalive_seconds,
#  }
#
#  jcliff::jcliff::webconnector { 'http' :
#    executor        => 'httpexecutor',
#    max_connections => $::variables::http_max_connections,
#    proxy_name      => $::proxy,
#    proxy_port      => '80',
#    pattern         => "\%h \%l \%u \%t '\%r' \%s \%b '\%{Referer}i \%{User-Agent}i' \%S \%T",
#    rotate          => 'true',
#    prefix          => 'access_log.',
#    resolve_hosts   => 'false',
#    access_log      => 'access_log',
#  }
#
# HTTPS:
#
#
#  if $::variables::enable_ssl {
#      jcliff::jcliff::thread_pool { 'https_executor' :
#        pool_type         => 'blocking-bounded-queue-thread-pool',
#        max_thread_count  => $::variables::http_thread_max,
#        queue_length      => $::variables::http_thread_queue,
#        keepalive_time    => $::variables::http_thread_keepalive_seconds,
#      }
#
#      jcliff::jcliff::webconnector { 'https' :
#        executor                => 'https_executor',
#        max_connections         => $::variables::http_max_connections,
#        proxy_name              => $::proxy,
#        proxy_port              => '443',
#        socket_binding          => 'https',
#        scheme                  => 'https',
#        protocol                => 'HTTP/1.1',
#        ssl                     => 'ssl',
#        key_alias               => $::variables::ssl_keystore_alias,
#        ca_certificate_file     => "$::variables::jboss_keystore_location/eap6trust.keystore",
#        certificate_key_file    => "$::variables::jboss_keystore_location/eap6.keystore",
#        pattern                 => "\%h \%l \%u \%t '\%r' \%s \%b '\%{Referer}i \%{User-Agent}i' \%S \%T",
#        rotate                  => true,
#        prefix                  => 'access_log.',
#        resolve_hosts           => false,
#        access_log              => 'access_log',
#        secure                  => true,
#        password                => $::variables::ks_password,
#      }
#  }
define jcliff::jcliff::webconnector (
  $enable_lookups='',
  $enabled='',
  $executor='',
  $max_connections='',
  $max_post_size='',
  $max_save_post_size='',
  $protocol='',
  $proxy_name='',
  $proxy_port='',
  $redirect_port='',
  $scheme='',
  $secure='',
  $socket_binding='',
  $ssl='',
  $virtual_server='',
  $access_log='',
  $pattern='',
  $rotate='',
  $prefix='',
  $resolve_hosts='',
  $password='',
  $certificate_key_file='',
  $key_alias='',
  $ca_certificate_file='',
  $ca_certificate_password=''
) {
  jcliff::jcliff::configfile { "web-connector-${name}.conf":
    content => template('jcliff/web-connector.conf.erb'),
  }
}
