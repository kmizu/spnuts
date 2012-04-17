#!/bin/bash

sysname=$(uname -s)

case $sysname in
CYGWIN*)
  whence_cygwin()
  {
    local path=

    for cmd
    do
       path=$(builtin type -path $cmd)
       if [ "$path" ] ; then
	  cygpath -m $path
       else
	  case "$cmd" in
	  /*) if [ -x "$cmd" ]; then
	         cygpath -m "$cmd"
	      fi
	      ;;
	  *) case "$(builtin type -type $cmd)" in
	     "") ;;
	     *) cygpath -m "$cmd"
		 ;;
	     esac
	     ;;
	  esac
	fi
    done
    return 0
  }
  PNUTS_HOME=$(dirname $(whence_cygwin $0))/..
  ;;
  *)
  PNUTS_HOME=$(dirname $(builtin type -path $0))/..
  ;;
esac

let i=0
let j=0

if [ "x${HTTP_PROXY_HOST}" != "x" ] && [ "x${HTTP_PROXY_PORT}" != "x" ]; then
   flags[$i]="-Dhttp.proxyHost=${HTTP_PROXY_HOST}"
   let i++
   flags[$i]="-Dhttp.proxyPort=${HTTP_PROXY_PORT}"
   let i++
fi

for a in "$@"
do
    case "$a" in
    -J*)
	flags[$i]=${a#-J}
	let i++
	;;
    -vd|-d|-v)
	_g=true
	export JAVA_COMPILER=NONE
	args[$j]=$a
	let j++
	;;
    *)
	args[$j]=$a
	let j++
	;;
    esac
done

case $sysname in
    CYGWIN*)
      PNUTS_HOME=$(cygpath -m ${PNUTS_HOME})
      PATHSEP=";"
      ;;
    Darwin*)
      export DYN_LIBRARY_PATH=${DYN_LIBRARY_PATH}:${PNUTS_HOME}/lib
      PATHSEP=":"
      ;;
    *)
      export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:${PNUTS_HOME}/lib
      PATHSEP=":"
      ;;
esac
if [ "${PNUTS_JDK11_COMPATIBLE}" = "true" ]; then
  CLASSPATH=${PNUTS_HOME}/lib/pnuts.jar${PATHSEP}${CLASSPATH}
  export CLASSPATH
else
  flags[$i]="-Xbootclasspath/a:${PNUTS_HOME}/lib/pnuts.jar"
fi

MODULE_DIR=${PNUTS_HOME}/modules

if [ "x${PNUTS_MODULE}" = "x" ]; then
  module=pnuts.tools
else
  module=${PNUTS_MODULE}
fi

if [ "x${PNUTS_JAVA_COMMAND}" = "x" ]; then
   java="java"
else
   java=${PNUTS_JAVA_COMMAND}
fi

exec ${java} "${flags[@]}" "-Dpnuts.home=${PNUTS_HOME}" "-Djava.endorsed.dirs=${MODULE_DIR}" pnuts.tools.Main -m "${module}" "${args[@]}"
