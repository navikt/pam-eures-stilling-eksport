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

## Admin functions

### Get statistics
Get a count of Job Vacancies in the database grouped by their status.

`curl -k https://$BASE_URL/internal/admin/statistikk`

Get a count of Job Vacancies updated after the given timestamp grouped by their status.
Timestamp is use the following format: `YYYY-MM-DDTHH:mm:SS`, e.g. `2019-12-01T12:00:00`

`curl -k https://$BASE_URL/internal/admin/statistikk/<timestamp>`


