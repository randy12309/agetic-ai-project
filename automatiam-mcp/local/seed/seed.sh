#!/bin/sh
set -eu

index_document() {
    curl --fail --silent --show-error --output /dev/null \
        --request PUT --header 'Content-Type: application/json' --data-binary @- \
        "${OPENSEARCH_URL%/}/$1/_doc/$2?refresh=wait_for"
}

index_document identity_index EF11111 <<'JSON'
{"id":"EF11111","name":"EF11111","displayName":"Fixture Owner","display_name":"Fixture Owner"}
JSON

index_document application_index APM001 <<'JSON'
{
  "name":"Claims Workbench","elApmName":"Claims Workbench","el_apm_name":"Claims Workbench",
  "elApmNumber":"APM001","el_apm_number":"APM001","owner":"EF11111"
}
JSON

index_document managed_attributes_index 0abcfdb58d231b42818d28910e5d78b3 <<'JSON'
{
  "value":"claims.adjuster","displayName":"claims.adjuster","display_name":"claims.adjuster",
  "description":"Adjust synthetic claims","applicationName":"Claims Workbench","application_name":"Claims Workbench",
  "elApmNumber":"APM001","el_apm_number":"APM001","owner":"EF11111",
  "elevatedAccess":true,"elevated_access":true,"phi":true,"pfi":true
}
JSON

index_document managed_attributes_index 1bcdfdb58d231b42818d28910e5d78c4 <<'JSON'
{
  "value":"claims.reader","displayName":"claims.reader","display_name":"claims.reader",
  "description":"Read synthetic claims","applicationName":"Claims Workbench","application_name":"Claims Workbench",
  "owner":"EF11111","elevatedAccess":false,"elevated_access":false,"phi":false,"pfi":false
}
JSON

echo 'seed complete'
