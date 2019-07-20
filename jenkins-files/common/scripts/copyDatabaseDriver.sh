#!/bin/sh

# copyDatabaseDriver.sh SRC_PATH TARGET_PATH DB_CONTAINER JDK DB_FAMILY DB_VERSION SERVER_FAMILY SERVER_VERSION
#                       [1]      [2]         [3]          [4] [5]       [6]        [7]           [8]
echo "SRC_PATH: $1"
echo "TARGET_PATH: $2"
echo "DB_HOST: $3"
echo "JDK: $4"
echo "DB_FAMILY: $5"
echo "DB_VERSION: $6"
echo "SERVER_FAMILY: $7"
echo "SERVER_VERSION: $8"

MODULE_XML=""
MODULE_JAR=""
MODULE_XML_COPY_PATH=""

if [ $5 = "db2" ]
then
    echo "Using DB2 library..."
    MODULE_XML="module_$4.xml"
    MODULE_JAR="db2jcc4.jar"
elif [ $5 = "mariadb" ]
then
    echo "Using MariaDB library..."
    MODULE_JAR="mariadb-java-client.jar"
elif [ $5 = "mssql" ]
then
    echo "Using MS SQL library..."
    echo "Error: Not implemented"
    exit 1
elif [ $5 = "oracle" ]
then
    echo "Using Oracle library..."
    echo "Error: Not implemented"
    exit 1
else
  echo "Error: Cannot map the database family"
  exit 1
fi

if [ MODULE_XML != "" ]
then
    cp $1\lib\$5\$MODULE_XML $2\module.xml
fi

if [ MODULE_JAR != "" ]
then
    cp $1\lib\$5\$MODULE_JAR $2\dbdriver.jar
fi

if [ MODULE_XML_COPY_PATH != "" ]
then
    export MODULE_XML_COPY_PATH=$MODULE_XML_COPY_PATH
fi
