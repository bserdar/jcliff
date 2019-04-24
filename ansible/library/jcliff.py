#!/usr/bin/python

from ansible.module_utils.basic import AnsibleModule
from jinja2 import Template, Environment, FileSystemLoader
import json
import subprocess
import os
import tempfile
import shutil

def formatOutput(output):
  result = ""
  i = 0
  for line in output.split(os.linesep):
      i += 1
      result += { i : line }
  return result

def formatOutput2(output):
  result = ""
  if type(output) is not list:
    for line in output.split(os.linesep):
      result += { line : str("") }
  else:
    for line in output:
      if type(line) is dict:
        for item in line:
          result += { str(item): str("") }
      else:
        result += { str(item): str("") }
  return result

def formatOutput3(output):
  return output.split(os.linesep)

def renderFromTemplate(rulesdir, template_name, output_file,values):
  # FIXME: can't load that everytime
  file_loader = FileSystemLoader('rules_templates')
  env = Environment(loader=file_loader)
  template = env.get_template(template_name)
  with open( os.path.join(rulesdir,output_file), 'w') as template_file:
    template_file.write(template.render(values=values))

def generateRuleFromTemplate(data,rulesdir):
  if data['subsystems'] is not None:
    subsystems = data['subsystems']
    for subsys in subsystems:
      if subsys['datasources'] is not None:
        datasources = subsys["datasources"]
        for ds in datasources:
          renderFromTemplate(rulesdir, 'datasource.j2', "datasource-" + ds['name'] + ".yml", ds)
      if subsys['system_props'] is not None:
        renderFromTemplate(rulesdir,'system-properties.j2', 'system-properties.yml', subsys['system_props'])
      if subsys['deployments'] is not None:
        renderFromTemplate(rulesdir,'deployments.j2', 'deployments.yml', subsys['deployments'])

def listRuleFiles(rulesdir):
  rules_filename = os.listdir(rulesdir)
  rule_files = []
  for filename in rules_filename:
      rule_files.append(rulesdir + "/" + filename)
  return rule_files

def executeRulesWithJCliff(data,rulesdir):
  jcliff_command_line = [data["jcliff"], "--cli=" + data['wfly_home'] + "/bin/jboss-cli.sh", "--ruledir=" + data['rules_dir'], "--controller=" + data['management_host'] + ":" + data['management_port'], "-v"]
  jcliff_command_line.extend(listRuleFiles(rulesdir))
  output = None
  try:
    output = subprocess.check_output(jcliff_command_line, shell=False, env=os.environ)
  except subprocess.CalledProcessError as jcliffexc:
    error = str(jcliffexc.output, 'utf-8')
    if (jcliffexc.returncode != 2) and (jcliffexc.returncode != 0):
        return { "failed": { "status" : jcliffexc.returncode, "output":  error, "rulesdir": rulesdir , "jcliff_cli": jcliff_command_line  } }
    else:
        return {"present:": formatOutput2(error) }
  except Exception as e:
     print(e)
  return {"present" : output, "ruledir" : rulesdir, "jcliff_cli": jcliff_command_line }

def copyRulesToRulesdir(rulefiles, rulesdir):
  try:
    for rulefile in rulefiles.split(" "):
      shutil.copy2(rulefile, rulesdir)
  except shutil.Error as e:
    print('Directory not copied. Error: %s' % e)
  except OSError as e:
    print('Directory not copied. Error: %s' % e)

def jcliff_present(data):
  has_changed = False
  rulesdir = tempfile.mkdtemp()
  generateRuleFromTemplate(data, rulesdir)
  if data['rule_file'] is not None:
    copyRulesToRulesdir(data['rule_file'], rulesdir)
  print("Executing JCliff:")
  meta = executeRulesWithJCliff(data, rulesdir)
  has_changed = True
  #shutil.rmtree(rulesdir)
  return (has_changed, meta)

def jcliff_absent(data=None):
   has_changed = False
   #subprocess.run(["ls", "-l"])
   meta = {"absent": "not yet implemented"}

def main():
    default_jcliff_home = "/usr/share/jcliff-2.11.12"
    fields = dict(
         jcliff_home=dict(type='str', default=default_jcliff_home),
         jcliff=dict(default='/usr/bin/jcliff', type='str'),
         rules_dir=dict(type='str', default=default_jcliff_home + "/rules"),
         wfly_home=dict(required=True, type='str'),
         management_host=dict(default='localhost', type='str'),
         management_port=dict(default='9990', type='str'),
         rule_file=dict(required=False, type='str'),
         subsystems=dict(type='list', required=False, elements='dict',
            options=dict(
                datasources=dict(type='list', required=False, elements='dict', options=dict(
                    name=dict(type='str', required=True),
                    jndi_name=dict(type='str', required=True),
                    use_java_context=dict(type='str', default='true'),
                    connection_url=dict(type='str', required=True),
                    driver_name=dict(type='str', required=True),
                    enabled=dict(type='str', default='true'),
                    password=dict(type='str', required=False),
                    user_name=dict(type='str', required=False),
		    max_pool_size=dict(type='str', default='undefined'),
            	    min_pool_size=dict(type='str', default='undefined'),
	            idle_timeout_minutes=dict(type='str', default='undefined'),
                    query_timeout=dict(type='str', default='undefined'),
                    check_valid_connection_sql=dict(type='str', default='undefined'),
                    validate_on_match=dict(type='str', default='undefined')
                    )
                ),
                system_props=dict(type='list', required=False, elements='dict', options=dict(
                    name=dict(type='str', required=False),
                    value=dict(type='str', required=False)
                    )
                ),
                deployments=dict(type='list', required=False, elements='dict', options=dict(
                    artifact_id=dict(type='str', required=True),
                    name=dict(type='str', required=False),
                    path=dict(type='str', required=True)
                    )
                )
            )
         ),
         state=dict(default="present", choices=['present', 'absent'], type='str')
    )

    module = AnsibleModule(argument_spec=fields)
    if os.environ.get("JCLIFF_HOME"):
        fields["jcliff_home"] = os.environ.get("JCLIFF_HOME")
    # TODO validation that jcliff_home exits + dir

    choice_map = {
        "present": jcliff_present,
        "absent": jcliff_absent,
    }
    has_changed, result = choice_map.get(module.params['state'])(data=module.params)
    module.exit_json(changed=has_changed, meta=result)
if __name__ == '__main__':
    main()
