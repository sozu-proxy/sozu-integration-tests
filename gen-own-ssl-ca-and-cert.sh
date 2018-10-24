#!/bin/sh
# Create Your Own SSL Certificate Authority for Local HTTPS Development
# $1 = hostname
# $2 = output directory (src/test/resources/certs by default)

set -e
#set -x

OUT_PATH=${2:-src/test/resources/certs}    

echo $#

if [ "$#" -ne 1 -o "$#" -ne 2]; then
    echo "Illegal number of parameters"
    echo "Usage: DOMAIN [OUT_PATH]"
    exit 1
fi

# Generate a passphrase
openssl rand -base64 48 > passphrase.txt

# Create the Certificate Authority pem and key
openssl genrsa -des3 -passout file:passphrase.txt -out $OUT_PATH/CA.key 2048
openssl req -x509 -passin file:passphrase.txt -new -nodes -key $OUT_PATH/CA.key -sha256 -days 7300 -out $OUT_PATH/CA.pem \
        -subj "/C=FR/O=clevercloud/OU=sozu/CN=ca.sozu.com"


# Create CA-Signed certificates for the test
openssl genrsa -passout file:passphrase.txt -out $OUT_PATH/$1.key 2048

openssl req -passin file:passphrase.txt -new -key $OUT_PATH/$1.key -out $OUT_PATH/$1.csr \
        -subj "/C=FR/O=devcompany/OU=dev/CN=$1"

openssl x509 -req -passin file:passphrase.txt -in $OUT_PATH/$1.csr -CA $OUT_PATH/CA.pem -CAkey $OUT_PATH/CA.key -CAcreateserial -out $OUT_PATH/$1.crt -days 7300 -sha256

# == Clean ==
rm -f passphrase.txt
