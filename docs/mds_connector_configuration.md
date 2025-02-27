# MDS Connector Configuration

MDS Connector is configured in the `docker-compose.yml` using **environments** variables.

Here is the list of the considered configuration properties with example values: 

```properties
edc.participant.id = my_participant_id
edc.hostname = my_edc_hostname

web.http.path = /api
web.http.port = 11001
web.http.control.path = /control
web.http.control.port = 11002
web.http.management.path = /management
web.http.management.port = 11003
web.http.protocol.path = /protocol
web.http.protocol.port = 11004
web.http.public.path = /public
web.http.public.port = 11005
web.http.version.path = /version
web.http.version.port = 11006

edc.transfer.proxy.token.verifier.publickey.alias = public-key
edc.transfer.proxy.token.signer.privatekey.alias = private-key

edc.vault.hashicorp.url = http://hashicorp_vault_url
edc.vault.hashicorp.token = root
edc.vault.hashicorp.folder = my_mds_edc
```
