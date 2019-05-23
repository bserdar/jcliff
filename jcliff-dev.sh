#!/bin/bash
#    Copyright 2013 Red Hat, Inc. and/or its affiliates.
#
#    This file is part of jcliff.
#
#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program.  If not, see <http://www.gnu.org/licenses/>.

#
# A simple script to run jcliff from the development environement
set -euo pipefail

export JCLIFF_HOME=${JCLIFF_HOME:-$(pwd)}
export CLASSPATH=$(echo "${JCLIFF_HOME}"/target/jcliff-*.jar)
export JCLIFF_RULES_DIR=${JCLIFF_RULES_DIR:-'./src/main/resources'}
export JCLIFF_DEPS_DIR=${JCLIFF_DEPS_DIR:-'./target/dependency/'}

./src/main/bash/jcliff ${@}
