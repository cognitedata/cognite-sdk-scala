#!/bin/sh

# secrets were encrypted with
#   $ gpg --symmetric --cipher-algo AES256 <file_to_encrypt>

# --batch to prevent interactive command --yes to assume "yes" for questions
gpg --quiet --batch --yes --decrypt --passphrase="$SECRETS_KEY" \
--output $2 $1
