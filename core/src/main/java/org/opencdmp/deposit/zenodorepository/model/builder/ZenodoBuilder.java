package org.opencdmp.deposit.zenodorepository.model.builder;

import com.ibm.icu.util.ULocale;
import gr.cite.tools.logging.LoggerService;
import gr.cite.tools.logging.MapLogEntry;
import org.opencdmp.commonmodels.enums.PlanAccessType;
import org.opencdmp.commonmodels.models.PlanUserModel;
import org.opencdmp.commonmodels.models.description.*;
import org.opencdmp.commonmodels.models.descriptiotemplate.DefinitionModel;
import org.opencdmp.commonmodels.models.descriptiotemplate.FieldSetModel;
import org.opencdmp.commonmodels.models.descriptiotemplate.fielddata.RadioBoxDataModel;
import org.opencdmp.commonmodels.models.descriptiotemplate.fielddata.SelectDataModel;
import org.opencdmp.commonmodels.models.plan.PlanBlueprintValueModel;
import org.opencdmp.commonmodels.models.plan.PlanModel;
import org.opencdmp.commonmodels.models.planblueprint.SectionModel;
import org.opencdmp.commonmodels.models.planreference.PlanReferenceModel;
import org.opencdmp.commonmodels.models.reference.ReferenceFieldModel;
import org.opencdmp.commonmodels.models.reference.ReferenceModel;
import org.opencdmp.deposit.zenodorepository.configuration.funder.FunderProperties;
import org.opencdmp.deposit.zenodorepository.configuration.pid.PidProperties;
import org.opencdmp.deposit.zenodorepository.configuration.programminglanguages.ProgrammingLanguagesProperties;
import org.opencdmp.deposit.zenodorepository.configuration.semantics.SemanticsProperties;
import org.opencdmp.deposit.zenodorepository.enums.ZenodoAccessRight;
import org.opencdmp.deposit.zenodorepository.enums.ZenodoAdditionalTitleIdType;
import org.opencdmp.deposit.zenodorepository.model.*;
import org.opencdmp.deposit.zenodorepository.service.descriptiontemplatesearcher.TemplateFieldSearcherService;
import org.opencdmp.deposit.zenodorepository.service.zenodo.ZenodoDepositServiceImpl;
import org.opencdmp.deposit.zenodorepository.service.zenodo.ZenodoServiceProperties;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ZenodoBuilder {
    private static final LoggerService logger = new LoggerService(LoggerFactory.getLogger(ZenodoDepositServiceImpl.class));

    private static final String IS_IDENTICAL_TO = "isidenticalto";
    private static final String RELATED_IDENTIFIER_SCHEME_URL = "url";
    private static final String RELATED_IDENTIFIER_SCHEME_OTHER = "other";
    private static final String CONTRIBUTOR_TYPE_RESEARCHER = "researcher";
    private static final String CONTRIBUTOR_TYPE_PROJECT_MANAGER = "projectmember";
    private static final String PERSON_OR_ORG_TYPE = "personal";

    private static final String SEMANTIC_COMMUNITY = "zenodo.community";
    private static final String SEMANTIC_PUBLICATION_DATE = "zenodo.publication_date";
    private static final String SEMANTIC_SUBJECT = "zenodo.subject";
    private static final String SEMANTIC_LANGUAGE = "zenodo.language";
    private static final String SEMANTIC_REFERENCE = "zenodo.reference";

    private static final String SEMANTIC_ADDITIONAL_TITLE = "zenodo.additional_title.title";
    private static final String SEMANTIC_ADDITIONAL_TITLE_TYPE = "zenodo.additional_title.type";
    private static final String SEMANTIC_ADDITIONAL_TITLE_LANGUAGE = "zenodo.additional_title.language";

    private static final String SEMANTIC_JOURNAL_TITLE = "zenodo.publishing_information.journal.title";
    private static final String SEMANTIC_JOURNAL_ISSN = "zenodo.publishing_information.journal.issn";
    private static final String SEMANTIC_JOURNAL_VOLUME = "zenodo.publishing_information.journal.volume";
    private static final String SEMANTIC_JOURNAL_ISSUE = "zenodo.publishing_information.journal.issue";
    private static final String SEMANTIC_JOURNAL_PAGE_RANGE = "zenodo.publishing_information.journal.page_range_or_article_number";

    private static final String SEMANTIC_IMPRINT_TITLE = "zenodo.publishing_information.imprint.title";
    private static final String SEMANTIC_IMPRINT_ISBN = "zenodo.publishing_information.imprint.isbn";
    private static final String SEMANTIC_IMPRINT_PLACE = "zenodo.publishing_information.imprint.place";
    private static final String SEMANTIC_IMPRINT_PAGINATION = "zenodo.publishing_information.imprint.pagination";

    private static final String SEMANTIC_THESIS_UNIVERSITY = "zenodo.publishing_information.thesis.university";

    private static final String SEMANTIC_SOFTWARE_PROGRAMMING_LANGUAGE = "zenodo.software.programming-language";
    private static final String SEMANTIC_SOFTWARE_REPOSITORY_URL = "zenodo.software.repository-url";
    private static final String SEMANTIC_SOFTWARE_DEVELOPMENT_STATUS = "zenodo.software.development-status";

    private static final String SEMANTIC_CONFERENCE_TITLE = "zenodo.conference.title";
    private static final String SEMANTIC_CONFERENCE_ACRONYM = "zenodo.conference.acronym";
    private static final String SEMANTIC_CONFERENCE_PLACE = "zenodo.conference.place";
    private static final String SEMANTIC_CONFERENCE_DATES = "zenodo.conference.dates";
    private static final String SEMANTIC_CONFERENCE_WEBSITE = "zenodo.conference.website";
    private static final String SEMANTIC_CONFERENCE_SESSION = "zenodo.conference.session";
    private static final String SEMANTIC_CONFERENCE_PART = "zenodo.conference.part";

    private final TemplateFieldSearcherService templateFieldSearcherService;
    private final PidProperties pidProperties;
    private final SemanticsProperties semanticsProperties;
    private final FunderProperties funderProperties;
    private final ZenodoServiceProperties zenodoServiceProperties;
    private final ProgrammingLanguagesProperties programmingLanguagesProperties;

    @Autowired
    public ZenodoBuilder(TemplateFieldSearcherService templateFieldSearcherService, PidProperties pidProperties, SemanticsProperties semanticsProperties, FunderProperties funderProperties, ZenodoServiceProperties zenodoServiceProperties, ProgrammingLanguagesProperties programmingLanguagesProperties){
        this.templateFieldSearcherService = templateFieldSearcherService;
        this.pidProperties = pidProperties;
        this.semanticsProperties = semanticsProperties;
        this.funderProperties = funderProperties;
	    this.zenodoServiceProperties = zenodoServiceProperties;
        this.programmingLanguagesProperties = programmingLanguagesProperties;
    }

    public String getCommunity(PlanModel planModel){
        if (planModel == null) return zenodoServiceProperties.getCommunity();

        //plan blueprint semantics
        List<org.opencdmp.commonmodels.models.planblueprint.FieldModel> blueprintFieldsWithSemantic = this.getFieldOfSemantic(planModel, SEMANTIC_COMMUNITY);
        for (org.opencdmp.commonmodels.models.planblueprint.FieldModel field: blueprintFieldsWithSemantic) {
            PlanBlueprintValueModel planBlueprintValueModel = this.getPlanBlueprintValue(planModel, field.getId());
            if (planBlueprintValueModel != null) {
                if (planBlueprintValueModel.getValue() != null && !planBlueprintValueModel.getValue().isBlank()) return planBlueprintValueModel.getValue();
            }
        }

        //description template
        for (DescriptionModel descriptionModel: planModel.getDescriptions()) {
            List<org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel> fieldsWithSemantics = this.findSchematicValues(SEMANTIC_COMMUNITY, descriptionModel.getDescriptionTemplate().getDefinition());
            Set<String> values = extractSchematicValues(fieldsWithSemantics, descriptionModel.getProperties());
            String value = values.stream().findFirst().orElse(null);
            if (value != null) return value;
        }

        return zenodoServiceProperties.getCommunity();
    }

    public ZenodoDeposit build(PlanModel plan) {
        ZenodoDeposit deposit = new ZenodoDeposit();

        this.applyAccess(plan, deposit);
        this.applyZenodoRelator(plan, deposit);

        deposit.getMetadata().setTitle(plan.getLabel());
        deposit.getMetadata().setResourceType(new ZenodoResourceType(this.zenodoServiceProperties.getResourceTypeId()));
        deposit.getMetadata().setPublisher(this.zenodoServiceProperties.getPublisherName());
        deposit.getMetadata().setPublicationDate(DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(Instant.now()));
        deposit.getMetadata().setDescription((plan.getDescription() != null && !plan.getDescription().isEmpty() ? plan.getDescription() : "<p></p>"));
        deposit.getMetadata().setVersion(String.valueOf(plan.getVersion()));

        List<org.opencdmp.commonmodels.models.planblueprint.FieldModel> fieldOfSemantic = this.getFieldOfSemantic(plan, SEMANTIC_PUBLICATION_DATE);
        if (!fieldOfSemantic.isEmpty()){
            PlanBlueprintValueModel planBlueprintValueModel = this.getPlanBlueprintValue(plan, fieldOfSemantic.getFirst().getId());
            if (planBlueprintValueModel != null && planBlueprintValueModel.getDateValue() != null) {
                deposit.getMetadata().setPublicationDate(DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(planBlueprintValueModel.getDateValue()));
            } else if (planBlueprintValueModel != null && planBlueprintValueModel.getValue() != null && !planBlueprintValueModel.getValue().isBlank()){
                try {
                    Instant instant = Instant.parse(planBlueprintValueModel.getValue());
                    deposit.getMetadata().setPublicationDate(DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(instant));
                }catch (Exception e){
                    logger.error(e.getMessage(), e);
                }
            }
        }

        this.applyIsIdenticalTo(plan, deposit);
        this.applyLicenses(plan, deposit);
        this.applyResearchers(plan, deposit);
        this.applyFunding(plan, deposit);
        this.applyCreators(plan, deposit);
        this.applyLanguages(plan, deposit);
        this.applySubjects(plan, deposit);
        this.applyAdditionalTitle(plan, deposit);
        this.applyReferences(plan, deposit);
        this.applyDates(plan, deposit);
        this.applyIdentifiers(plan, deposit);
        this.applyCustomFields(plan, deposit);

        return deposit;
    }

    private void applyAccess(PlanModel plan, ZenodoDeposit deposit){

        ZenodoDepositAccess access = new ZenodoDepositAccess();
        access.setRecord(ZenodoAccessRight.PUBLIC);

        ZenodoDepositAccess.Embargo embargo = new ZenodoDepositAccess.Embargo();
        embargo.setActive(false);

        if (plan.getAccessType() == null) {
            access.setFiles(ZenodoAccessRight.RESTRICTED);
        } else {
            if (plan.getAccessType().equals(PlanAccessType.Public)) {
                Instant publicationDate = plan.getFinalizedAt();
                if (publicationDate == null) publicationDate = Instant.now().minusSeconds(1);

                if (publicationDate.isBefore(Instant.now())) {
                    access.setFiles(ZenodoAccessRight.PUBLIC);
                } else {
                    embargo.setActive(true);
                    embargo.setUntil(DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(publicationDate));
                }
            } else {
                access.setFiles(ZenodoAccessRight.RESTRICTED);
            }
        }

        access.setEmbargo(embargo);
        deposit.setAccess(access);
    }

    private Set<String> extractSchematicValues(List<org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel> fields, PropertyDefinitionModel propertyDefinition) {
        Set<String> values = new HashSet<>();
        for (org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel field : fields) {
            if (field.getData() == null) continue;
            List<FieldModel> valueFields = this.findValueFieldsByIds(field.getId(), propertyDefinition);
            for (FieldModel valueField : valueFields) {
                switch (field.getData().getFieldType()) {
                    case FREE_TEXT, TEXT_AREA, RICH_TEXT_AREA -> {
                        if (valueField.getTextValue() != null && !valueField.getTextValue().isBlank()) values.add(valueField.getTextValue());
                    }
                    case BOOLEAN_DECISION, CHECK_BOX -> {
                        if (valueField.getBooleanValue() != null) values.add(valueField.getBooleanValue().toString());
                    }
                    case DATE_PICKER -> {
                        if (valueField.getDateValue() != null) values.add(DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(valueField.getDateValue()));
                    }
                    case DATASET_IDENTIFIER, VALIDATION -> {
                        if (valueField.getExternalIdentifier() != null && valueField.getExternalIdentifier().getIdentifier() != null && !valueField.getExternalIdentifier().getIdentifier().isBlank()) {
                            values.add(valueField.getExternalIdentifier().getIdentifier());
                        }
                    }
                    case TAGS -> {
                        if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                            values.addAll(valueField.getTextListValue());
                        }
                    }
                    case SELECT -> {
                        if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                            SelectDataModel selectDataModel = (SelectDataModel)field.getData();
                            if (selectDataModel != null && selectDataModel.getOptions() != null && !selectDataModel.getOptions().isEmpty()){
                                for (SelectDataModel.OptionModel option : selectDataModel.getOptions()){
                                    if (valueField.getTextListValue().contains(option.getValue()) || valueField.getTextListValue().contains(option.getLabel())) values.add(option.getLabel());
                                }
                            }
                        }
                    }
                    case RADIO_BOX -> {
                        if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                            RadioBoxDataModel radioBoxModel = (RadioBoxDataModel)field.getData();
                            if (radioBoxModel != null && radioBoxModel.getOptions() != null && !radioBoxModel.getOptions().isEmpty()){
                                for (RadioBoxDataModel.RadioBoxOptionModel option : radioBoxModel.getOptions()){
                                    if (valueField.getTextListValue().contains(option.getValue()) || valueField.getTextListValue().contains(option.getLabel())) values.add(option.getLabel());
                                }
                            }
                        }
                    }
                    case REFERENCE_TYPES -> {
                        if (valueField.getReferences() != null && !valueField.getReferences().isEmpty()) {
                            for (ReferenceModel referenceModel : valueField.getReferences()) {
                                if (referenceModel == null
                                        || referenceModel.getType() == null || referenceModel.getType().getCode() == null || referenceModel.getType().getCode().isBlank()
                                        || referenceModel.getDefinition() == null || referenceModel.getDefinition().getFields() == null || referenceModel.getDefinition().getFields().isEmpty()) continue;
                                if (referenceModel.getReference() != null && !referenceModel.getReference().isBlank()) {
                                    values.add(referenceModel.getReference());
                                }
                            }
                        }
                    }
                }
            }
        }
        return values;
    }

    private String extractSchematicSingleValue(org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel field, org.opencdmp.commonmodels.models.description.FieldModel valueField) {
        if (field == null || field.getData() == null) return null;

        switch (field.getData().getFieldType()) {
            case FREE_TEXT, TEXT_AREA, RICH_TEXT_AREA -> {
                if (valueField.getTextValue() != null && !valueField.getTextValue().isBlank()) return valueField.getTextValue();
            }
            case BOOLEAN_DECISION, CHECK_BOX -> {
                if (valueField.getBooleanValue() != null) return valueField.getBooleanValue().toString();
            }
            case DATE_PICKER -> {
                if (valueField.getDateValue() != null) return (DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(valueField.getDateValue()));
            }
            case DATASET_IDENTIFIER, VALIDATION -> {
                if (valueField.getExternalIdentifier() != null && valueField.getExternalIdentifier().getIdentifier() != null && !valueField.getExternalIdentifier().getIdentifier().isBlank()) {
                    return (valueField.getExternalIdentifier().getIdentifier());
                }
            }
            case TAGS -> {
                if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                    return String.join(", ", valueField.getTextListValue());
                }
            }
            case SELECT -> {
                if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                    SelectDataModel selectDataModel = (SelectDataModel)field.getData();
                    if (selectDataModel != null && selectDataModel.getOptions() != null && !selectDataModel.getOptions().isEmpty()){
                        for (SelectDataModel.OptionModel option : selectDataModel.getOptions()){
                            if (valueField.getTextListValue().contains(option.getValue()) || valueField.getTextListValue().contains(option.getLabel()))  return (option.getValue());
                        }
                    }
                }
            }
            case RADIO_BOX -> {
                if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                    RadioBoxDataModel radioBoxModel = (RadioBoxDataModel)field.getData();
                    if (radioBoxModel != null && radioBoxModel.getOptions() != null && !radioBoxModel.getOptions().isEmpty()){
                        for (RadioBoxDataModel.RadioBoxOptionModel option : radioBoxModel.getOptions()){
                            if (valueField.getTextListValue().contains(option.getValue()) || valueField.getTextListValue().contains(option.getLabel()))  return (option.getValue());
                        }
                    }
                }
            }
            case REFERENCE_TYPES -> {
                if (valueField.getReferences() != null && !valueField.getReferences().isEmpty()) {
                    for (ReferenceModel referenceModel : valueField.getReferences()) {
                        if (referenceModel == null
                                || referenceModel.getType() == null || referenceModel.getType().getCode() == null || referenceModel.getType().getCode().isBlank()
                                || referenceModel.getDefinition() == null || referenceModel.getDefinition().getFields() == null || referenceModel.getDefinition().getFields().isEmpty()) continue;
                        if (referenceModel.getReference() != null && !referenceModel.getReference().isBlank()) {
                            return (referenceModel.getReference());
                        }
                    }
                }
            }
        }

        return null;
    }

    private PlanBlueprintValueModel getPlanBlueprintValue(PlanModel plan, UUID id){
        if (plan == null || plan.getProperties() == null || plan.getProperties().getPlanBlueprintValues() == null) return null;
        return plan.getProperties().getPlanBlueprintValues().stream().filter(x-> x.getFieldId().equals(id)).findFirst().orElse(null);
    }

    private List<org.opencdmp.commonmodels.models.planblueprint.FieldModel> getFieldOfSemantic(PlanModel plan, String semanticKey){
        List<org.opencdmp.commonmodels.models.planblueprint.FieldModel> fields = new ArrayList<>();

        if (plan == null || plan.getPlanBlueprint() == null || plan.getPlanBlueprint().getDefinition() == null || plan.getPlanBlueprint().getDefinition().getSections() == null) return fields;
        for (SectionModel sectionModel : plan.getPlanBlueprint().getDefinition().getSections()){
            if (sectionModel.getFields() != null){
                org.opencdmp.commonmodels.models.planblueprint.FieldModel fieldModel = sectionModel.getFields().stream().filter(x-> x.getSemantics() != null && x.getSemantics().contains(semanticKey)).findFirst().orElse(null);
                if (fieldModel != null) fields.add(fieldModel);
            }
        }
        return fields;
    }

    private List<org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel> findSchematicValues(String relatedId, DefinitionModel definitionModel){
        return definitionModel.getAllField().stream().filter(x-> x.getSemantics() != null && x.getSemantics().contains(relatedId)).toList();
    }

    private List<FieldModel> findValueFieldsByIds(String fieldId, PropertyDefinitionModel definitionModel){
        List<FieldModel> models = new ArrayList<>();
        if (definitionModel == null || definitionModel.getFieldSets() == null || definitionModel.getFieldSets().isEmpty()) return models;
        for (PropertyDefinitionFieldSetModel propertyDefinitionFieldSetModel : definitionModel.getFieldSets().values()){
            if (propertyDefinitionFieldSetModel == null ||propertyDefinitionFieldSetModel.getItems() == null || propertyDefinitionFieldSetModel.getItems().isEmpty()) continue;
            for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetModel.getItems()){
                if (propertyDefinitionFieldSetItemModel == null ||propertyDefinitionFieldSetItemModel.getFields() == null || propertyDefinitionFieldSetItemModel.getFields().isEmpty()) continue;
                for (Map.Entry<String, FieldModel> entry : propertyDefinitionFieldSetItemModel.getFields().entrySet()){
                    if (entry == null || entry.getValue() == null) continue;
                    if (entry.getKey().equalsIgnoreCase(fieldId)) models.add(entry.getValue());
                }
            }
        }
        return models;
    }

    private List<ZenodoRelator> buildZenodoRelators(List<org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel> fields, PropertyDefinitionModel propertyDefinition, List<String> acceptedPidTypes, String relatedId) {
        List<ZenodoRelator> relatedIdentifier = new ArrayList<>();
        for (org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel field : fields) {
            if (field.getData() == null) continue;
            List<FieldModel> valueFields = this.findValueFieldsByIds(field.getId(), propertyDefinition);
            ZenodoRelator zenodoRelator = new ZenodoRelator();
            for (FieldModel valueField : valueFields) {
                zenodoRelator.setIdentifier(this.extractSchematicSingleValue(field, valueField));
                switch (field.getData().getFieldType()) {
                    case REFERENCE_TYPES -> {
                        if (valueField.getReferences() != null && !valueField.getReferences().isEmpty()) {
                            for (ReferenceModel referenceModel : valueField.getReferences()) {
                                if (referenceModel == null
                                        || referenceModel.getType() == null || referenceModel.getType().getCode() == null || referenceModel.getType().getCode().isBlank()
                                        || referenceModel.getDefinition() == null || referenceModel.getDefinition().getFields() == null || referenceModel.getDefinition().getFields().isEmpty()) continue;
                                if (referenceModel.getType().getCode().equals(zenodoServiceProperties.getOrganizationReferenceCode()) || referenceModel.getType().getCode().equals(zenodoServiceProperties.getResearcherReferenceCode())) {
                                    if (referenceModel.getReference() != null && !referenceModel.getReference().isBlank()) {
                                        zenodoRelator.setIdentifier(referenceModel.getReference());
                                    }
                                } else {
                                    String pid = referenceModel.getDefinition().getFields().stream().filter(x -> x.getCode() != null && x.getCode().equals(this.pidProperties.getFields().getPidName())).map(ReferenceFieldModel::getValue).findFirst().orElse(null);
                                    String pidType = referenceModel.getDefinition().getFields().stream().filter(x -> x.getCode() != null && x.getCode().equals(this.pidProperties.getFields().getPidTypeName())).map(ReferenceFieldModel::getValue).findFirst().orElse(null);
                                    if (pid != null && !pid.isBlank() && pidType != null && !pidType.isBlank() && acceptedPidTypes.contains(pidType)) {
                                        zenodoRelator.setIdentifier(pid);
                                    }
                                }
                            }
                        }
                    }
                }

                if( zenodoRelator.getIdentifier() != null && !zenodoRelator.getIdentifier().isEmpty()){
                    if(field.getSemantics()!= null) {
                        String scheme = field.getSemantics().stream().filter(this.semanticsProperties.getRelatedIdentifiersScheme() :: contains).findFirst().orElse(null);
                        if(scheme!=null) zenodoRelator.setScheme(scheme.substring(relatedId.toLowerCase().lastIndexOf(".") + 8));
                        else zenodoRelator.setScheme(RELATED_IDENTIFIER_SCHEME_OTHER);
                    }else zenodoRelator.setScheme(RELATED_IDENTIFIER_SCHEME_OTHER);

                    ZenodoRelator.RelationType relationType = new ZenodoRelator.RelationType();
                    relationType.setId(relatedId.toLowerCase().substring(relatedId.lastIndexOf(".") + 1));
                    zenodoRelator.setRelationType(relationType);

                }
                relatedIdentifier.add(zenodoRelator);
            }
        }
        return relatedIdentifier;
    }
    
    private List<ReferenceModel> getReferenceModelOfType(PlanModel plan, String code){
        List<ReferenceModel> response = new ArrayList<>();
        if (plan.getReferences() == null) return response;
        for (PlanReferenceModel planReferenceModel : plan.getReferences()){
            if (planReferenceModel.getReference() != null && planReferenceModel.getReference().getType() != null && planReferenceModel.getReference().getType().getCode() != null  && planReferenceModel.getReference().getType().getCode().equals(code)){
                response.add(planReferenceModel.getReference());
            }
        }
        return response;
    }

    private void applyZenodoRelator(PlanModel plan, ZenodoDeposit deposit) {
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        List<String> acceptedPidTypes = this.pidProperties.getAcceptedTypes();
        List<ZenodoRelator> relatedIdentifiers = new ArrayList<>();
        for(DescriptionModel descriptionModel: plan.getDescriptions()){
            for(String relatedId: this.semanticsProperties.getRelatedIdentifiers()){
                List<org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel> fields = this.findSchematicValues(relatedId, descriptionModel.getDescriptionTemplate().getDefinition());
                relatedIdentifiers.addAll(buildZenodoRelators(fields, descriptionModel.getProperties(), acceptedPidTypes, relatedId));
            }

        }
        deposit.getMetadata().setRelatedIdentifiers(relatedIdentifiers);
    }

    private void applyIsIdenticalTo(PlanModel plan, ZenodoDeposit deposit){
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());
        
        if (plan.getAccessType().equals(PlanAccessType.Public)) {
            ZenodoRelator relator = new ZenodoRelator();
            relator.setIdentifier(zenodoServiceProperties.getDomain() + "explore-plans/overview/public/" + plan.getId().toString());
            relator.setScheme(RELATED_IDENTIFIER_SCHEME_URL);
            ZenodoRelator.RelationType relationType = new ZenodoRelator.RelationType();
            relationType.setId(IS_IDENTICAL_TO);
            relator.setRelationType(relationType);
            if (deposit.getMetadata().getRelatedIdentifiers() == null)deposit.getMetadata().setRelatedIdentifiers(new ArrayList<>());
            deposit.getMetadata().getRelatedIdentifiers().add(relator);
        }
    }

    private void applyLicenses(PlanModel plan, ZenodoDeposit deposit){
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());
        List<ReferenceModel> planLicenses = this.getReferenceModelOfType(plan, zenodoServiceProperties.getLicenceReferenceCode());
        List<ZenodoLicense> licenses = new ArrayList<>();
        if (!planLicenses.isEmpty()) {
            for (ReferenceModel planLicense : planLicenses) {
                if (planLicense != null && planLicense.getReference() != null && !planLicense.getReference().isBlank()) {
                    ZenodoLicense license = new ZenodoLicense();
                    license = this.setLicense(planLicense);

                    licenses.add(license);
                }
            }
            if (deposit.getMetadata().getLicense() == null)deposit.getMetadata().setLicense(new ArrayList<>());

            deposit.getMetadata().getLicense().addAll(licenses);
        }

    }

    private void applyResearchers(PlanModel plan, ZenodoDeposit deposit){
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        List<ZenodoCreator> researchers = new ArrayList<>();
        List<ReferenceModel> planResearchers = this.getReferenceModelOfType(plan, zenodoServiceProperties.getResearcherReferenceCode());
        if (!planResearchers.isEmpty()) {
            for (ReferenceModel researcher : planResearchers) {
                ZenodoCreator contributor = new ZenodoCreator();
                contributor.setRole(new ZenodoCreator.Role(CONTRIBUTOR_TYPE_RESEARCHER));

                ZenodoCreator.PersonOrOrg personOrOrg = new ZenodoCreator.PersonOrOrg();
                personOrOrg.setFamilyName(researcher.getLabel());
                personOrOrg.setGivenName("");
                personOrOrg.setType(PERSON_OR_ORG_TYPE);

                if (researcher.getSource().equalsIgnoreCase(zenodoServiceProperties.getOrcidResearcherSourceCode())) {
                    List<ZenodoCreator.PersonOrOrg.Identifier> identifiers = new ArrayList<>();
                    ZenodoCreator.PersonOrOrg.Identifier identifier = new ZenodoCreator.PersonOrOrg.Identifier();
                    identifier.setScheme("orcid");
                    identifier.setIdentifier(researcher.getReference());
                    identifiers.add(identifier);
                    personOrOrg.setIdentifiers(identifiers);
                }

                contributor.setPersonOrOrg(personOrOrg);

                List<ZenodoCreator.Affiliation> affiliations = new ArrayList<>();
                affiliations.add(new ZenodoCreator.Affiliation(researcher.getSource()));

                contributor.setAffiliations(affiliations);
                researchers.add(contributor);
            }
        }

        if (deposit.getMetadata().getContributors() == null)deposit.getMetadata().setContributors(new ArrayList<>());

        deposit.getMetadata().getContributors().addAll(researchers);
    }

    private void applyFunding(PlanModel plan, ZenodoDeposit deposit){
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());
        List<ReferenceModel> planGrants = this.getReferenceModelOfType(plan, zenodoServiceProperties.getGrantReferenceCode());
        List<ReferenceModel> planFunders = this.getReferenceModelOfType(plan, zenodoServiceProperties.getFunderReferenceCode());

        ZenodoFunding funding = new ZenodoFunding();
        ReferenceModel funderReference = !planFunders.isEmpty() ? planFunders.getFirst(): null;
        if (funderReference != null) {
            List<FunderProperties.DoiFunder> doiFunders = this.funderProperties.getAvailable();

            FunderProperties.DoiFunder doiFunder = doiFunders.stream()
                    .filter(doiFunder1 -> funderReference.getLabel().contains(doiFunder1.getFunder()) || doiFunder1.getFunder().contains(funderReference.getLabel()))
                    .findFirst().orElse(null);

            ZenodoFunding.Funder funder = null;
            if (doiFunder != null) funder = this.getFunderFromDB(doiFunder.getDoi());
            if (funder == null) {
                funder = new ZenodoFunding.Funder();
                funder.setName(funderReference.getLabel());
            }
            funding.setFunder(funder);
        }

        ReferenceModel grantOpenaireReference = planGrants.stream().filter(x-> x.getSource().equalsIgnoreCase(zenodoServiceProperties.getOpenaireGrantSourceCode())).findFirst().orElse(null);
        if (grantOpenaireReference != null) {
            String grantReferenceTail = grantOpenaireReference.getReference().split(":")[2];
            ZenodoFunding.Grant grant = this.getGrantFromDB(grantReferenceTail);
            if (grant == null) {
                grant = new ZenodoFunding.Grant();
                grant.setTitle(grantOpenaireReference.getLabel());
                grant.setNumber(grantOpenaireReference.getReference());
            }
            funding.setGrant(grant);
        } else if (!planGrants.isEmpty() && planGrants.getFirst() != null){
            ZenodoFunding.Grant grant = new ZenodoFunding.Grant();
            grant.setTitle(planGrants.getFirst().getLabel());
            grant.setNumber(planGrants.getFirst().getReference());
            funding.setGrant(grant);
        }

        if (deposit.getMetadata().getFunding() == null)deposit.getMetadata().setFunding(new ArrayList<>());
        deposit.getMetadata().getFunding().add(funding);
    }

    private void applyCreators(PlanModel plan, ZenodoDeposit deposit){
        if (plan.getUsers() == null) return;

        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        if (deposit.getMetadata().getCreators() == null)deposit.getMetadata().setCreators(new ArrayList<>());
        deposit.getMetadata().getCreators().addAll(this.setUsers(plan));
    }

    private List<ZenodoCreator> setUsers(PlanModel plan){

        List<ReferenceModel> planOrganizations = this.getReferenceModelOfType(plan, zenodoServiceProperties.getOrganizationReferenceCode());
        String zenodoAffiliation = zenodoServiceProperties.getAffiliation();

        List<ZenodoCreator> contributors = new ArrayList<>();
        for (PlanUserModel planUser: plan.getUsers()) {

            ZenodoCreator contributor = new ZenodoCreator();
            contributor.setRole(new ZenodoCreator.Role(CONTRIBUTOR_TYPE_PROJECT_MANAGER));

            ZenodoCreator.PersonOrOrg personOrOrg = new ZenodoCreator.PersonOrOrg();
            personOrOrg.setFamilyName(planUser.getUser().getName());
            personOrOrg.setGivenName("");
            personOrOrg.setType(PERSON_OR_ORG_TYPE);

            contributor.setPersonOrOrg(personOrOrg);

            List<ZenodoCreator.Affiliation> affiliations = new ArrayList<>();

            if (!planOrganizations.isEmpty()) {
                for (String label: planOrganizations.stream().map(ReferenceModel::getLabel).toList()) {
                    ZenodoCreator.Affiliation affiliation = new ZenodoCreator.Affiliation(label);
                    if (!affiliations.contains(affiliation)) affiliations.add(affiliation);
                }
            } else {
                if (zenodoAffiliation != null && !zenodoAffiliation.isEmpty()) {
                    affiliations.add(new ZenodoCreator.Affiliation(zenodoAffiliation));
                }
            }
            contributor.setAffiliations(affiliations);
            contributors.add(contributor);

        }
        return contributors;
    }

    private void applySubjects(PlanModel planModel, ZenodoDeposit deposit) {
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        List<ZenodoSubject> subjects = new ArrayList<>();
        for (String value: this.applyListValue(planModel, SEMANTIC_SUBJECT)) {
            ZenodoSubject subject = new ZenodoSubject();
            subject.setSubject(value);
            if (!subjects.contains(subject)) subjects.add(subject);
        }

        if (!subjects.isEmpty()) deposit.getMetadata().setSubjects(subjects);
    }

    private void applyLanguages(PlanModel planModel, ZenodoDeposit deposit) {
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        List<ZenodoLanguage> languages = new ArrayList<>();
        for (String value: this.applyListValue(planModel, SEMANTIC_LANGUAGE)) {
            ULocale locale = new ULocale(value);
            if (locale.getISO3Language().equals(value)) {
                ZenodoLanguage language = new ZenodoLanguage();
                language.setId(value);
                if (!languages.contains(language)) languages.add(language);
            }

        }

        if (!languages.isEmpty()) deposit.getMetadata().setLanguages(languages);
    }

    private void applyReferences(PlanModel planModel, ZenodoDeposit deposit) {
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        List<ZenodoReference> references = new ArrayList<>();
        for (String value: this.applyListValue(planModel, SEMANTIC_REFERENCE)) {
            ZenodoReference reference = new ZenodoReference();
            reference.setReference(value);
            if (!references.contains(reference)) references.add(reference);
        }

        if (!references.isEmpty()) deposit.getMetadata().setReferences(references);
    }

    private void applyCustomFields(PlanModel planModel, ZenodoDeposit deposit) {

        ZenodoDepositCustomFields customFields = new ZenodoDepositCustomFields();
        this.applyPublishingInformation(planModel, customFields);
        this.applyConference(planModel,customFields);
        this.applySoftware(planModel, customFields);

        deposit.setCustomFields(customFields);

    }

    private void applyPublishingInformation(PlanModel planModel, ZenodoDepositCustomFields customFields) {

        ZenodoDepositCustomFields.Journal journal = new ZenodoDepositCustomFields.Journal();

        List<String> journalTitle = this.applyListValue(planModel, SEMANTIC_JOURNAL_TITLE);
        if (!journalTitle.isEmpty()) journal.setTitle(journalTitle.getFirst());

        List<String> journalIssn = this.applyListValue(planModel, SEMANTIC_JOURNAL_ISSN);
        if (!journalIssn.isEmpty()) journal.setIssn(journalIssn.getFirst());

        List<String> journalVolume = this.applyListValue(planModel, SEMANTIC_JOURNAL_VOLUME);
        if (!journalVolume.isEmpty()) journal.setVolume(journalVolume.getFirst());

        List<String> journalIssue = this.applyListValue(planModel, SEMANTIC_JOURNAL_ISSUE);
        if (!journalIssue.isEmpty()) journal.setIssue(journalIssue.getFirst());

        List<String> journalPage = this.applyListValue(planModel, SEMANTIC_JOURNAL_PAGE_RANGE);
        if (!journalPage.isEmpty()) journal.setPages(journalPage.getFirst());

        customFields.setJournal(journal);

        ZenodoDepositCustomFields.Imprint imprint = new ZenodoDepositCustomFields.Imprint();

        List<String> imprintTitle = this.applyListValue(planModel, SEMANTIC_IMPRINT_TITLE);
        if (!imprintTitle.isEmpty()) imprint.setTitle(imprintTitle.getFirst());

        List<String> imprintIsbn = this.applyListValue(planModel, SEMANTIC_IMPRINT_ISBN);
        if (!imprintIsbn.isEmpty()) imprint.setIsbn(imprintIsbn.getFirst());

        List<String> imprintPlace = this.applyListValue(planModel, SEMANTIC_IMPRINT_PLACE);
        if (!imprintPlace.isEmpty()) imprint.setPlace(imprintPlace.getFirst());

        List<String> imprintPagination = this.applyListValue(planModel, SEMANTIC_IMPRINT_PAGINATION);
        if (!imprintPagination.isEmpty()) imprint.setPages(imprintPagination.getFirst());

        customFields.setImprint(imprint);

        List<String> thesisUniversity = this.applyListValue(planModel, SEMANTIC_THESIS_UNIVERSITY);
        if (!thesisUniversity.isEmpty()) customFields.setThesisUniversity(thesisUniversity.getFirst());
    }

    private List<String> applyListValue(PlanModel planModel, String semanticCode) {
        List<String> fields = new ArrayList<>();

        //plan blueprint semantics
        List<org.opencdmp.commonmodels.models.planblueprint.FieldModel> blueprintFieldsWithSemantic = this.getFieldOfSemantic(planModel, semanticCode);
        for (org.opencdmp.commonmodels.models.planblueprint.FieldModel field: blueprintFieldsWithSemantic) {
            PlanBlueprintValueModel planBlueprintValueModel = this.getPlanBlueprintValue(planModel, field.getId());
            if (planBlueprintValueModel != null) {
                if (planBlueprintValueModel.getValue() != null && !planBlueprintValueModel.getValue().isBlank()) fields.add(planBlueprintValueModel.getValue());
                else if (planBlueprintValueModel.getNumberValue() != null) fields.add(planBlueprintValueModel.getNumberValue().toString());
                else if (planBlueprintValueModel.getDateValue() != null) fields.add(DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(planBlueprintValueModel.getDateValue()));
            }
        }

        //description template
        for (DescriptionModel descriptionModel: planModel.getDescriptions()) {
            List<org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel> fieldsWithSemantics = this.findSchematicValues(semanticCode, descriptionModel.getDescriptionTemplate().getDefinition());
            Set<String> values = extractSchematicValues(fieldsWithSemantics, descriptionModel.getProperties());
            //description tags from semantic
            for (String value: values){
                if (!fields.contains(value)) fields.add(value);
            }
        }

        return fields;
    }

    private void applyAdditionalTitle(PlanModel planModel, ZenodoDeposit deposit) {
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        List<ZenodoAdditionalTitle> additionalTitles = new ArrayList<>();
        if (planModel.getDescriptions() != null) {
            for (DescriptionModel descriptionModel : planModel.getDescriptions()) {
                for (FieldSetModel fieldSet : this.templateFieldSearcherService.searchFieldSetsBySemantics(descriptionModel.getDescriptionTemplate(), List.of(SEMANTIC_ADDITIONAL_TITLE, SEMANTIC_ADDITIONAL_TITLE_TYPE, SEMANTIC_ADDITIONAL_TITLE_LANGUAGE))) {

                    List<org.opencdmp.commonmodels.models.description.PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetValue(fieldSet, descriptionModel.getProperties());
                    for (org.opencdmp.commonmodels.models.description.PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {

                        ZenodoAdditionalTitle additionalTitle = new ZenodoAdditionalTitle();
                        ZenodoAdditionalTitle.Type type = new ZenodoAdditionalTitle.Type();

                        FieldModel fieldValue = this.findValueFieldBySemantic(fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_ADDITIONAL_TITLE);
                        if (fieldValue != null) {
                            FieldModel finalFieldValue = fieldValue;
                            String value = this.extractSchematicSingleValue(descriptionModel.getDescriptionTemplate().getDefinition().getAllField().stream().filter(x-> x.getId().equals(finalFieldValue.getId())).findFirst().orElse(null), fieldValue);
                            if (value != null) additionalTitle.setTitle(value);
                        }

                        fieldValue = this.findValueFieldBySemantic(fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_ADDITIONAL_TITLE_TYPE);
                        if (fieldValue != null) {
                            FieldModel finalFieldValue1 = fieldValue;
                            String value = this.extractSchematicSingleValue(descriptionModel.getDescriptionTemplate().getDefinition().getAllField().stream().filter(x-> x.getId().equals(finalFieldValue1.getId())).findFirst().orElse(null), fieldValue);
                            if (value != null) {
                                if (Arrays.stream(ZenodoAdditionalTitleIdType.values()).toList().stream().map(ZenodoAdditionalTitleIdType::getValue).toList().contains(value)) {
                                    type.setId(ZenodoAdditionalTitleIdType.of(value));
                                } else {
                                    type.setId(ZenodoAdditionalTitleIdType.OTHER);
                                }
                                additionalTitle.setType(type);
                            }
                        } else {
                            type.setId(ZenodoAdditionalTitleIdType.OTHER);
                            additionalTitle.setType(type);
                        }

                        fieldValue = this.findValueFieldBySemantic(fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_ADDITIONAL_TITLE_LANGUAGE);
                        if (fieldValue != null) {
                            FieldModel finalFieldValue = fieldValue;
                            String value = this.extractSchematicSingleValue(descriptionModel.getDescriptionTemplate().getDefinition().getAllField().stream().filter(x-> x.getId().equals(finalFieldValue.getId())).findFirst().orElse(null), fieldValue);
                            if (value != null) {
                                ULocale locale = new ULocale(value);
                                if (locale.getISO3Language().equals(value)) {
                                    ZenodoAdditionalTitle.Language language = new ZenodoAdditionalTitle.Language();
                                    language.setId(value);
                                    additionalTitle.setLang(language);
                                }
                            }
                        }

                        additionalTitles.add(additionalTitle);
                    }
                }
            }
        }

        if (!additionalTitles.isEmpty()) deposit.getMetadata().setAdditionalTitles(additionalTitles);
    }

    private void applySoftware(PlanModel planModel, ZenodoDepositCustomFields customFields){
            List<String> availableLanguages = this.programmingLanguagesProperties.getProgrammingCodes();

            List<String> availableStatus = this.programmingLanguagesProperties.getDevelopmentStatus();

            List<String> softwareURL = this.applyListValue(planModel, SEMANTIC_SOFTWARE_REPOSITORY_URL);
            if(!softwareURL.isEmpty() && isValidUrl(softwareURL.getFirst())) customFields.setCodeRepository(softwareURL.getFirst());

            List<String> softwareLanguage = this.applyListValue(planModel, SEMANTIC_SOFTWARE_PROGRAMMING_LANGUAGE);
            if(!softwareLanguage.isEmpty()){
                List<ZenodoDepositCustomFields.Code> programmingLanguage = new ArrayList<>();
                for(String language : softwareLanguage){
                    if(availableLanguages.contains(language)){
                        ZenodoDepositCustomFields.Code code = new ZenodoDepositCustomFields.Code();
                        code.setId(language);

                        programmingLanguage.add(code);
                    }
                }
                customFields.setCodeProgrammingLanguage(programmingLanguage);
            }

            List<String> softwareStatus = this.applyListValue(planModel, SEMANTIC_SOFTWARE_DEVELOPMENT_STATUS);
            if(!softwareStatus.isEmpty()){
                if(availableStatus.contains(softwareStatus.getFirst())){
                    ZenodoDepositCustomFields.Code code = new ZenodoDepositCustomFields.Code();
                    code.setId(softwareStatus.getFirst());

                    customFields.setCodeDevelopmentStatus(code);
                }
            }

    }

    private void applyConference(PlanModel planModel, ZenodoDepositCustomFields customFields){

        if(customFields.getConference() == null) customFields.setConference(new ZenodoDepositCustomFields.Conference());

        ZenodoDepositCustomFields.Conference conference = new ZenodoDepositCustomFields.Conference();

        List<String> conferenceTitle = this.applyListValue(planModel, SEMANTIC_CONFERENCE_TITLE);
        if (!conferenceTitle.isEmpty()) conference.setTitle(conferenceTitle.getFirst());

        List<String> conferenceAcronym = this.applyListValue(planModel, SEMANTIC_CONFERENCE_ACRONYM);
        if (!conferenceAcronym.isEmpty()) conference.setAcronym(conferenceAcronym.getFirst());

        List<String> conferencePlace = this.applyListValue(planModel, SEMANTIC_CONFERENCE_PLACE);
        if (!conferencePlace.isEmpty()) conference.setPlace(conferencePlace.getFirst());

        List<String> conferenceDates = this.applyListValue(planModel, SEMANTIC_CONFERENCE_DATES);
        if (!conferenceDates.isEmpty()) conference.setDates(conferenceDates.getFirst());

        List<String> conferenceWebsite = this.applyListValue(planModel, SEMANTIC_CONFERENCE_WEBSITE);
        if (!conferenceWebsite.isEmpty() && isValidUrl(conferenceWebsite.getFirst())) conference.setUrl(conferenceWebsite.getFirst());

        List<String> conferenceSession = this.applyListValue(planModel, SEMANTIC_CONFERENCE_SESSION);
        if (!conferenceSession.isEmpty()) conference.setSession(conferenceSession.getFirst());

        List<String> conferencePart = this.applyListValue(planModel, SEMANTIC_CONFERENCE_PART);
        if (!conferencePart.isEmpty()) conference.setSessionPart(conferencePart.getFirst());


        customFields.setConference(conference);

    }

    private boolean isValidUrl(String url){
        try {
            new URL(url).toURI();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    private void applyDates(PlanModel planModel, ZenodoDeposit deposit){
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        List<String> acceptedTypes = this.semanticsProperties.getDateTypes();

        List<ZenodoDates> dates = new ArrayList<>();
        for(String acceptedType : acceptedTypes){
            List<String> results = this.applyListValue(planModel, acceptedType);
            for(String dateValue : results){
                ZenodoDates date = new ZenodoDates();
                String validDate = checkValidDate(dateValue);
                if(validDate != null) {
                    ZenodoDates.Type type = new ZenodoDates.Type();
                    type.setId(acceptedType.toLowerCase().substring(acceptedType.lastIndexOf(".") + 1));
                    date.setDate(validDate);
                    date.setType(type);

                    dates.add(date);
                }
            }

        }
        if (!dates.isEmpty()) deposit.getMetadata().setDates(dates);
    }

    private void applyIdentifiers(PlanModel planModel, ZenodoDeposit deposit){
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        List<String> acceptedSchemes = this.semanticsProperties.getAlternateIdentifiersScheme();
        List<ZenodoAlternateIdentifiers> alternateIdentifiers = new ArrayList<>();

        for(String scheme : acceptedSchemes){
            List<String> results = this.applyListValue(planModel, scheme);
            for(String IdentifierValue : results){

                ZenodoAlternateIdentifiers alternateIdentifier = new ZenodoAlternateIdentifiers();
                alternateIdentifier.setIdentifier(IdentifierValue);
                alternateIdentifier.setScheme(scheme.toLowerCase().substring(scheme.lastIndexOf(".") + 1));

                alternateIdentifiers.add(alternateIdentifier);
            }
        }
        if (!alternateIdentifiers.isEmpty()) deposit.getMetadata().setAlternateIdentifiers(alternateIdentifiers);
    }


    private String checkValidDate(String dateStr) {
        ///TODO Make the input more resilient to crashes. Convert the string Time to correct. For example if its 12/12/2024 to make it to 2024-12-12
        try{
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            try{
                return LocalDate.parse(dateStr, formatter).toString();

            }catch (Exception e){
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                LocalDate date = LocalDate.parse(dateStr, inputFormatter);
                return date.format(outputFormatter);
            }
        }catch(Exception e){
            logger.warn("invalid rda project parse date for value: " + dateStr);
            return null;
        }
    }


    private ZenodoFunding.Funder getFunderFromDB(String identifier){

        if (identifier == null) return null;

        ResponseEntity<List<Map>> funderResponse = this.getWebClient().get().uri(this.zenodoServiceProperties.getDepositConfiguration().getRepositoryUrl() + "funders?q=identifiers.identifier:\"" + identifier + "\"").retrieve().toEntityList(Map.class).block();

        try {
            if (funderResponse != null) {
                Object result = ((ArrayList<?>) ((LinkedHashMap<?, ?>) Objects.requireNonNull(funderResponse.getBody()).getFirst().get("hits")).get("hits")).getFirst();
                if (result instanceof LinkedHashMap<?, ?>) {
                    ZenodoFunding.Funder funder = new ZenodoFunding.Funder();
                    LinkedHashMap resultFunder = (LinkedHashMap) result;
                    if (resultFunder.containsKey("id") && resultFunder.get("id") != null && resultFunder.get("id") instanceof String) {
                        funder.setId(resultFunder.get("id").toString());
                        return funder;
                    }
                }
            }
        } catch (Exception e) {
        }

        return null;
    }

    private ZenodoLicense setLicense(ReferenceModel identifier) {
        if (identifier == null) return null;
        ZenodoLicense license = new ZenodoLicense();

        try {
            ResponseEntity<List<Map>> licenseResponse = this.getWebClient().get().uri(this.zenodoServiceProperties.getDepositConfiguration().getRepositoryUrl() + "vocabularies/licenses/" + identifier.getReference().toLowerCase()).retrieve().toEntityList(Map.class).block();

            if (licenseResponse == null) return null;

            Map resultLicense = licenseResponse.getBody().get(0);


            if (resultLicense.containsKey("id") && resultLicense.get("id") instanceof String) {
                license.setId(resultLicense.get("id").toString());
            }


        return license;
        } catch (Exception e) {
            System.err.println("Error fetching license: " + e.getMessage());
            ZenodoLicense.Title title = new ZenodoLicense.Title();
            title.setEn(identifier.getReference());

            ZenodoLicense.Description description = new ZenodoLicense.Description();
            description.setEn(identifier.getDescription());
            license.setDescription(description);
            license.setTitle(title);
            return license;
        }
    }

    private ZenodoFunding.Grant getGrantFromDB(String number){

        if (number == null) return null;

        ResponseEntity<List<Map>> grantResponse = this.getWebClient().get().uri(this.zenodoServiceProperties.getDepositConfiguration().getRepositoryUrl() + "awards?q=number:\"" + number + "\"").retrieve().toEntityList(Map.class).block();

        try {
            if (grantResponse != null) {
                Object result = ((ArrayList<?>) ((LinkedHashMap<?, ?>) Objects.requireNonNull(grantResponse.getBody()).getFirst().get("hits")).get("hits")).getFirst();
                if (result instanceof LinkedHashMap<?, ?>) {
                    ZenodoFunding.Grant grant = new ZenodoFunding.Grant();
                    LinkedHashMap resultGrant = (LinkedHashMap) result;
                    if (resultGrant.containsKey("id") && resultGrant.get("id") != null && resultGrant.get("id") instanceof String) {
                        grant.setId(resultGrant.get("id").toString());
                        return grant;
                    }
                }
            }
        } catch (Exception e) {
        }

        return null;
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

    private org.opencdmp.commonmodels.models.description.FieldModel findValueFieldBySemantic(FieldSetModel fieldSet, org.opencdmp.commonmodels.models.description.PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel, String semantic){
        org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel field = this.templateFieldSearcherService.findFieldBySemantic(fieldSet, semantic);
        return field != null ? propertyDefinitionFieldSetItemModel.getFields().getOrDefault(field.getId(), null) : null;
    }

    private List<org.opencdmp.commonmodels.models.description.PropertyDefinitionFieldSetItemModel> findFieldSetValue(FieldSetModel fieldSetModel, PropertyDefinitionModel descriptionTemplateModel){
        List<org.opencdmp.commonmodels.models.description.PropertyDefinitionFieldSetItemModel> items = new ArrayList<>();
        if (fieldSetModel == null || descriptionTemplateModel == null || descriptionTemplateModel.getFieldSets() == null) return items;
        PropertyDefinitionFieldSetModel propertyDefinitionFieldSetModel =  descriptionTemplateModel.getFieldSets().getOrDefault(fieldSetModel.getId(), null);
        if (propertyDefinitionFieldSetModel != null && propertyDefinitionFieldSetModel.getItems() != null) return propertyDefinitionFieldSetModel.getItems();
        return items;
    }

}

