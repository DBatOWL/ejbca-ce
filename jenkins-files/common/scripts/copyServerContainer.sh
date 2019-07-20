#!/bin/sh

# copyDatabaseContainer.sh SRC_PATH TARGET_PATH DB_CONTAINER JDK DB_FAMILY DB_VERSION SERVER_FAMILY SERVER_VERSION
#                          [1]      [2]         [3]          [4] [5]       [6]        [7]           [8]
echo "SRC_PATH: $1"
echo "TARGET_PATH: $2"
echo "DB_HOST: $3"
echo "JDK: $4"
echo "DB_FAMILY: $5"
echo "DB_VERSION: $6"
echo "SERVER_FAMILY: $7"
echo "SERVER_VERSION: $8"

DOCKERFILE_PATH=""
DOCKERFILE_RUN_PATH=""
DOCKERFILE_STANDALONE1_PATH=""
DOCKERFILE_STANDALONE2_PATH=""
DOCKERFILE_STANDALONE_DATASOURCE_JNDI_NAME="java:/EjbcaDS"
DOCKERFILE_STANDALONE_DATASOURCE_CONNECTION_URL=""
DOCKERFILE_STANDALONE_DATASOURCE_DRIVER=""
DOCKERFILE_STANDALONE_DATASOURCE_DRIVER_CLASS=""
DOCKERFILE_STANDALONE_DATASOURCE_USERNAME=""
DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD=""
DOCKERFILE_STANDALONE_DATASOURCE_VALID_CONNECTION_SQL=""
DOCKERFILE_STANDALONE_DRIVER=""

echo "Looking for application server container..."
if [ $7 = "wildfly" ]
then
    if [ -f "$1/$7/$8/Dockerfile" ]
    then
        echo "Found WildFly container with version $8"
        DOCKERFILE_PATH="$1/$7/$8/Dockerfile"
        DOCKERFILE_RUN_PATH="$1/$7/$8/run.sh"
        DOCKERFILE_STANDALONE1_PATH="$1/$7/standalone1.xml"
        DOCKERFILE_STANDALONE2_PATH="$1/$7/standalone2.xml"
    else
        echo "Error: Cannot find the WildFly container with version $6"
        exit 1
    fi
else
  echo "Error: Cannot map the application server family"
  exit 1
fi

echo "Configuring database in standalone.xml files..."
if [ $5 = "db2" ]
then
    echo "Using DB2 pattern..."
    DOCKERFILE_STANDALONE_DATASOURCE_CONNECTION_URL="jdbc:db2://$3:50000/ejbca"
    DOCKERFILE_STANDALONE_DATASOURCE_DRIVER="db2"
    DOCKERFILE_STANDALONE_DATASOURCE_DRIVER_CLASS=""
    DOCKERFILE_STANDALONE_DATASOURCE_USERNAME="db2inst1"
    DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD="db2inst1"
    DOCKERFILE_STANDALONE_DATASOURCE_VALID_CONNECTION_SQL="select 1 from sysibm.sysdummy1"
    DOCKERFILE_STANDALONE_DRIVER="<driver name=\"db2\" module=\"com.ibm.db2\"><xa-datasource-class>com.ibm.db2.jcc.DB2XADataSource</xa-datasource-class></driver>"
elif [ $5 = "mariadb" ]
then
    echo "Using MariaDB pattern..."
    DOCKERFILE_STANDALONE_DATASOURCE_CONNECTION_URL="jdbc:mysql://$3:3306/ejbca"
    DOCKERFILE_STANDALONE_DATASOURCE_DRIVER="dbdriver.jar"
    DOCKERFILE_STANDALONE_DATASOURCE_DRIVER_CLASS="<driver-class>org.mariadb.jdbc.Driver</driver-class>"
    DOCKERFILE_STANDALONE_DATASOURCE_USERNAME="ejbca"
    DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD="ejbca"
    DOCKERFILE_STANDALONE_DATASOURCE_VALID_CONNECTION_SQL="select 1;"
    DOCKERFILE_STANDALONE_DRIVER=""
elif [ $5 = "mssql" ]
then
    echo "Using MS SQL pattern..."
    echo "Error: Not implemented"
    exit 1
elif [ $5 = "oracle" ]
then
    echo "Using Oracle DB pattern..."
    echo "Error: Not implemented"
    exit 1
else
  echo "Error: Cannot map the database family"
  exit 1
fi

# Copy resources
cp $DOCKERFILE_PATH $2/
cp $DOCKERFILE_RUN_PATH $2/
sed -e "s#DOCKERFILE_STANDALONE_DATASOURCE_JNDI_NAME#$DOCKERFILE_STANDALONE_DATASOURCE_JNDI_NAME#" \
    -e "s#DOCKERFILE_STANDALONE_DATASOURCE_CONNECTION_URL#$DOCKERFILE_STANDALONE_DATASOURCE_CONNECTION_URL#" \
    -e "s#DOCKERFILE_STANDALONE_DATASOURCE_DRIVER#$DOCKERFILE_STANDALONE_DATASOURCE_DRIVER#" \
    -e "s#DOCKERFILE_STANDALONE_DATASOURCE_DRV_CLASS#$DOCKERFILE_STANDALONE_DATASOURCE_DRIVER_CLASS#" \
    -e "s#DOCKERFILE_STANDALONE_DATASOURCE_USERNAME#$DOCKERFILE_STANDALONE_DATASOURCE_USERNAME#" \
    -e "s#DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD#$DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD#" \
    -e "s#DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD#$DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD#" \
    -e "s#DOCKERFILE_STANDALONE_DATASOURCE_VALID_CONNECTION_SQL#$DOCKERFILE_STANDALONE_DATASOURCE_VALID_CONNECTION_SQL#" \
    $DOCKERFILE_STANDALONE1_PATH > $2/standalone1.xml

sed -e "s#DOCKERFILE_STANDALONE_DATASOURCE_JNDI_NAME#$DOCKERFILE_STANDALONE_DATASOURCE_JNDI_NAME#" \
    -e "s#DOCKERFILE_STANDALONE_DATASOURCE_CONNECTION_URL#$DOCKERFILE_STANDALONE_DATASOURCE_CONNECTION_URL#" \
    -e "s#DOCKERFILE_STANDALONE_DATASOURCE_DRIVER#$DOCKERFILE_STANDALONE_DATASOURCE_DRIVER#" \
    -e "s#DOCKERFILE_STANDALONE_DATASOURCE_DRV_CLASS#$DOCKERFILE_STANDALONE_DATASOURCE_DRIVER_CLASS#" \
    -e "s#DOCKERFILE_STANDALONE_DATASOURCE_USERNAME#$DOCKERFILE_STANDALONE_DATASOURCE_USERNAME#" \
    -e "s#DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD#$DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD#" \
    -e "s#DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD#$DOCKERFILE_STANDALONE_DATASOURCE_PASSWORD#" \
    -e "s#DOCKERFILE_STANDALONE_DATASOURCE_VALID_CONNECTION_SQL#$DOCKERFILE_STANDALONE_DATASOURCE_VALID_CONNECTION_SQL#" \
    $DOCKERFILE_STANDALONE2_PATH > $2/standalone2.xml