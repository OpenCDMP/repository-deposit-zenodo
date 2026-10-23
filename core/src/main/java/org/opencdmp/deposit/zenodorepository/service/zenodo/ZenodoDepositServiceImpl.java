package org.opencdmp.deposit.zenodorepository.service.zenodo;

import gr.cite.tools.exception.MyApplicationException;
import gr.cite.tools.exception.MyValidationException;
import gr.cite.tools.logging.LoggerService;
import gr.cite.tools.logging.MapLogEntry;
import org.opencdmp.commonmodels.models.FileEnvelopeModel;
import org.opencdmp.commonmodels.models.deposit.DepositPreviewModel;
import org.opencdmp.commonmodels.models.plan.PlanModel;
import org.opencdmp.commonmodels.models.plugin.PluginUserFieldModel;
import org.opencdmp.deposit.zenodorepository.model.ZenodoCommunity;
import org.opencdmp.deposit.zenodorepository.model.ZenodoDeposit;
import org.opencdmp.deposit.zenodorepository.model.ZenodoUploadFile;
import org.opencdmp.deposit.zenodorepository.model.builder.ZenodoBuilder;
import org.opencdmp.deposit.zenodorepository.configuration.programminglanguages.ProgrammingLanguagesProperties;
import org.opencdmp.deposit.zenodorepository.configuration.semantics.SemanticsProperties;
import org.opencdmp.deposit.zenodorepository.configuration.semantics.ZenodoSemanticsManifest;
import org.opencdmp.deposit.zenodorepository.model.builder.ZenodoPreviewMapper;
import org.opencdmp.deposit.zenodorepository.service.storage.FileStorageService;
import org.opencdmp.deposit.zenodorepository.validation.ZenodoDepositValidator;
import org.opencdmp.deposit.zenodorepository.validation.ZenodoErrors;
import org.opencdmp.commonmodels.models.deposit.PreviewFieldOption;
import org.opencdmp.depositbase.repository.DepositAccount;
import org.opencdmp.depositbase.repository.DepositConfiguration;
import org.opencdmp.depositbase.repository.DepositPluginSettings;
import org.opencdmp.depositbase.repository.DepositPreviewMergeHelper;
import org.opencdmp.depositbase.repository.PlanDepositModel;
import tools.jackson.databind.JsonNode;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import tools.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
public class ZenodoDepositServiceImpl implements ZenodoDepositService {
    private static final LoggerService logger = new LoggerService(LoggerFactory.getLogger(ZenodoDepositServiceImpl.class));
    private static final String PUBLISH_ID = "doi";

    private static final String CONFIGURATION_FIELD_ACCESS_TOKEN = "zenodo-access-token";
    private static final String SETTING_ORGANISATION_ACCESS_TOKEN = "organisation-access-token";
    private static final String SETTING_ADDITIONAL_COMMUNITIES = "additional-communities";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String GRANT_TYPE = "grant_type";
    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String CODE = "code";
    private static final String ZENODO_LINKS = "links";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String ZENODO_LINKS_RECORD = "record";
    private static final String ZENODO_LINKS_SELF = "self";
    private static final String ZENODO_METADATA = "metadata";
    private static final String ZENODO_METADATA_VERSION = "version";
    private static final String ZENODO_ERRORS = "errors";
    private static final String INVALID_VALUES = "Zenodo does not accept some of the record's values.";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ZenodoServiceProperties zenodoServiceProperties;
    private final ZenodoBuilder zenodoBuilder;
    private final ZenodoPreviewMapper zenodoPreviewMapper;
    private final ProgrammingLanguagesProperties programmingLanguagesProperties;
    private final FileStorageService storageService;
    private final ResourceLoader resourceLoader;
    private final ZenodoDepositValidator zenodoDepositValidator;

    private byte[] logo;

    @Autowired
    public ZenodoDepositServiceImpl(ZenodoServiceProperties zenodoServiceProperties, ZenodoBuilder mapper, ZenodoPreviewMapper zenodoPreviewMapper, ProgrammingLanguagesProperties programmingLanguagesProperties, FileStorageService storageService, ResourceLoader resourceLoader, SemanticsProperties semanticsProperties, ZenodoDepositValidator zenodoDepositValidator){
        this.zenodoServiceProperties = zenodoServiceProperties;
        this.zenodoBuilder = mapper;
        this.zenodoPreviewMapper = zenodoPreviewMapper;
        this.programmingLanguagesProperties = programmingLanguagesProperties;
	    this.storageService = storageService;
        this.resourceLoader = resourceLoader;
        this.zenodoDepositValidator = zenodoDepositValidator;
        this.logo = null;
        this.zenodoServiceProperties.getDepositConfiguration().setSemantics(ZenodoSemanticsManifest.build(semanticsProperties, programmingLanguagesProperties));
    }

    @Override
    public DepositPreviewModel preview(PlanDepositModel planDepositModel) {
        if (planDepositModel == null || planDepositModel.getPlanModel() == null) return null;

        ZenodoDeposit deposit = zenodoBuilder.build(planDepositModel.getPlanModel());
        String community = zenodoBuilder.getCommunity(planDepositModel.getPlanModel());
        List<String> uploadFileNames = new ArrayList<>();
        PlanModel planModel = planDepositModel.getPlanModel();
        if (planModel.getPdfFile() != null && planModel.getPdfFile().getFilename() != null) uploadFileNames.add(planModel.getPdfFile().getFilename());
        if (planModel.getRdaJsonFile() != null && planModel.getRdaJsonFile().getFilename() != null) uploadFileNames.add(planModel.getRdaJsonFile().getFilename());
        if (planModel.getSupportingFilesZip() != null && planModel.getSupportingFilesZip().getFilename() != null) uploadFileNames.add(planModel.getSupportingFilesZip().getFilename());
        DepositPreviewModel preview = this.zenodoPreviewMapper.toPreview(deposit, community, uploadFileNames, DepositPluginSettings.of(planDepositModel, this.getConfiguration()));
        DepositPreviewMergeHelper.merge(preview, planDepositModel.getStoredFields(), planDepositModel.getStoredGroups());
        return preview;
    }

    @Override
    public List<PreviewFieldOption> searchVocabulary(String fieldCode, String like) {
        if (ZenodoPreviewMapper.FIELD_SOFTWARE_PROGRAMMING_LANGUAGE.equals(fieldCode)) return this.searchProgrammingLanguages(like);

        String path = switch (fieldCode) {
            case ZenodoPreviewMapper.FIELD_LICENSE -> "vocabularies/licenses";
            case ZenodoPreviewMapper.FIELD_LANGUAGE, ZenodoPreviewMapper.ITEM_ADDITIONAL_TITLE_LANG -> "vocabularies/languages";
            case ZenodoPreviewMapper.ITEM_AFFILIATION -> "affiliations";
            case ZenodoPreviewMapper.FIELD_KEYWORDS -> "subjects";
            case ZenodoPreviewMapper.ITEM_FUNDER_NAME -> "funders";
            case ZenodoPreviewMapper.FIELD_ADDITIONAL_COMMUNITIES -> "communities";
            case ZenodoPreviewMapper.ITEM_AWARD_TITLE, ZenodoPreviewMapper.ITEM_AWARD_NUMBER -> "awards";
            default -> null;
        };
        if (path == null) return List.of();

        String repositoryUrl = this.zenodoServiceProperties.getDepositConfiguration().getRepositoryUrl();
        List<PreviewFieldOption> options = new ArrayList<>();
        try {
            String response = this.getWebClient().get()
                    .uri(repositoryUrl + path, uriBuilder -> uriBuilder
                            .queryParam("size", 25)
                            .queryParamIfPresent("q", like != null && !like.isBlank() ? java.util.Optional.of(like) : java.util.Optional.empty())
                            .build())
                    .retrieve().bodyToMono(String.class).block();
            if (response == null) return List.of();
            JsonNode hits = objectMapper.readTree(response).path("hits").path("hits");
            for (JsonNode hit : hits) {
                switch (path) {
                    case "subjects" -> {
                        // free-text subjects: the human-readable term is both label and stored value
                        String subject = hit.path("subject").asText(null);
                        if (subject == null || subject.isBlank()) continue;
                        String scheme = hit.path("scheme").asText(null);
                        options.add(new PreviewFieldOption(scheme != null && !scheme.isBlank() ? subject + " (" + scheme + ")" : subject, subject));
                    }
                    case "funders" -> {
                        String name = hit.path("name").asText(null);
                        if (name == null || name.isBlank()) continue;
                        String country = hit.path("country_name").asText(null);
                        options.add(new PreviewFieldOption(country != null && !country.isBlank() ? name + " (" + country + ")" : name, name));
                    }
                    case "affiliations" -> {
                        // the review keeps an affiliation by its name
                        String name = hit.path("name").asText(null);
                        if (name == null || name.isBlank()) continue;
                        String country = hit.path("country").asText(null);
                        options.add(new PreviewFieldOption(country != null && !country.isBlank() ? name + " (" + country + ")" : name, name));
                    }
                    case "communities" -> {
                        // stored value is the community slug, which is what the inclusion request API expects
                        String slug = hit.path("slug").asText(null);
                        if (slug == null || slug.isBlank()) continue;
                        String title = hit.path("metadata").path("title").asText(null);
                        options.add(new PreviewFieldOption(title != null && !title.isBlank() ? title : slug, slug));
                    }
                    case "awards" -> {
                        String title = hit.path("title").path("en").asText(null);
                        String number = hit.path("number").asText(null);
                        String funder = hit.path("funder").path("name").asText(null);
                        if (ZenodoPreviewMapper.ITEM_AWARD_NUMBER.equals(fieldCode)) {
                            if (number == null || number.isBlank()) continue;
                            String label = number + (title != null && !title.isBlank() ? " — " + title : "") + (funder != null && !funder.isBlank() ? " (" + funder + ")" : "");
                            options.add(new PreviewFieldOption(label, number));
                        } else {
                            if (title == null || title.isBlank()) continue;
                            String label = title + (number != null && !number.isBlank() ? " (" + number + ")" : "") + (funder != null && !funder.isBlank() ? " — " + funder : "");
                            options.add(new PreviewFieldOption(label, title));
                        }
                    }
                    default -> {
                        String id = hit.path("id").asText(null);
                        if (id == null || id.isBlank()) continue;
                        String label = hit.path("title").path("en").asText(null);
                        options.add(new PreviewFieldOption(label != null && !label.isBlank() ? label : id, id));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("vocabulary search failed for " + fieldCode + ": " + e.getMessage(), e);
        }
        return options;
    }

    private List<PreviewFieldOption> searchProgrammingLanguages(String like) {
        List<String> codes = this.programmingLanguagesProperties.getProgrammingCodes();
        if (codes == null) return List.of();
        String term = like != null ? like.trim().toLowerCase(Locale.ROOT) : "";
        List<PreviewFieldOption> options = new ArrayList<>();
        for (String code : codes) {
            if (code == null || code.isBlank()) continue;
            if (!term.isEmpty() && !code.toLowerCase(Locale.ROOT).contains(term)) continue;
            options.add(new PreviewFieldOption(code, code));
            if (options.size() >= 25) break;
        }
        return options;
    }

    @Override
    public String deposit(PlanDepositModel planDepositModel) {

        DepositConfiguration depositConfiguration = this.getConfiguration();

        if(depositConfiguration != null && planDepositModel != null && planDepositModel.getPlanModel() != null) {

            DepositPluginSettings settings = DepositPluginSettings.of(planDepositModel, depositConfiguration);
            String zenodoToken = this.zenodoToken(planDepositModel, depositConfiguration, settings);

            String zenodoUrl = depositConfiguration.getRepositoryUrl();

            // First step, post call to Zenodo, to create the entry.
            // the token goes in a header, not the URL, because the request logger and Zenodo's errors print URLs
            WebClient zenodoClient = this.getWebClient().mutate().defaultHeaders(httpHeaders -> httpHeaders.setBearerAuth(zenodoToken)).build();

            DepositConfiguration zenodoConfig = this.zenodoServiceProperties.getDepositConfiguration();
            if (zenodoConfig == null) return null;
            ZenodoDeposit deposit = zenodoBuilder.build(planDepositModel.getPlanModel());
            this.zenodoPreviewMapper.applyOverrides(deposit, planDepositModel.getEditedFields(), planDepositModel.getEditedGroups(), settings);
            // values Zenodo would refuse stop the deposit here, before a draft or a file is sent
            Map<String, List<String>> problems = this.zenodoDepositValidator.validate(deposit);
            if (!problems.isEmpty()) throw ZenodoErrors.exception(INVALID_VALUES, problems);
            List<String> additionalCommunities = new ArrayList<>(settings.list(SETTING_ADDITIONAL_COMMUNITIES));
            additionalCommunities.addAll(this.zenodoPreviewMapper.extractAdditionalCommunities(planDepositModel.getEditedFields()));

            LinkedHashMap<String, String> links = null;
            String previousDOI = planDepositModel.getPlanModel().getPreviousDOI();
            String publishUrl;
            boolean isNewVersion = false;
            try {

                if (previousDOI == null) {
                    links = deposit(zenodoUrl, zenodoClient, deposit);
                } else {
                        //It requires more than one step to create a new version
                        //First, get the deposit related to the concept DOI
                        links = depositNewVersion(zenodoUrl, previousDOI, zenodoClient, deposit);
                        isNewVersion = true;
                }

                // Second step, add the file to the entry.

                if (links == null || !links.containsKey(ZENODO_LINKS_RECORD)) throw new MyApplicationException("record not found");

                // the platform leaves out the files the organisation chose not to deposit
                FileEnvelopeModel pdfEnvelope = planDepositModel.getPlanModel().getPdfFile();
                if (pdfEnvelope != null) this.uploadFile(zenodoClient, links, pdfEnvelope);

                FileEnvelopeModel rdaJsonEnvelope = planDepositModel.getPlanModel().getRdaJsonFile();
                if (rdaJsonEnvelope != null) this.uploadFile(zenodoClient, links, rdaJsonEnvelope);

                if (planDepositModel.getPlanModel().getSupportingFilesZip() != null) {
                    this.uploadFile(zenodoClient, links, planDepositModel.getPlanModel().getSupportingFilesZip());
                }

                // Third post call to Zenodo to publish the entry and return the DOI.
                publishUrl = links.get(ZENODO_LINKS_RECORD) + "/draft/actions/publish";

                return this.publish(zenodoClient, publishUrl, isNewVersion, planDepositModel.getPlanModel(), additionalCommunities);

            } catch (WebClientResponseException ex) {
                // a draft Zenodo would not publish is removed, so it does not stay behind in the account
                this.deleteDraft(zenodoClient, links);
                throw this.toException(ex);
            } catch (HttpClientErrorException | HttpServerErrorException ex) {
                logger.error(ex.getMessage(), ex);
                Map<String, String> parsedException = objectMapper.readValue(ex.getResponseBodyAsString(), Map.class);
                try {
                    throw new IOException(parsedException.get("message"), ex);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }

        return null;

    }
    
    private static String cleanFileName(String name){
        if (name == null || name.isEmpty()) return null;

        int extensionIndex = name.lastIndexOf('.');
        String extension = "";
        String namePart = "";
        if (extensionIndex > 0) {
            extension = name.substring(extensionIndex + 1);
            namePart = name.substring(0, extensionIndex);
        }
        
        if (!namePart.isEmpty()) namePart = namePart.replaceAll("[^a-zA-Z0-9_+ ]", "").replace(" ", "_").replace(",", "_");
        
        return namePart + "." + extension;
    }

    private LinkedHashMap<String, String> depositNewVersion(String zenodoUrl, String previousDOI, WebClient zenodoClient, ZenodoDeposit deposit) {
        Map<String, Object> createResponse;
        LinkedHashMap<String, String> links;

        String id = previousDOI.substring(previousDOI.lastIndexOf(".") + 1);

        // if previous doi is parent doi get record to get the latest doi
        String recordUrl = zenodoUrl + "records/" + id;
        Map<String, Object> record = zenodoClient.get()
                .uri(recordUrl)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (record == null) throw new MyApplicationException("record not found");

        Map<String, Object> metadata = (Map<String, Object>) record.getOrDefault("metadata", Collections.emptyMap());
        String doi = (String) metadata.get("doi");

        String listUrl = zenodoUrl + "records/" + (doi.substring(previousDOI.lastIndexOf(".") + 1)) + "/versions";
        logger.debug("listUrl = " + listUrl);
        ResponseEntity<List<Map>> listResponses = zenodoClient.post().uri(listUrl).retrieve().toEntityList(Map.class).block();
        if (listResponses == null || listResponses.getBody() == null || listResponses.getBody().isEmpty()) return null;
        createResponse = (Map<String, Object>) listResponses.getBody().get(0);
        logger.debug("createResponse-previousDoi:");
        logger.debug(objectMapper.writeValueAsString(createResponse));
        links = (LinkedHashMap<String, String>) createResponse.getOrDefault(ZENODO_LINKS, new LinkedHashMap<>());

        Map<String, Object> draft = zenodoClient.put().uri(links.get(ZENODO_LINKS_SELF)).headers(httpHeaders -> {
                    httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(deposit).exchangeToMono(mono ->
                        mono.statusCode().isError() ?
                                mono.createException().flatMap(Mono::error) :
                                mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})).block();
        this.failOnDraftErrors(zenodoClient, links, draft);

        return links;
    }


    private LinkedHashMap<String, String> deposit(String zenodoUrl, WebClient zenodoClient, ZenodoDeposit deposit) {
        Map<String, Object> createResponse;
        String createUrl = zenodoUrl + "records";
        logger.debug(new MapLogEntry("Deposit")
                .And("url", createUrl)
                .And("body", deposit));
        createResponse = zenodoClient.post().uri(createUrl).headers(httpHeaders -> {
            httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        })
        .bodyValue(deposit).exchangeToMono(mono ->   
                        mono.statusCode().isError() ?
                                mono.createException().flatMap(Mono::error) : 
                                mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})).block();
        LinkedHashMap<String, String> links = createResponse != null ? (LinkedHashMap<String, String>) createResponse.getOrDefault(ZENODO_LINKS, null) : null;
        this.failOnDraftErrors(zenodoClient, links, createResponse);
	    return links;
    }

    /**
     * Zenodo keeps a draft whatever its values, leaves out the ones it does not accept and lists them in "errors";
     * such a draft is removed and the deposit stops with what Zenodo said, instead of publishing without them.
     */
    private void failOnDraftErrors(WebClient zenodoClient, LinkedHashMap<String, String> links, Map<String, Object> draft) {
        if (draft == null || !(draft.get(ZENODO_ERRORS) instanceof List<?> errors) || errors.isEmpty()) return;
        Map<String, List<String>> problems = ZenodoErrors.fromZenodo(errors);
        logger.warn(new MapLogEntry("Zenodo did not accept the draft").And("errors", problems));
        this.deleteDraft(zenodoClient, links);
        throw ZenodoErrors.exception(INVALID_VALUES, problems);
    }

    private void deleteDraft(WebClient zenodoClient, LinkedHashMap<String, String> links) {
        if (links == null || links.get(ZENODO_LINKS_RECORD) == null) return;
        try {
            zenodoClient.delete().uri(links.get(ZENODO_LINKS_RECORD) + "/draft").retrieve().toBodilessEntity().block();
        } catch (Exception e) {
            logger.warn(new MapLogEntry("Could not remove the Zenodo draft").And("record", links.get(ZENODO_LINKS_RECORD)).And("error", e.getMessage()));
        }
    }

    /** What Zenodo answered, as a validation error when it refused the values and otherwise as it said it. */
    private RuntimeException toException(WebClientResponseException ex) {
        logger.error(ex.getMessage(), ex);
        Map<String, Object> body = null;
        try {
            body = objectMapper.readValue(ex.getResponseBodyAsString(), new tools.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            // not a JSON answer
        }
        Map<String, List<String>> problems = body != null ? ZenodoErrors.fromZenodo(body.get(ZENODO_ERRORS)) : Map.of();
        if (!problems.isEmpty()) return ZenodoErrors.exception(INVALID_VALUES, problems);
        String message = body != null && body.get("message") != null ? body.get("message").toString() : ex.getStatusText();
        if (ex.getStatusCode().is4xxClientError()) return new MyValidationException("Zenodo refused the deposit: " + message);
        return new MyApplicationException("Zenodo could not take the deposit: " + message);
    }

    private void uploadFile(WebClient zenodoClient, LinkedHashMap<String, String> links, FileEnvelopeModel fileEnvelopeModel) {
        ZenodoUploadFile uploadFile = new ZenodoUploadFile(cleanFileName(fileEnvelopeModel.getFilename()));

        String addFileUrl = links.get(ZENODO_LINKS_RECORD) + "/draft/files";
        logger.debug(new MapLogEntry("Deposit")
                .And("url", addFileUrl)
                .And("body", uploadFile));

        zenodoClient.post().uri(addFileUrl)
                .bodyValue(List.of(uploadFile))
                .headers(httpHeaders -> {
                    httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                }).exchangeToMono(mono ->
                        mono.statusCode().isError() ?
                                mono.createException().flatMap(Mono::error) :
                                mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})).block();

        byte[] fileBytes = null;
        if (this.getConfiguration().isUseSharedStorage() && fileEnvelopeModel.getFileRef() != null && !fileEnvelopeModel.getFileRef().isBlank()) {
            fileBytes = this.storageService.readFile(fileEnvelopeModel.getFileRef());
        }
        if (fileBytes == null || fileBytes.length == 0){
            fileBytes = fileEnvelopeModel.getFile();
        }

        String contentFileUrl = links.get(ZENODO_LINKS_RECORD) + "/draft/files/" + uploadFile.getKey() + "/content";
        zenodoClient.put().uri(contentFileUrl)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(BodyInserters
                        .fromResource(new ByteArrayResource(fileBytes)))
                .retrieve().toEntity(Map.class).block();

        String commitFileUrl = links.get(ZENODO_LINKS_RECORD) + "/draft/files/" + uploadFile.getKey() + "/commit";
        zenodoClient.post().uri(commitFileUrl)
                .retrieve().toEntity(Map.class).block();

    }

    private String publish(WebClient webClient, String publishUrl, boolean isNewVersion, PlanModel planModel, List<String> additionalCommunities){
        logger.debug(new MapLogEntry("publish")
                .And("url", publishUrl));
        Map<String, Object> publishResponse = webClient.post().uri(publishUrl).bodyValue("").exchangeToMono(mono ->
                mono.statusCode().isError() ?
                        mono.createException().flatMap(Mono::error) :
                        mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})).block();
        if (publishResponse == null) throw new UnsupportedOperationException("Failed to publish to Zenodo");

        if(!isNewVersion) this.addCommunityEntry(webClient, publishResponse, planModel, additionalCommunities);
        return (String) publishResponse.get(PUBLISH_ID);
    }

    private void addCommunityEntry(WebClient webClient, Map<String, Object> publishResponse, PlanModel planModel, List<String> additionalCommunities) {
        String communityId = this.zenodoBuilder.getCommunity(planModel);
        List<String> slugs = new ArrayList<>();
        if (communityId != null && !communityId.isBlank()) slugs.add(communityId);
        if (additionalCommunities != null) {
            for (String slug : additionalCommunities) {
                if (slug != null && !slug.isBlank() && !slugs.contains(slug)) slugs.add(slug);
            }
        }
        if (!slugs.isEmpty()) {
            List<ZenodoCommunity.Community> communities = new ArrayList<>();
            for (String slug : slugs) {
                ZenodoCommunity.Community community = new ZenodoCommunity.Community();
                community.setId(slug);
                communities.add(community);
            }

            ZenodoCommunity zenodoCommunity = new ZenodoCommunity();
            zenodoCommunity.setCommunities(communities);

            try {
                var links = (LinkedHashMap<String, String>) publishResponse.getOrDefault(ZENODO_LINKS, null);
                webClient.post().uri(links.get(ZENODO_LINKS_SELF) + "/communities").headers(httpHeaders -> {
                            httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                        })
                        .bodyValue(zenodoCommunity).exchangeToMono(mono ->
                                mono.statusCode().isError() ?
                                        mono.createException().flatMap(Mono::error) :
                                        mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})).block();
            } catch (Exception e) {
                logger.warn("Failed to add community entries '{}': {}", slugs, e.getMessage());
            }
        }
    }


    @Override
    public DepositConfiguration getConfiguration() {
        return this.zenodoServiceProperties.getDepositConfiguration();
    }
    
    private String zenodoToken(PlanDepositModel planDepositModel, DepositConfiguration depositConfiguration, DepositPluginSettings settings) {
        String zenodoToken = null;
        if (planDepositModel.getAuthInfo() != null) {
            if (planDepositModel.getAuthInfo().getAuthToken() != null && !planDepositModel.getAuthInfo().getAuthToken().isBlank()) zenodoToken = planDepositModel.getAuthInfo().getAuthToken();
            else if (planDepositModel.getAuthInfo().getAuthFields() != null && !planDepositModel.getAuthInfo().getAuthFields().isEmpty() && depositConfiguration.getUserConfigurationFields() != null) {
                PluginUserFieldModel userFieldModel = planDepositModel.getAuthInfo().getAuthFields().stream().filter(x -> x.getCode().equals(CONFIGURATION_FIELD_ACCESS_TOKEN)).findFirst().orElse(null);
                if (userFieldModel != null && userFieldModel.getTextValue() != null && !userFieldModel.getTextValue().isBlank()) zenodoToken = userFieldModel.getTextValue();
            }
        }

        if (zenodoToken == null || zenodoToken.isBlank()) {
            zenodoToken = settings.text(SETTING_ORGANISATION_ACCESS_TOKEN, depositConfiguration.getAccessToken());
        }
        return zenodoToken;
    }

    @Override
    public DepositAccount account(PlanDepositModel planDepositModel) {
        DepositConfiguration depositConfiguration = this.getConfiguration();
        if (depositConfiguration == null || planDepositModel == null) return null;

        String zenodoToken = this.zenodoToken(planDepositModel, depositConfiguration, DepositPluginSettings.of(planDepositModel, depositConfiguration));
        if (zenodoToken == null || zenodoToken.isBlank()) return null;

        try {
            // the token goes in the header, not the URL, because the request logger prints URLs
            Map<String, Object> me = this.getWebClient().get().uri(depositConfiguration.getRepositoryUrl() + "me").headers(httpHeaders -> {
                httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                httpHeaders.setBearerAuth(zenodoToken);
            }).exchangeToMono(mono -> mono.statusCode().isError() ? mono.createException().flatMap(Mono::error) : mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})).block();
            if (me == null || me.get("id") == null) return null;

            Object profile = me.get("profile");
            String fullName = profile instanceof Map<?, ?> profileMap && profileMap.get("full_name") instanceof String value && !value.isBlank() ? value : null;
            String username = me.get("username") instanceof String value && !value.isBlank() ? value : null;
            String email = me.get("email") instanceof String value && !value.isBlank() ? value : null;
            String name = fullName != null ? fullName : username != null ? username : email;
            if (name != null && email != null && !name.equals(email)) name = name + " (" + email + ")";

            DepositAccount account = new DepositAccount();
            account.setId(me.get("id").toString());
            account.setName(name);
            return account;
        } catch (Exception ex) {
            logger.error("could not read the Zenodo account of the deposit", ex);
            return null;
        }
    }

    @Override
    public String authenticate(String code){

        DepositConfiguration depositConfiguration = this.getConfiguration();

        if(depositConfiguration != null) {

            WebClient client = WebClient.builder().filters(exchangeFilterFunctions -> {
                exchangeFilterFunctions.add(logRequest());
                exchangeFilterFunctions.add(logResponse());
            }).defaultHeaders(httpHeaders -> {
                httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            }).build();

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add(CLIENT_ID, depositConfiguration.getRepositoryClientId());
            map.add(CLIENT_SECRET, depositConfiguration.getRepositoryClientSecret());
            map.add(GRANT_TYPE, AUTHORIZATION_CODE);
            map.add(CODE, code);
            map.add(REDIRECT_URI, depositConfiguration.getRedirectUri());
            
            try {
                logger.debug(new MapLogEntry("Get Access Token")
                        .And("url", depositConfiguration.getRepositoryAccessTokenUrl()));
                
                Map<String, Object> values = client.post().uri(depositConfiguration.getRepositoryAccessTokenUrl()).bodyValue(map).exchangeToMono(mono ->
                        mono.statusCode().isError() ?
                                mono.createException().flatMap(Mono::error) :
                                mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                                })).block();
                
	            return values != null ? (String) values.getOrDefault(ACCESS_TOKEN, null) : null;
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                return null;
            }
        }

        return null;
    }

    @Override
    public String getLogo() {
        DepositConfiguration zenodoConfig = this.zenodoServiceProperties.getDepositConfiguration();
        if(zenodoConfig != null && zenodoConfig.isHasLogo() && this.zenodoServiceProperties.getLogo() != null && !this.zenodoServiceProperties.getLogo().isBlank()) {
            if (this.logo == null) {
                try {
                    Resource resource = resourceLoader.getResource(this.zenodoServiceProperties.getLogo());
                    if(!resource.isReadable()) return null;
                    try(InputStream inputStream = resource.getInputStream()) {
                        this.logo = inputStream.readAllBytes();
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
            return (this.logo != null && this.logo.length != 0) ? Base64.getEncoder().encodeToString(this.logo) : null;
        }
        return null;
    }

    private String getUnpublishedDOI(WebClient client, String zenodoUrl, String doi, String token, Short version) {
        try {
            Map<String, LinkedHashMap<String, String>> createResponse = null;
            LinkedHashMap<String, String> links;
            LinkedHashMap<String, String> metadata;
            String listUrl = zenodoUrl + "records/" + (doi.substring(doi.lastIndexOf(".") + 1));
            ResponseEntity<List<Map>> listResponses = client.get().uri(listUrl).retrieve().toEntityList(Map.class).block();
            if (listResponses == null || listResponses.getBody() == null || listResponses.getBody().isEmpty()) return null;
            
            createResponse = (Map<String, LinkedHashMap<String, String>>) listResponses.getBody().get(0);
            metadata = createResponse.getOrDefault(ZENODO_METADATA, new LinkedHashMap<>());
            links = createResponse.getOrDefault(ZENODO_LINKS, new LinkedHashMap<>());

            if (metadata.get(ZENODO_METADATA_VERSION).equals(version.toString())) {
                return links.get(ZENODO_LINKS_SELF);
            } else {
                return null;
            }
        }catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }
    
    private WebClient getWebClient(){
        HttpClient httpClient = HttpClient.create().followRedirect(true);

        return WebClient.builder().filters(exchangeFilterFunctions -> {
            exchangeFilterFunctions.add(logRequest());
            exchangeFilterFunctions.add(logResponse());
        }).codecs(codecs -> codecs
                .defaultCodecs()
                .maxInMemorySize(this.zenodoServiceProperties.getMaxInMemorySizeInBytes())
        ).clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            logger.debug(new MapLogEntry("Request").And("method", clientRequest.method().toString()).And("url", clientRequest.url().toString()));
            return Mono.just(clientRequest);
        });
    }

    private static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                // reading the body consumes it, so the response passed on gets it back for the caller to read Zenodo's errors
                return response.bodyToMono(String.class).defaultIfEmpty("")
                    .map(body -> {
                        logger.error(new MapLogEntry("Response").And("method", response.request().getMethod().toString()).And("url", response.request().getURI()).And("status", response.statusCode().toString()).And("body", body));
                        return response.mutate().body(body).build();
                    });
            }
            return Mono.just(response);
            
        });
    }
}
