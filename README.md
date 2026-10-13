# OpenCDMP Zenodo deposit service

**repository-deposit-zenodo** is an [OpenCDMP](https://github.com/OpenCDMP/OpenCDMP) deposit service that publishes plans to [Zenodo](https://zenodo.org/) or another [InvenioRDM](https://inveniordm.docs.cern.ch/) instance. It creates a record with the plan's metadata and files, publishes it, and returns the DOI that Zenodo mints, which OpenCDMP stores with the plan.

The service implements the [repository-deposit-base](https://github.com/OpenCDMP/repository-deposit-base) contract. See [Deposit services](https://opencdmp.github.io/optional-services/deposit-services/) in the OpenCDMP documentation.

## Features

- Deposits a plan as a new Zenodo record, or as a new version of a record deposited earlier
- Publishes under the organisation's account or the user's own account, with a personal access token or by signing in to Zenodo (OAuth 2.0)
- Uploads the plan document (PDF), the machine-actionable plan (RDA JSON) and the supporting files (ZIP)
- Lets users review and edit the record's metadata before it is published, with search in Zenodo's vocabularies
- Submits new records to the configured community and to any further communities
- Fills Zenodo fields from the answers of the plan blueprint and asset templates, through [semantics](https://opencdmp.github.io/optional-services/deposit-services/zenodo-semantics/)

## Requirements

- Zenodo or another InvenioRDM instance (REST API v12.0), and a Zenodo account with a personal access token (scopes `deposit:write` and `deposit:actions`). For sign-in per user, a developer application on Zenodo.
- An OpenCDMP 2.0 installation, and its Keycloak realm with the `plugins` client scope
- To build from source: Java 21 and Maven. The service is a Spring Boot 4.0 application built on `repository-deposit-base`; the exact versions are in [`pom.xml`](pom.xml).

## Run with Docker

The image is [`opencdmp/repository-deposit-zenodo`](https://hub.docker.com/r/opencdmp/repository-deposit-zenodo) on Docker Hub. It listens on port `8080`, reads its settings from `/config/app-<PROFILE>.env` and writes its logs to `/logs`:

```bash
docker run -d --name opencdmp-zenodo \
  --network <opencdmp-network> \
  -e PROFILE=production \
  -v /srv/opencdmp/zenodo/config:/config \
  -v /srv/opencdmp/zenodo/logs:/logs \
  opencdmp/repository-deposit-zenodo:<tag>
```

Use the tag that the [changelog](https://opencdmp.github.io/deployment/install/changelog/) lists for your OpenCDMP release. Keep the port on the internal network: the `/actuator` endpoints are served without authentication. For a Docker Compose entry, see [Registering plugin services](https://opencdmp.github.io/deployment/configuration/plugin-services/#step-1-run-the-service).

## Build from source

```bash
mvn clean package
docker build -t repository-deposit-zenodo:local .
```

Maven builds `web/target/repository-deposit-web-1.0.0-SNAPSHOT.jar`, and the `Dockerfile` builds the service from source on its own. The service contacts Keycloak at `IDP_ISSUER_URI` when it starts, and does not start if it cannot reach it. To run the JAR against a local OpenCDMP installation, see [Run and test a plugin locally](https://opencdmp.github.io/developers/plugins/run-locally/).

## Configuration

The service reads its settings from environment variables or from `app-<PROFILE>.env`. The main ones:

| Variable | Description |
| --- | --- |
| `IDP_ISSUER_URI` | Keycloak realm URL, the same value as in the OpenCDMP API service |
| `IDP_CLAIMS_ROLES_PATH` | `plugins.roles` |
| `SECURITY_AUDIENCE` | `plugins` |
| `OPENCDMP_DOMAIN` | Address of the OpenCDMP installation, with a trailing slash |
| `DEPOSIT_ZENODO_URL`, `DEPOSIT_ZENODO_RECORD_URL` | Zenodo API and record addresses; default to zenodo.org |
| `DEPOSIT_ZENODO_ACCESS_TOKEN` | Access token of the installation's Zenodo account |
| `DEPOSIT_ZENODO_CLIENT_ID`, `DEPOSIT_ZENODO_CLIENT_SECRET`, `DEPOSIT_ZENODO_REDIRECT_URL` | Zenodo developer application, for sign-in per user |
| `DEPOSIT_ZENODO_COMMUNITY_ID` | Community that new records are submitted to |
| `DEPOSIT_ZENODO_SUPPORTS_PREVIEW` | Metadata review before publishing (default `true`) |
| `STORAGE_PATH` | Folder of temporary and shared files |

All variables, with defaults and examples, are in [Deposit services: Zenodo](https://opencdmp.github.io/deployment/configuration/backend/deposit/#zenodo); the authentication, logging and storage settings are in [Settings common to all plugin services](https://opencdmp.github.io/deployment/configuration/plugin-services/#settings-common-to-all-plugin-services). Each organisation can override the account and the record defaults in its [organisation settings](https://opencdmp.github.io/optional-services/deposit-services/#organisation-settings).

### Publishing accounts

For each deposit, the service uses the first of these Zenodo tokens that is set:

1. The token the user obtained by signing in to Zenodo (`authInfo.authToken`)
2. The personal access token the user saved in their [profile](https://opencdmp.github.io/using/profile-settings/#repository-accounts) or typed in the publish dialog (`authInfo.authFields`, code `zenodo-access-token`)
3. The organisation's **Access token** setting
4. `DEPOSIT_ZENODO_ACCESS_TOKEN`

OpenCDMP decides which accounts a user may publish under, and never falls back to the organisation's or the installation's account when a user chose their own. See [Publishing accounts and files](https://opencdmp.github.io/optional-services/deposit-services/#publishing-accounts-and-files).

## Register with OpenCDMP

Register the service for the whole installation in the configuration of the API service, or for one organisation under **Settings > Plugins > Add plugin**. Use the plugin id `zenodo`, and register a PDF and an RDA JSON transformer with it. See [Registering plugin services](https://opencdmp.github.io/deployment/configuration/plugin-services/) and [Organisation plugins](https://opencdmp.github.io/administering/tenant-management/plugins/).

## API

All endpoints are under `/api/deposit`. They accept only a Bearer token from the installation's Keycloak realm that carries the `plugins` audience; the OpenCDMP API service obtains one for each call. See [Let the plugin accept the API service's tokens](https://opencdmp.github.io/deployment/configuration/plugin-services/#plugin-tokens).

| Endpoint | Description |
| --- | --- |
| `POST /api/deposit` | Deposit a plan and return its DOI |
| `POST /api/deposit/preview` | Return the metadata that would be deposited, as an editable form |
| `GET /api/deposit/vocab/{fieldCode}?like=` | Search the vocabulary behind a review field |
| `GET /api/deposit/authenticate?authToken=` | Exchange a Zenodo OAuth 2.0 authorization code for an access token |
| `GET /api/deposit/configuration` | Return the service's `DepositConfiguration` |
| `GET /api/deposit/logo` | Return the Zenodo logo, Base64-encoded |

A deposit takes a `PlanDepositModel`. For a new version of a record deposited earlier, `previousDOI` holds the record's DOI:

```json
{
  "planModel": {
    "id": "<plan-id>",
    "label": "My research plan",
    "previousDOI": "<doi-of-the-earlier-version>"
  },
  "authInfo": {
    "authFields": [
      { "code": "zenodo-access-token", "textValue": "<user-access-token>" }
    ]
  }
}
```

The models are described in the [Deposit plugin guide](https://opencdmp.github.io/developers/plugins/deposit/#key-interfaces-and-classes) and [Common models](https://opencdmp.github.io/developers/plugins/common-models/).

## Deposit preview

The service reports `supportsPreview: true` in its configuration unless `DEPOSIT_ZENODO_SUPPORTS_PREVIEW` is `false`. OpenCDMP then shows users the record's metadata in a form before anything is submitted to Zenodo:

- `POST /api/deposit/preview` takes the same `PlanDepositModel` as a deposit; `authInfo` is not needed. It runs the same mapping as a deposit and returns a `DepositPreviewModel`: the form, with the mapped values filled in. Overrides saved earlier, in the optional `storedFields` and `storedGroups`, are merged in.
- `POST /api/deposit` applies the user's `editedFields` (single values) and `editedGroups` (repeatable groups) to the record just before it is submitted.

The contract is described in [Deposit preview and metadata review](https://opencdmp.github.io/developers/plugins/deposit-preview/).

### The form

Fields are Mapped (filled in from the plan and editable, such as `title`, `license` and `keywords`), Manual (empty for the user to complete, such as `embargo-reason`, `copyright` and `additional-communities`) or System (read-only, such as `resource-type` and `community`).

The form has up to fifteen groups. `license` and `language` take several values from a vocabulary, and `description` is rich text. `license` also accepts custom entries: a value that is not a lowercase vocabulary id is deposited as a custom licence (`rights: [{title: {en: ...}}]`).

| Group code | Contents |
|---|---|
| `files` | `files` (read-only): the names of the files the deposit will upload, when the platform provides them |
| `basic-information` | `title`, `description`, `publication-date`, `version`, `publisher`, `language`, `keywords`, `resource-type` |
| `license-access` | `license`, `access-files`, `embargo-until`, `embargo-reason`, `copyright` |
| `creators` | repeatable rows: `person-type`, `given-name`, `family-name`, `name`, `orcid`, `affiliation` |
| `contributors` | the fields of `creators`, plus `role` |
| `funding` | repeatable rows: `funder-name`, `funder-id`, `award-title`, `award-number` (funder and award fields autocomplete) |
| `additional-titles` | repeatable rows: `additional-title`, `additional-title-type`, `additional-title-lang` |
| `dates` | repeatable rows: `date-value`, `date-type`, `date-description` |
| `alternate-identifiers` | repeatable rows: `identifier-value`, `identifier-scheme` |
| `publishing-information` | `journal-title`, `journal-issn`, `journal-volume`, `journal-issue`, `journal-pages`, `imprint-title`, `imprint-isbn`, `imprint-place`, `imprint-pages`, `imprint-edition`, `thesis-university`, `thesis-department`, `thesis-type`, `thesis-date-submitted`, `thesis-date-defended` |
| `conference` | `conference-title`, `conference-acronym`, `conference-place`, `conference-dates`, `conference-session`, `conference-part`, `conference-website` |
| `software` | `software-repository-url`, `software-programming-language`, `software-development-status` |
| `additional-descriptions` | repeatable rows: `additional-description-text`, `additional-description-type` |
| `domain-specific` | Manual fields defined by the installation in `preview-extra-fields` (see below) |
| `additional-information` | `references`, `related-works` (read-only), `community` (read-only), `additional-communities` |

Field codes are unique across the form, and the service ignores codes it does not know. An organisation can hide groups and lock record defaults in its settings.

### Communities

The community set by the installation, the organisation or the semantics is read-only in the form. In `additional-communities`, users add further communities, with autocomplete against Zenodo's communities. Communities are not part of the record: after publishing a new record, the service submits it to the configured community and to the additional ones. A new version is not submitted again.

### Installation-specific fields

InvenioRDM has no API that lists an instance's custom fields, so declare fields such as Zenodo's Darwin Core fields (`dwc:*`) in configuration, for example in `/config/zenodo-<PROFILE>.yml`. They appear as Manual fields in the "Domain specific fields" group, and their values are written as they are under `custom_fields.<key>`:

```yaml
preview-extra-fields:
    fields:
        - key: "dwc:family"          # the custom_fields key; also the preview field code
          label: "Family (Darwin Core)"
          type: tags                  # text | textarea | tags | boolean | number | date
```

Zenodo drops a key that the instance does not register, without an error.

### How edits apply

- A blank edit to a single value never erases a mapped value.
- An edited repeatable group replaces the whole list. If all edited `creators` rows are empty, the service keeps the creators mapped from the plan, since Zenodo requires at least one creator.
- Saved overrides are merged into the preview by `DepositPreviewMergeHelper` from repository-deposit-base, with the [shared merge rules](https://opencdmp.github.io/developers/plugins/deposit-preview/#depositpreviewmergehelper).

### Vocabulary search

`GET /api/deposit/vocab/{fieldCode}?like=<term>` returns autocomplete options for the review fields marked `vocabSearch`:

| Field code | Source |
| --- | --- |
| `license`, `language` | InvenioRDM vocabularies API |
| `keywords` | InvenioRDM subjects API |
| `funder-name` | InvenioRDM funders API |
| `award-title`, `award-number` | InvenioRDM awards API |
| `additional-communities` | InvenioRDM communities API (options are community slugs) |
| `software-programming-language` | The programming-language list configured in the service |

Any other code returns an empty list, and so does a lookup Zenodo does not answer. Until the user searches, `license` and `language` show the short option lists included in the form.

## Related resources

- [Deposit services overview](https://opencdmp.github.io/optional-services/deposit-services/)
- [Zenodo semantics reference](https://opencdmp.github.io/optional-services/deposit-services/zenodo-semantics/)
- [Deposit services configuration](https://opencdmp.github.io/deployment/configuration/backend/deposit/#zenodo)
- [Registering plugin services](https://opencdmp.github.io/deployment/configuration/plugin-services/)
- [Deposit a plan](https://opencdmp.github.io/using/plans/deposit-a-plan/): the user guide
- [Deposit plugin guide](https://opencdmp.github.io/developers/plugins/deposit/): building your own deposit service

## Contributing

Report bugs and suggest changes in [GitHub Issues](https://github.com/OpenCDMP/repository-deposit-zenodo/issues). See [Contributing](https://opencdmp.github.io/developers/contributing/) for how to propose a change.

## License

repository-deposit-zenodo is licensed under the [European Union Public Licence (EUPL) v1.2](LICENSE).

## Contact

Questions: email opencdmp at cite.gr.
