package org.opencdmp.deposit.zenodorepository.service.zenodo;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.cite.tools.exception.MyApplicationException;
import gr.cite.tools.logging.LoggerService;
import gr.cite.tools.logging.MapLogEntry;
import org.opencdmp.commonmodels.models.FileEnvelopeModel;
import org.opencdmp.commonmodels.models.plan.PlanModel;
import org.opencdmp.deposit.zenodorepository.model.ZenodoDeposit;
import org.opencdmp.deposit.zenodorepository.model.builder.ZenodoBuilder;
import org.opencdmp.deposit.zenodorepository.service.storage.FileStorageService;
import org.opencdmp.depositbase.repository.DepositConfiguration;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
public class ZenodoDepositServiceImpl implements ZenodoDepositService {
    private static final LoggerService logger = new LoggerService(LoggerFactory.getLogger(ZenodoDepositServiceImpl.class));
    private static final String PUBLISH_ID = "conceptdoi";

    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String GRANT_TYPE = "grant_type";
    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String CODE = "code";
    private static final String ZENODO_LINKS = "links";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String ZENODO_LINKS_BUCKET = "bucket";
    private static final String ZENODO_LINKS_PUBLISH = "publish";
    private static final String ZENODO_LINKS_SELF = "self";
    private static final String ZENODO_LINKS_LATEST_DRAFT = "latest_draft";
    private static final String ZENODO_METADATA = "metadata";
    private static final String ZENODO_METADATA_VERSION = "version";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ZenodoServiceProperties zenodoServiceProperties;
    private final ZenodoBuilder zenodoBuilder;
    private final FileStorageService storageService;

    private byte[] logo;
    
    @Autowired
    public ZenodoDepositServiceImpl(ZenodoServiceProperties zenodoServiceProperties, ZenodoBuilder mapper, FileStorageService storageService){
        this.zenodoServiceProperties = zenodoServiceProperties;
        this.zenodoBuilder = mapper;
	    this.storageService = storageService;
	    this.logo = null;
    }

    @Override
    public String deposit(PlanModel planModel, String zenodoToken) throws Exception {

        DepositConfiguration depositConfiguration = this.getConfiguration();

        if(depositConfiguration != null) {

            if (zenodoToken == null || zenodoToken.isEmpty()) {
                zenodoToken = depositConfiguration.getAccessToken();
            }

            String zenodoUrl = depositConfiguration.getRepositoryUrl();

            // First step, post call to Zenodo, to create the entry.
            WebClient zenodoClient = this.getWebClient();

            DepositConfiguration zenodoConfig = this.zenodoServiceProperties.getDepositConfiguration();
            if (zenodoConfig == null) return null;
            ZenodoDeposit deposit = zenodoBuilder.build(planModel);

            LinkedHashMap<String, String> links;
            String previousDOI = planModel.getPreviousDOI();
            String unpublishedUrl = null;
            String publishUrl;
            try {

                if (previousDOI == null) {
                    links = deposit(zenodoToken, zenodoUrl, zenodoClient, deposit);
                } else {
                    unpublishedUrl = this.getUnpublishedDOI(zenodoClient, zenodoUrl, previousDOI, zenodoToken, planModel.getVersion());
                    if (unpublishedUrl == null) {
                        //It requires more than one step to create a new version
                        //First, get the deposit related to the concept DOI
                        links = depositNewVersion(zenodoToken, zenodoUrl, previousDOI, zenodoClient, deposit);
                    } else {
                        links = depositFromPreviousDoi(zenodoToken, zenodoUrl, previousDOI, zenodoClient);
                    }
                }

                if (unpublishedUrl == null) {
                    // Second step, add the file to the entry.
                    FileEnvelopeModel pdfEnvelope = planModel.getPdfFile();

                    if (links == null || !links.containsKey(ZENODO_LINKS_BUCKET)) throw new MyApplicationException("bucket not found");
                    String addFileUrl = links.get(ZENODO_LINKS_BUCKET) + "/" + cleanFileName(pdfEnvelope.getFilename()) + "?access_token=" + zenodoToken;

                    byte[] pdfFileBytes = null;
                    if (this.getConfiguration().isUseSharedStorage() && pdfEnvelope.getFileRef() != null && !pdfEnvelope.getFileRef().isBlank()) {
                        pdfFileBytes = this.storageService.readFile(pdfEnvelope.getFileRef());
                    } 
                    if (pdfFileBytes == null || pdfFileBytes.length == 0){
                        pdfFileBytes = pdfEnvelope.getFile();
                    }
                    zenodoClient.put().uri(addFileUrl)
                            .body(BodyInserters
                                    .fromResource(new ByteArrayResource(pdfFileBytes)))
                            .retrieve().toEntity(Map.class).block();
                    FileEnvelopeModel rdaJsonEnvelope = planModel.getRdaJsonFile();

                    String jsonFileName = cleanFileName(rdaJsonEnvelope.getFilename());
                    addFileUrl = links.get(ZENODO_LINKS_BUCKET) + "/" + jsonFileName + "?access_token=" + zenodoToken;

                    byte[] rdaJsonBytes = null;
                    if (this.getConfiguration().isUseSharedStorage() && rdaJsonEnvelope.getFileRef() != null && !rdaJsonEnvelope.getFileRef().isBlank()) {
                        rdaJsonBytes = this.storageService.readFile(rdaJsonEnvelope.getFileRef());
                    }
                    if (rdaJsonBytes == null || rdaJsonBytes.length == 0){
                        rdaJsonBytes = rdaJsonEnvelope.getFile();
                    }
                    zenodoClient.put().uri(addFileUrl).headers(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM)).body(BodyInserters.fromResource(new ByteArrayResource(rdaJsonBytes))).retrieve().toEntity(Map.class).block();

                    if (planModel.getSupportingFilesZip() != null) {
                        String supportingFilesZipName = cleanFileName(planModel.getSupportingFilesZip().getFilename());

                        addFileUrl = links.get(ZENODO_LINKS_BUCKET) + "/" + supportingFilesZipName + "?access_token=" + zenodoToken;
                        zenodoClient.put().uri(addFileUrl).body(BodyInserters.fromResource(new ByteArrayResource(supportingFilesZipName.getBytes()))).retrieve().toEntity(Map.class).block();
                    }

                    // Third post call to Zenodo to publish the entry and return the DOI.
                    publishUrl = links.get(ZENODO_LINKS_PUBLISH) + "?access_token=" + zenodoToken;
                } else {
                    publishUrl = unpublishedUrl + "?access_token=" + zenodoToken;
                }

                return this.publish(zenodoClient, publishUrl);

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
        String listUrl = zenodoUrl + "deposit/depositions" + "?q=conceptdoi:\"" + previousDOI + "\"&access_token=" + zenodoToken;
        logger.debug("listUrl = " + listUrl);
        ResponseEntity<List<Map>> listResponses = zenodoClient.get().uri(listUrl).retrieve().toEntityList(Map.class).block();
        if (listResponses == null || listResponses.getBody() == null || listResponses.getBody().isEmpty()) return null;
        createResponse = (Map<String, Object>) listResponses.getBody().get(0);
        logger.debug("createResponse-previousDoi:");
        logger.debug(objectMapper.writeValueAsString(createResponse));
        links = (LinkedHashMap<String, String>) createResponse.getOrDefault(ZENODO_LINKS, new LinkedHashMap<>());
        
        //Second, make the new version (not in the links?)
        if (!links.containsKey(ZENODO_LINKS_LATEST_DRAFT)) throw new MyApplicationException("previousDOI not found");
        String newVersionUrl = links.get(ZENODO_LINKS_LATEST_DRAFT) + "/actions/newversion" + "?access_token=" + zenodoToken;
        logger.debug("new version url: " + newVersionUrl);
        createResponse = zenodoClient.post().uri(newVersionUrl)
                .exchangeToMono(mono ->
                                mono.statusCode().isError() ?
                                        mono.createException().flatMap(Mono::error) :
                                        mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        ).block();
        logger.debug("createResponse-newVersion:");
        logger.debug(objectMapper.writeValueAsString(createResponse));
        links = createResponse == null ? new LinkedHashMap<>() : (LinkedHashMap<String, String>) createResponse.getOrDefault(ZENODO_LINKS, new LinkedHashMap<>());
        
        //Third, get the new deposit
        if (!links.containsKey(ZENODO_LINKS_LATEST_DRAFT)) throw new MyApplicationException("can not create latest draft");
        String latestDraftUrl = links.get(ZENODO_LINKS_LATEST_DRAFT) + "?access_token=" + zenodoToken;
        createResponse = zenodoClient.get().uri(latestDraftUrl)
                .exchangeToMono(mono -> mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})).block();
        logger.debug("createResponse-latestDraft:");
        logger.debug(objectMapper.writeValueAsString(createResponse));
        links = createResponse == null ? new LinkedHashMap<>() : (LinkedHashMap<String, String>) createResponse.getOrDefault(ZENODO_LINKS, new LinkedHashMap<>());

        //At this point it might fail to perform the next requests so enclose them with try catch
        try {
            //Forth, update the new deposit's metadata
            String updateUrl = links.get(ZENODO_LINKS_SELF) + "?access_token=" + zenodoToken;
            logger.debug(new MapLogEntry("Deposit New Version")
                    .And("url", updateUrl)
                    .And("body", deposit));
            zenodoClient.put().uri(updateUrl)
                    .headers(httpHeaders -> {
                        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .bodyValue(deposit).retrieve().toEntity(Map.class).block();
            //And finally remove pre-existing files from it
            String fileListUrl = links.get(ZENODO_LINKS_SELF) + "/files" + "?access_token=" + zenodoToken;
            ResponseEntity<List<Map>> fileListResponse = zenodoClient.get().uri(fileListUrl).retrieve().toEntityList(Map.class).block();
            for (Map file : fileListResponse.getBody()) {
                String fileDeleteUrl = links.get(ZENODO_LINKS_SELF) + "/files/" + file.get("id") + "?access_token=" + zenodoToken;
                zenodoClient.delete().uri(fileDeleteUrl).retrieve().toEntity(Void.class).block();
            }
        } catch (Exception e) {
            //In case the last two steps fail delete the latest Deposit it in order to create a new one (only one at a time is allowed)
            //restTemplate.delete(latestDraftUrl);
            logger.error(e.getMessage(), e);
            zenodoClient.delete().uri(latestDraftUrl).retrieve().toEntity(Void.class).block();
            throw e;
        }
        return links;
    }

    private static LinkedHashMap<String, String> depositFromPreviousDoi(String zenodoToken, String zenodoUrl, String previousDOI, WebClient zenodoClient) {
        Map<String, LinkedHashMap<String, String>> createResponse;
        String listUrl = zenodoUrl + "deposit/depositions" + "?q=conceptdoi:\"" + previousDOI + "\"&access_token=" + zenodoToken;
        ResponseEntity<List<Map>> listResponses = zenodoClient.get().uri(listUrl).retrieve().toEntityList(Map.class).block();
        if (listResponses == null || listResponses.getBody() == null || listResponses.getBody().isEmpty()) return null;

        createResponse = (Map<String, LinkedHashMap<String, String>>) listResponses.getBody().get(0);

        return createResponse.getOrDefault(ZENODO_LINKS, null);
    }

    private LinkedHashMap<String, String> deposit(String zenodoToken, String zenodoUrl, WebClient zenodoClient, ZenodoDeposit deposit) {
        Map<String, Object> createResponse;
        String createUrl = zenodoUrl + "deposit/depositions" + "?access_token=" + zenodoToken;
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

    private String publish(WebClient webClient, String publishUrl){
        logger.debug(new MapLogEntry("publish")
                .And("url", publishUrl));
        Map<String, Object> publishResponse = webClient.post().uri(publishUrl).bodyValue("").exchangeToMono(mono ->
                mono.statusCode().isError() ?
                        mono.createException().flatMap(Mono::error) :
                        mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})).block();
        if (publishResponse == null) throw new UnsupportedOperationException("Failed to publish to Zenodo"); 
        return (String) publishResponse.get(PUBLISH_ID);
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
            } catch (HttpClientErrorException ex) {
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
                    java.io.File logoFile = ResourceUtils.getFile(this.zenodoServiceProperties.getLogo());
                    if (!logoFile.exists()) return null;
                    try(InputStream inputStream = new FileInputStream(logoFile)){
                        this.logo = inputStream.readAllBytes();
                    };
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
            String listUrl = zenodoUrl + "deposit/depositions" + "?q=conceptdoi:\"" + doi + "\"&access_token=" + token;
            ResponseEntity<List<Map>> listResponses = client.get().uri(listUrl).retrieve().toEntityList(Map.class).block();
            if (listResponses == null || listResponses.getBody() == null || listResponses.getBody().isEmpty()) return null;
            
            createResponse = (Map<String, LinkedHashMap<String, String>>) listResponses.getBody().get(0);
            metadata = createResponse.getOrDefault(ZENODO_METADATA, new LinkedHashMap<>());
            links = createResponse.getOrDefault(ZENODO_LINKS, new LinkedHashMap<>());

            if (metadata.get(ZENODO_METADATA_VERSION).equals(version.toString())) {
                return links.get(ZENODO_LINKS_PUBLISH);
            } else {
                return null;
            }
        }catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }
    
    private WebClient getWebClient(){
        return WebClient.builder().filters(exchangeFilterFunctions -> {
            exchangeFilterFunctions.add(logRequest());
            exchangeFilterFunctions.add(logResponse());
        }).codecs(codecs -> codecs
                .defaultCodecs()
                .maxInMemorySize(this.zenodoServiceProperties.getMaxInMemorySizeInBytes())
        ).build();
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
