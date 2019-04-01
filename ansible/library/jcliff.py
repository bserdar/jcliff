#!/usr/bin/python

from ansible.module_utils.basic import AnsibleModule
from jinja2 import Template, Environment, FileSystemLoader
import subprocess
import os

def formatOutput(output):
  result = ""
  i = 0
  for line in output.split(os.linesep):
      i += 1
      result += { i : line }
  return result

#def generateRuleFromTemplate(data):
#  file_loader = FileSystemLoader('rules_templates')
#  env = Environment(loader=file_loader)
#  name = "Romain"
#  template = env.get_template('datasource.j2')
#  return template.render(name=name)

def jcliff_present(data):
  has_changed = False
#  msg = generateRuleFromTemplate(data)
  try:
    output = subprocess.check_output([data["jcliff"], "--cli=" + data['wfly_home'] + "/bin/jboss-cli.sh", "--ruledir=" + data['rules_dir'], "--controller=" + data['management_host'] + ":" + data['management_port'], "-v", data["rule_file"]], shell=False, env=os.environ)
    meta = {"present" : output }
  except subprocess.CalledProcessError as jcliffexc:
    if jcliffexc.returncode != 2:
          meta = { "failed": { "status" : jcliffexc.returncode, "output": jcliffexc.output } }
    else:
        meta = {"present": jcliffexc.output }
    has_changed = True
  return (has_changed, meta)

def jcliff_absent(data=None):
   has_changed = False
   #subprocess.run(["ls", "-l"])
   meta = {"absent": "not yet implemented"}

def main():
    default_jcliff_home = "/usr/share/jcliff-2.11.12/"
    fields = {
        "jcliff_home": {"default": default_jcliff_home, "type": "str" },
        "jcliff": { "default": "/usr/bin/jcliff", "type": "str" },
        "rules_dir": {"default": default_jcliff_home + "/rules", "type": "str"},
        "wfly_home": {"required": True, "type": "str" },
        "management_host": {"default": 'localhost', "type": "str" },
        "management_port": {"default": '9990', "type": "str" },
        "rule_file": { "required": True, "type": "str" },
        "state": {
            "default": "present",
            "choices": ['present', 'absent'],
            "type": 'str'
        },
    }

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
