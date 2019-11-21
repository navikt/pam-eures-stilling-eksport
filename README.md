# pam-eures-stilling-eksport

Implementation of the Eures input API, used by EU and EURES to fetch Job Vacancies in Norway.

## Examples

curl-examples of API usage. You can substitute *BASE_URL* with 
*pam-eures-stilling-eksport.nais.oera-q.local* to query the test environment.

### Get all job vacancies
`curl -k https://$BASE_URL/input/api/jv/v0.1/getAll`

### Get all job vacancies changed after timestamp

`curl -k https://$BASE_URL/input/api/jv/v0.1/getChanges/<timestamp>`

The timestamp is milliseconds since epoch in the UTC timezone, 
i.e. `System.currentTimeMillis()` with Java, or this perl one-liner from the unix shell:
`perl -e 'use Time::HiRes qw(gettimeofday); print gettimeofday'`

### Get job vacancy details
This will return the job vacancy details for JVs with UUID uuid1, uuid2, etc 

```
curl -k -XPOST -H 'Content-type: application/json' -d '["<uuid1>", "<uuid2>", ...]' \
 https://$BASE_URL/input/api/jv/v0.1/getDetails
 ```


### Reread a job vacancy from pam-ad
If a job vacancy for some reason needs to be fetched again from pam-ad:

`curl -k https://$BASE_URL/internal/rekjor/<uuid>`


