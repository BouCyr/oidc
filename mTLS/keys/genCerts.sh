#!/bin/bash
LOCALNAME=localhost
PASS=aaaaaa

function main
{
    gen
    list
}

function gen
{
mkdir -p keys
rm -f authclient.cert authclient.jks server.cert server.jks servertrust.jks clienttrust.jks unauthclient.jks

# create the keypairs for authclient, unauthclient and for server.
keytool -genkey -alias server -keyalg RSA -keystore server.jks -validity 365 -dname "cn=$LOCALNAME, ou=Auth, o=Auth, c=CA" -storepass $PASS -keypass $PASS
keytool -genkey -alias authclient -keyalg RSA -keystore authclient.jks -validity 365 -dname "cn=$LOCALNAME, ou=Auth, o=Auth, c=CA" -storepass $PASS -keypass $PASS
keytool -genkey -alias unauthclient -keyalg RSA -keystore unauthclient.jks -validity 365 -dname "cn=$LOCALNAME, ou=Auth, o=Auth, c=CA" -storepass $PASS -keypass $PASS

keytool -export -file server.cert -keystore server.jks -storepass $PASS -alias server
keytool -export -file authclient.cert -keystore authclient.jks -storepass $PASS -alias authclient

# Create a bare client truststore with no keypair
echo yes | keytool -import -file server.cert -alias server -keystore clienttrust.jks -storepass $PASS

# Create a truststore for the server containing ONLY authclient
echo yes | keytool -import -file authclient.cert -alias authclient -keystore servertrust.jks -storepass $PASS

# Add the server's cert to the client's keystores
#echo yes | keytool -import -file server.cert -alias server -keystore authclient.jks -storepass $PASS
#echo yes | keytool -import -file server.cert -alias server -keystore unauthclient.jks -storepass $PASS
}

function list {
for x in *.jks; do
    SER=$(keytool -list -v  -keystore $x -storepass aaaaaa  | grep Serial)
    echo $x $SER
done
}
main