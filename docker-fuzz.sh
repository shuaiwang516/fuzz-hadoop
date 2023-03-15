# first run 'docker build -t 'hadoop-build' -f Dockerfile .' from the docker dir
# Usage: bash docker.sh <testModule> <testClass> <testMethod> <duration>
# assumes that constraintFile is named constraint and regex file name
# root dir is where the two files are stored

# Note that fuzz-hadoop is stored in /home/ctestfuzz/ in the docker
containerName=fuzzingcon
testModule=$1
testClass=$2
testMethod=$3
constraintFile=constraint
regexFile=regex
duration=$4

docker run --name ${containerName} -u ctestfuzz -w "/home/ctestfuzz/fuzz-hadoop/${testModule}" -d -i -t "hadoop-build" bash
docker exec -u ctestfuzz ${containerName} mvn confuzz:fuzz -Dmeringue.testClass=${testClass} -Dmeringue.testMethod=${testMethod} -DconstraintFile="${constraintFile}" -DregexFile="${regexFile}" -Dmeringue.duration=${duration}
docker exec -u ctestfuzz ${containerName} mvn confuzz:analyze -Dmeringue.testClass=${testClass} -Dmeringue.testMethod=${testMethod} -DconstraintFile="${constraintFile}" -DregexFile="${regexFile}" -Dmeringue.duration=${duration}
docker exec -u ctestfuzz ${containerName} mvn confuzz:debug -Dmeringue.testClass=${testClass} -Dmeringue.testMethod=${testMethod} -DconstraintFile="${constraintFile}" -DregexFile="${regexFile}"
mkdir -p result/$testClass
docker cp -r "${containerName}:/home/ctestfuzz/fuzz-hadoop/$testModule/target/meringue/${testClass}/${testMethod}" "result/$testClass/"
docker stop ${containerName}
docker rm ${containerName}
