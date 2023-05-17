#!/bin/bash
. vars.sh
rm -r ../html/plom
rm -r ../html/templates
cd ../../plom-core
${MVN} clean
