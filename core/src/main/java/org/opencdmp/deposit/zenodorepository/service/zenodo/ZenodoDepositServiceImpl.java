package org.opencdmp.deposit.zenodorepository.service.zenodo;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.cite.tools.exception.MyApplicationException;
import gr.cite.tools.logging.LoggerService;
import gr.cite.tools.logging.MapLogEntry;
import org.opencdmp.commonmodels.models.FileEnvelopeModel;
import org.opencdmp.commonmodels.models.plan.PlanModel;
import org.opencdmp.commonmodels.models.plugin.PluginUserFieldModel;
import org.opencdmp.deposit.zenodorepository.model.ZenodoCommunity;
import org.opencdmp.deposit.zenodorepository.model.ZenodoDeposit;
import org.opencdmp.deposit.zenodorepository.model.ZenodoUploadFile;
import org.opencdmp.deposit.zenodorepository.model.builder.ZenodoBuilder;
import org.opencdmp.deposit.zenodorepository.service.storage.FileStorageService;
import org.opencdmp.depositbase.repository.DepositConfiguration;
import org.opencdmp.depositbase.repository.PlanDepositModel;
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
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
public class ZenodoDepositServiceImpl implements ZenodoDepositService {
    private static final LoggerService logger = new LoggerService(LoggerFactory.getLogger(ZenodoDepositServiceImpl.class));
    private static final String PUBLISH_ID = "doi";

    private static final String CONFIGURATION_FIELD_ACCESS_TOKEN = "zenodo-access-token";
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

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ZenodoServiceProperties zenodoServiceProperties;
    private final ZenodoBuilder zenodoBuilder;
    private final FileStorageService storageService;
    private final ResourceLoader resourceLoader;

    private byte[] logo;
    
    @Autowired
    public ZenodoDepositServiceImpl(ZenodoServiceProperties zenodoServiceProperties, ZenodoBuilder mapper, FileStorageService storageService, ResourceLoader resourceLoader){
        this.zenodoServiceProperties = zenodoServiceProperties;
        this.zenodoBuilder = mapper;
	    this.storageService = storageService;
        this.resourceLoader = resourceLoader;
        this.logo = null;
    }

    @Override
    public String deposit(PlanDepositModel planDepositModel) throws Exception {

        DepositConfiguration depositConfiguration = this.getConfiguration();

        if(depositConfiguration != null && planDepositModel != null && planDepositModel.getPlanModel() != null) {

            String zenodoToken = null;
            if (planDepositModel.getAuthInfo() != null) {
                if (planDepositModel.getAuthInfo().getAuthToken() != null && !planDepositModel.getAuthInfo().getAuthToken().isBlank()) zenodoToken = planDepositModel.getAuthInfo().getAuthToken();
                else if (planDepositModel.getAuthInfo().getAuthFields() != null && !planDepositModel.getAuthInfo().getAuthFields().isEmpty() && depositConfiguration.getUserConfigurationFields() != null) {
                    PluginUserFieldModel userFieldModel = planDepositModel.getAuthInfo().getAuthFields().stream().filter(x -> x.getCode().equals(CONFIGURATION_FIELD_ACCESS_TOKEN)).findFirst().orElse(null);
                    if (userFieldModel != null && userFieldModel.getTextValue() != null && !userFieldModel.getTextValue().isBlank()) zenodoToken = userFieldModel.getTextValue();
                }
            }

            if (zenodoToken == null || zenodoToken.isBlank()) {
                zenodoToken = depositConfiguration.getAccessToken();
            }

            String zenodoUrl = depositConfiguration.getRepositoryUrl();

            // First step, post call to Zenodo, to create the entry.
            WebClient zenodoClient = this.getWebClient();

            DepositConfiguration zenodoConfig = this.zenodoServiceProperties.getDepositConfiguration();
            if (zenodoConfig == null) return null;
            ZenodoDeposit deposit = zenodoBuilder.build(planDepositModel.getPlanModel());

            LinkedHashMap<String, String> links;
            String previousDOI = planDepositModel.getPlanModel().getPreviousDOI();
            String publishUrl;
            boolean isNewVersion = false;
            try {

                if (previousDOI == null) {
                    links = deposit(zenodoToken, zenodoUrl, zenodoClient, deposit);
                } else {
                        //It requires more than one step to create a new version
                        //First, get the deposit related to the concept DOI
                        links = depositNewVersion(zenodoToken, zenodoUrl, previousDOI, zenodoClient, deposit);
                        isNewVersion = true;
                }

                // Second step, add the file to the entry.

                if (links == null || !links.containsKey(ZENODO_LINKS_RECORD)) throw new MyApplicationException("record not found");

                FileEnvelopeModel pdfEnvelope = planDepositModel.getPlanModel().getPdfFile();
                this.uploadFile(zenodoToken, zenodoClient, links, pdfEnvelope);

                FileEnvelopeModel rdaJsonEnvelope = planDepositModel.getPlanModel().getRdaJsonFile();
                this.uploadFile(zenodoToken, zenodoClient, links, rdaJsonEnvelope);

                if (planDepositModel.getPlanModel().getSupportingFilesZip() != null) {
                    this.uploadFile(zenodoToken, zenodoClient, links, planDepositModel.getPlanModel().getSupportingFilesZip());
                }

                // Third post call to Zenodo to publish the entry and return the DOI.
                publishUrl = links.get(ZENODO_LINKS_RECORD) + "/draft/actions/publish" + "?access_token=" + zenodoToken;

                return this.publish(zenodoClient, publishUrl, zenodoToken, isNewVersion, planDepositModel.getPlanModel());

            } catch (HttpClientErrorException | HttpServerErrorException ex) {
                logger.error(ex.getMessage(), ex);
                Map<String, String> parsedException = objectMapper.readValue(ex.getResponseBodyAsString(), Map.class);
                throw new IOException(parsedException.get("message"), ex);
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

    private static LinkedHashMap<String, String> depositNewVersion(String zenodoToken, String zenodoUrl, String previousDOI, WebClient zenodoClient, ZenodoDeposit deposit) throws Exception {
        Map<String, Object> createResponse;
        LinkedHashMap<String, String> links;

        String id = previousDOI.substring(previousDOI.lastIndexOf(".") + 1);

        // if previous doi is parent doi get record to get the latest doi
        String recordUrl = zenodoUrl + "records/" + id + "?access_token=" + zenodoToken;
        Map<String, Object> record = zenodoClient.get()
                .uri(recordUrl)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (record == null) throw new MyApplicationException("record not found");

        Map<String, Object> metadata = (Map<String, Object>) record.getOrDefault("metadata", Collections.emptyMap());
        String doi = (String) metadata.get("doi");

        String listUrl = zenodoUrl + "records/" + (doi.substring(previousDOI.lastIndexOf(".") + 1)) + "/versions" + "?access_token=" + zenodoToken;
        logger.debug("listUrl = " + listUrl);
        ResponseEntity<List<Map>> listResponses = zenodoClient.post().uri(listUrl).retrieve().toEntityList(Map.class).block();
        if (listResponses == null || listResponses.getBody() == null || listResponses.getBody().isEmpty()) return null;
        createResponse = (Map<String, Object>) listResponses.getBody().get(0);
        logger.debug("createResponse-previousDoi:");
        logger.debug(objectMapper.writeValueAsString(createResponse));
        links = (LinkedHashMap<String, String>) createResponse.getOrDefault(ZENODO_LINKS, new LinkedHashMap<>());

        zenodoClient.put().uri(links.get(ZENODO_LINKS_SELF) + "?access_token=" + zenodoToken).headers(httpHeaders -> {
                    httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(deposit).exchangeToMono(mono ->
                        mono.statusCode().isError() ?
                                mono.createException().flatMap(Mono::error) :
                                mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})).block();

        return links;
    }


    private LinkedHashMap<String, String> deposit(String zenodoToken, String zenodoUrl, WebClient zenodoClient, ZenodoDeposit deposit) {
        Map<String, Object> createResponse;
        String createUrl = zenodoUrl + "records" + "?access_token=" + zenodoToken;
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
	    return (LinkedHashMap<String, String>) createResponse.getOrDefault(ZENODO_LINKS, null);
    }

    private void uploadFile(String zenodoToken, WebClient zenodoClient, LinkedHashMap<String, String> links, FileEnvelopeModel fileEnvelopeModel) {
        ZenodoUploadFile uploadFile = new ZenodoUploadFile(cleanFileName(fileEnvelopeModel.getFilename()));

        String addFileUrl = links.get(ZENODO_LINKS_RECORD) + "/draft/files" + "?access_token=" + zenodoToken;
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

        String contentFileUrl = links.get(ZENODO_LINKS_RECORD) + "/draft/files/" + uploadFile.getKey() + "/content" + "?access_token=" + zenodoToken;
        zenodoClient.put().uri(contentFileUrl)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(BodyInserters
                        .fromResource(new ByteArrayResource(fileBytes)))
                .retrieve().toEntity(Map.class).block();

        String commitFileUrl = links.get(ZENODO_LINKS_RECORD) + "/draft/files/" + uploadFile.getKey() + "/commit" + "?access_token=" + zenodoToken;
        zenodoClient.post().uri(commitFileUrl)
                .retrieve().toEntity(Map.class).block();

    }

    private String publish(WebClient webClient, String publishUrl, String zenodoToken, boolean isNewVersion, PlanModel planModel){
        logger.debug(new MapLogEntry("publish")
                .And("url", publishUrl));
        Map<String, Object> publishResponse = webClient.post().uri(publishUrl).bodyValue("").exchangeToMono(mono ->
                mono.statusCode().isError() ?
                        mono.createException().flatMap(Mono::error) :
                        mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})).block();
        if (publishResponse == null) throw new UnsupportedOperationException("Failed to publish to Zenodo");

        if(!isNewVersion) this.addCommunityEntry(webClient, zenodoToken, publishResponse, planModel);
        return (String) publishResponse.get(PUBLISH_ID);
    }

    private void addCommunityEntry(WebClient webClient, String zenodoToken, Map<String, Object> publishResponse, PlanModel planModel) {
        String communityId = this.zenodoBuilder.getCommunity(planModel);
        if (communityId != null && !communityId.isBlank()) {
            ZenodoCommunity.Community community = new ZenodoCommunity.Community();
            community.setId(communityId);

            ZenodoCommunity zenodoCommunity = new ZenodoCommunity();
            zenodoCommunity.setCommunities(List.of(community));

            try {
                var links = (LinkedHashMap<String, String>) publishResponse.getOrDefault(ZENODO_LINKS, null);
                webClient.post().uri(links.get(ZENODO_LINKS_SELF) + "/communities?access_token=" + zenodoToken).headers(httpHeaders -> {
                            httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                        })
                        .bodyValue(zenodoCommunity).exchangeToMono(mono ->
                                mono.statusCode().isError() ?
                                        mono.createException().flatMap(Mono::error) :
                                        mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})).block();
            } catch (Exception e) {
                logger.warn("Failed to add community entry '{}': {}", communityId, e.getMessage());
            }
        }
    }


    @Override
    public DepositConfiguration getConfiguration() {
        return this.zenodoServiceProperties.getDepositConfiguration();
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
                        .And("url", depositConfiguration.getRepositoryAccessTokenUrl())
                        .And("body", map));
                
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
                return response.mutate().build().bodyToMono(String.class)
                    .flatMap(body -> {
                        logger.error(new MapLogEntry("Response").And("method", response.request().getMethod().toString()).And("url", response.request().getURI()).And("status", response.statusCode().toString()).And("body", body));
                        return Mono.just(response);
                    });
            }
            return Mono.just(response);
            
        });
    }
}
