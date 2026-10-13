package org.opencdmp.deposit.zenodorepository.model.builder;

import com.ibm.icu.util.ULocale;
import gr.cite.tools.logging.LoggerService;
import gr.cite.tools.logging.MapLogEntry;
import org.opencdmp.commonmodels.enums.PlanAccessType;
import org.opencdmp.commonmodels.models.PlanUserModel;
import org.opencdmp.commonmodels.models.description.*;
import org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldSetModel;
import org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.fielddata.RadioBoxDataModel;
import org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.fielddata.SelectDataModel;
import org.opencdmp.commonmodels.models.plan.PlanModel;
import org.opencdmp.commonmodels.models.plandescriptionfields.FieldModel;
import org.opencdmp.commonmodels.models.plandescriptionfields.PropertyDefinitionFieldSetItemModel;
import org.opencdmp.commonmodels.models.plandescriptionfields.PropertyDefinitionFieldSetModel;
import org.opencdmp.commonmodels.models.planreference.PlanReferenceModel;
import org.opencdmp.commonmodels.models.reference.ReferenceFieldModel;
import org.opencdmp.commonmodels.models.reference.ReferenceModel;
import org.opencdmp.commonmodels.models.referencetype.ReferenceTypeFieldModel;
import org.opencdmp.deposit.zenodorepository.configuration.funder.FunderProperties;
import org.opencdmp.deposit.zenodorepository.configuration.pid.PidProperties;
import org.opencdmp.deposit.zenodorepository.configuration.programminglanguages.ProgrammingLanguagesProperties;
import org.opencdmp.deposit.zenodorepository.configuration.semantics.SemanticsProperties;
import org.opencdmp.deposit.zenodorepository.enums.ZenodoAccessRight;
import org.opencdmp.deposit.zenodorepository.enums.ZenodoAdditionalTitleIdType;
import org.opencdmp.deposit.zenodorepository.model.*;
import org.opencdmp.deposit.zenodorepository.service.descriptiontemplatesearcher.TemplateFieldSearcherService;
import org.opencdmp.deposit.zenodorepository.service.planblueprintsearcher.PlanBlueprintFieldSearcherService;
import org.opencdmp.deposit.zenodorepository.service.zenodo.ZenodoDepositServiceImpl;
import org.opencdmp.deposit.zenodorepository.service.zenodo.ZenodoServiceProperties;
import org.opencdmp.depositbase.repository.DepositPluginSettings;
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

    private static final String SETTING_COMMUNITY = "community";
    private static final String SETTING_AFFILIATION = "affiliation";
    private static final String SETTING_PUBLISHER = "publisher";
    private static final String SETTING_RESOURCE_TYPE = "resource-type";
    private static final String SETTING_DEFAULT_LICENCE = "default-licence";
    private static final String SETTING_COPYRIGHT = "copyright";
    private static final String SETTING_DEFAULT_LANGUAGE = "default-language";
    private static final String SETTING_LICENCES_TYPE = "reference-type-licences";
    private static final String SETTING_FUNDERS_TYPE = "reference-type-funders";
    private static final String SETTING_GRANTS_TYPE = "reference-type-grants";
    private static final String SETTING_ORGANISATIONS_TYPE = "reference-type-organisations";
    private static final String SETTING_RESEARCHERS_TYPE = "reference-type-researchers";

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

    private static final String SEMANTIC_LICENSE_LINK = "zenodo.metadata.rights.link";

    private static final String SEMANTIC_CONTRIBUTORS_GIVEN_NAME = "zenodo.metadata.contributors.person_or_org.given_name";
    private static final String SEMANTIC_CONTRIBUTORS_FAMILY_NAME = "zenodo.metadata.contributors.person_or_org.family_name";

    private final PlanBlueprintFieldSearcherService blueprintFieldSearcherService;
    private final TemplateFieldSearcherService templateFieldSearcherService;
    private final PidProperties pidProperties;
    private final SemanticsProperties semanticsProperties;
    private final FunderProperties funderProperties;
    private final ZenodoServiceProperties zenodoServiceProperties;
    private final ProgrammingLanguagesProperties programmingLanguagesProperties;

    @Autowired
    public ZenodoBuilder(PlanBlueprintFieldSearcherService blueprintFieldSearcherService, TemplateFieldSearcherService templateFieldSearcherService, PidProperties pidProperties, SemanticsProperties semanticsProperties, FunderProperties funderProperties, ZenodoServiceProperties zenodoServiceProperties, ProgrammingLanguagesProperties programmingLanguagesProperties){
        this.blueprintFieldSearcherService = blueprintFieldSearcherService;
        this.templateFieldSearcherService = templateFieldSearcherService;
        this.pidProperties = pidProperties;
        this.semanticsProperties = semanticsProperties;
        this.funderProperties = funderProperties;
	    this.zenodoServiceProperties = zenodoServiceProperties;
        this.programmingLanguagesProperties = programmingLanguagesProperties;
    }

    public String getCommunity(PlanModel planModel){
        String fallback = this.settings(planModel).text(SETTING_COMMUNITY, zenodoServiceProperties.getCommunity());
        if (planModel == null) return fallback;

        String semantic = SEMANTIC_COMMUNITY;
        List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fieldsWithSemantics = this.findSchematicValues(semantic, planModel.getPlanBlueprint().getDefinition().getAllField());
        Set<String> values = extractSchematicValues(fieldsWithSemantics, planModel.getProperties().getFieldSets());
        String value = values.stream().findFirst().orElse(null);
        if (value != null) return value;

        //description template
        for (DescriptionModel descriptionModel: planModel.getDescriptions()) {
            fieldsWithSemantics = this.findSchematicValues(semantic, descriptionModel.getDescriptionTemplate().getDefinition().getAllField());
            values = extractSchematicValues(fieldsWithSemantics, descriptionModel.getProperties().getFieldSets());
            value = values.stream().findFirst().orElse(null);
            if (value != null) return value;
        }

        return fallback;
    }

    private DepositPluginSettings settings(PlanModel planModel) {
        return DepositPluginSettings.of(planModel, this.zenodoServiceProperties.getDepositConfiguration());
    }

    public ZenodoDeposit build(PlanModel plan) {
        ZenodoDeposit deposit = new ZenodoDeposit();
        DepositPluginSettings settings = this.settings(plan);

        this.applyAccess(plan, deposit);
        this.applyZenodoRelator(plan, deposit, settings);

        deposit.getMetadata().setTitle(plan.getLabel());
        deposit.getMetadata().setResourceType(new ZenodoResourceType(settings.text(SETTING_RESOURCE_TYPE, this.zenodoServiceProperties.getResourceTypeId())));
        deposit.getMetadata().setPublisher(settings.text(SETTING_PUBLISHER, this.zenodoServiceProperties.getPublisherName()));
        deposit.getMetadata().setPublicationDate(DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(Instant.now()));
        deposit.getMetadata().setDescription((plan.getDescription() != null && !plan.getDescription().isEmpty() ? plan.getDescription() : "<p></p>"));
        deposit.getMetadata().setVersion(String.valueOf(plan.getVersion()));

        for (String value: this.applyListValue(plan, SEMANTIC_PUBLICATION_DATE)) {
            String publicationDate = this.parsePublicationDate(value);
            if (publicationDate != null) {
                deposit.getMetadata().setPublicationDate(publicationDate);
                break;
            }
        }

        if (settings.linkPublicPlan()) this.applyIsIdenticalTo(plan, deposit);
        this.applyLicenses(plan, deposit, settings);
        this.applyResearchers(plan, deposit, settings);
        this.applyFunding(plan, deposit, settings);
        this.applyCreators(plan, deposit, settings);
        this.applyLanguages(plan, deposit, settings.text(SETTING_DEFAULT_LANGUAGE));
        deposit.getMetadata().setCopyright(settings.text(SETTING_COPYRIGHT));
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

    private Set<String> extractSchematicValues(List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fields, Map<String, PropertyDefinitionFieldSetModel> fieldSets) {
        Set<String> values = new LinkedHashSet<>();
        for (org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field : fields) {
            if (field.getData() == null) continue;
            List<FieldModel> valueFields = this.findValueFieldsByIds(field.getId(), fieldSets);
            for (FieldModel valueField : valueFields) {
                switch (field.getData().getFieldType()) {
                    case FREE_TEXT, TEXT_AREA, RICH_TEXT_AREA -> {
                        if (valueField.getTextValue() != null && !valueField.getTextValue().isBlank()) values.add(valueField.getTextValue());
                    }
                    case NUMBER -> {
                        if (valueField.getNumberValue() != null) values.add(String.valueOf(valueField.getNumberValue()));
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
                                    if (valueField.getTextListValue().contains(option.getValue()) || valueField.getTextListValue().contains(option.getLabel())) {
                                        String optionValue = this.optionValueOrLabel(option.getValue(), option.getLabel());
                                        if (optionValue != null) values.add(optionValue);
                                    }
                                }
                            }
                        }
                    }
                    case RADIO_BOX -> {
                        if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                            RadioBoxDataModel radioBoxModel = (RadioBoxDataModel)field.getData();
                            if (radioBoxModel != null && radioBoxModel.getOptions() != null && !radioBoxModel.getOptions().isEmpty()){
                                for (RadioBoxDataModel.RadioBoxOptionModel option : radioBoxModel.getOptions()){
                                    if (valueField.getTextListValue().contains(option.getValue()) || valueField.getTextListValue().contains(option.getLabel())) {
                                        String optionValue = this.optionValueOrLabel(option.getValue(), option.getLabel());
                                        if (optionValue != null) values.add(optionValue);
                                    }
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

    private String extractSchematicSingleValue(org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field, org.opencdmp.commonmodels.models.plandescriptionfields.FieldModel valueField) {
        if (field == null || field.getData() == null) return null;

        switch (field.getData().getFieldType()) {
            case FREE_TEXT, TEXT_AREA, RICH_TEXT_AREA -> {
                if (valueField.getTextValue() != null && !valueField.getTextValue().isBlank()) return valueField.getTextValue();
            }
            case NUMBER -> {
                if (valueField.getNumberValue() != null) return String.valueOf(valueField.getNumberValue());
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
                            if (valueField.getTextListValue().contains(option.getValue()) || valueField.getTextListValue().contains(option.getLabel()))  return this.optionValueOrLabel(option.getValue(), option.getLabel());
                        }
                    }
                }
            }
            case RADIO_BOX -> {
                if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                    RadioBoxDataModel radioBoxModel = (RadioBoxDataModel)field.getData();
                    if (radioBoxModel != null && radioBoxModel.getOptions() != null && !radioBoxModel.getOptions().isEmpty()){
                        for (RadioBoxDataModel.RadioBoxOptionModel option : radioBoxModel.getOptions()){
                            if (valueField.getTextListValue().contains(option.getValue()) || valueField.getTextListValue().contains(option.getLabel()))  return this.optionValueOrLabel(option.getValue(), option.getLabel());
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

    private List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> findSchematicValues(String relatedId, List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fields){
        return fields.stream().filter(x-> x.getSemantics() != null && x.getSemantics().contains(relatedId)).toList();
    }

    private List<FieldModel> findValueFieldsByIds(String fieldId, Map<String, PropertyDefinitionFieldSetModel> fieldSets){
        List<FieldModel> models = new ArrayList<>();
        if (fieldSets == null || fieldSets.isEmpty()) return models;
        for (PropertyDefinitionFieldSetModel propertyDefinitionFieldSetModel : fieldSets.values()){
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

    private List<ZenodoRelator> buildZenodoRelators(List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fields, Map<String, PropertyDefinitionFieldSetModel> fieldSets, List<String> acceptedPidTypes, String relatedId, DepositPluginSettings settings) {
        List<ZenodoRelator> relatedIdentifier = new ArrayList<>();
        String organisationsType = settings.text(SETTING_ORGANISATIONS_TYPE, zenodoServiceProperties.getOrganizationReferenceCode());
        String researchersType = settings.text(SETTING_RESEARCHERS_TYPE, zenodoServiceProperties.getResearcherReferenceCode());
        for (org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field : fields) {
            if (field.getData() == null) continue;
            List<FieldModel> valueFields = this.findValueFieldsByIds(field.getId(), fieldSets);
            for (FieldModel valueField : valueFields) {
                // one relator per answer, so every answer keeps its own identifier
                ZenodoRelator zenodoRelator = new ZenodoRelator();
                zenodoRelator.setIdentifier(this.extractSchematicSingleValue(field, valueField));
                switch (field.getData().getFieldType()) {
                    case REFERENCE_TYPES -> {
                        if (valueField.getReferences() != null && !valueField.getReferences().isEmpty()) {
                            for (ReferenceModel referenceModel : valueField.getReferences()) {
                                if (referenceModel == null
                                        || referenceModel.getType() == null || referenceModel.getType().getCode() == null || referenceModel.getType().getCode().isBlank()
                                        || referenceModel.getDefinition() == null || referenceModel.getDefinition().getFields() == null || referenceModel.getDefinition().getFields().isEmpty()) continue;
                                if (referenceModel.getType().getCode().equals(organisationsType) || referenceModel.getType().getCode().equals(researchersType)) {
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

                if (zenodoRelator.getIdentifier() == null || zenodoRelator.getIdentifier().isBlank()) continue;

                String scheme = field.getSemantics() != null && this.semanticsProperties.getRelatedIdentifiersScheme() != null
                        ? field.getSemantics().stream().filter(this.semanticsProperties.getRelatedIdentifiersScheme() :: contains).findFirst().orElse(null)
                        : null;
                // the scheme is the last part of the scheme key, e.g. zenodo.related_identifiers.scheme.doi -> doi
                if (scheme != null && scheme.lastIndexOf(".") < scheme.length() - 1) zenodoRelator.setScheme(scheme.substring(scheme.lastIndexOf(".") + 1).toLowerCase());
                else zenodoRelator.setScheme(RELATED_IDENTIFIER_SCHEME_OTHER);

                ZenodoRelator.RelationType relationType = new ZenodoRelator.RelationType();
                relationType.setId(relatedId.toLowerCase().substring(relatedId.lastIndexOf(".") + 1));
                zenodoRelator.setRelationType(relationType);

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

    private void applyZenodoRelator(PlanModel plan, ZenodoDeposit deposit, DepositPluginSettings settings) {
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        List<String> acceptedPidTypes = this.pidProperties.getAcceptedTypes();
        List<ZenodoRelator> relatedIdentifiers = new ArrayList<>();

        //plan
        for(String relatedId: this.semanticsProperties.getRelatedIdentifiers()){
            List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fields = this.findSchematicValues(relatedId, plan.getPlanBlueprint().getDefinition().getAllField());
            relatedIdentifiers.addAll(buildZenodoRelators(fields, plan.getProperties().getFieldSets(), acceptedPidTypes, relatedId, settings));
        }

        //description
        for(DescriptionModel descriptionModel: plan.getDescriptions()){
            for(String relatedId: this.semanticsProperties.getRelatedIdentifiers()){
                List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fields = this.findSchematicValues(relatedId, descriptionModel.getDescriptionTemplate().getDefinition().getAllField());
                relatedIdentifiers.addAll(buildZenodoRelators(fields, descriptionModel.getProperties().getFieldSets(), acceptedPidTypes, relatedId, settings));
            }

        }
        deposit.getMetadata().setRelatedIdentifiers(relatedIdentifiers);
    }

    private void applyIsIdenticalTo(PlanModel plan, ZenodoDeposit deposit){
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());
        
        if (PlanAccessType.Public.equals(plan.getAccessType())) {
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

    private void applyLicenses(PlanModel plan, ZenodoDeposit deposit, DepositPluginSettings settings){
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());
        List<ReferenceModel> planLicenses = this.getReferenceModelOfType(plan, settings.text(SETTING_LICENCES_TYPE, zenodoServiceProperties.getLicenceReferenceCode()));
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

        String defaultLicence = settings.text(SETTING_DEFAULT_LICENCE);
        if ((deposit.getMetadata().getLicense() == null || deposit.getMetadata().getLicense().isEmpty()) && defaultLicence != null) {
            ZenodoLicense license = new ZenodoLicense();
            license.setId(defaultLicence.toLowerCase());
            deposit.getMetadata().setLicense(new ArrayList<>(List.of(license)));
        }
    }

    private void applyResearchers(PlanModel plan, ZenodoDeposit deposit, DepositPluginSettings settings){
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        List<ZenodoCreator> researchers = new ArrayList<>();
        List<ReferenceModel> planResearchers = this.getReferenceModelOfType(plan, settings.text(SETTING_RESEARCHERS_TYPE, zenodoServiceProperties.getResearcherReferenceCode()));
        if (!planResearchers.isEmpty()) {
            for (ReferenceModel researcher : planResearchers) {
                ZenodoCreator contributor = new ZenodoCreator();
                contributor.setRole(new ZenodoCreator.Role(CONTRIBUTOR_TYPE_RESEARCHER));

                ZenodoCreator.PersonOrOrg personOrOrg = new ZenodoCreator.PersonOrOrg();
                personOrOrg.setName(researcher.getLabel());

                personOrOrg.setFamilyName(this.getReferenceFieldValueFromSemantic(researcher, SEMANTIC_CONTRIBUTORS_FAMILY_NAME));
                if (personOrOrg.getFamilyName() == null || personOrOrg.getFamilyName().isBlank()) personOrOrg.setFamilyName(researcher.getLabel());

                personOrOrg.setGivenName(this.getReferenceFieldValueFromSemantic(researcher, SEMANTIC_CONTRIBUTORS_GIVEN_NAME));
                if (personOrOrg.getGivenName() == null || personOrOrg.getGivenName().isBlank()) personOrOrg.setGivenName("");

                personOrOrg.setType(PERSON_OR_ORG_TYPE);

                if (researcher.getSource() != null && researcher.getSource().equalsIgnoreCase(zenodoServiceProperties.getOrcidResearcherSourceCode())) {
                    List<ZenodoCreator.PersonOrOrg.Identifier> identifiers = new ArrayList<>();
                    ZenodoCreator.PersonOrOrg.Identifier identifier = new ZenodoCreator.PersonOrOrg.Identifier();
                    identifier.setScheme("orcid");
                    identifier.setIdentifier(researcher.getReference());
                    identifiers.add(identifier);
                    personOrOrg.setIdentifiers(identifiers);
                }

                // no affiliation: the reference only knows its source (such as ORCID), which is not one
                contributor.setPersonOrOrg(personOrOrg);
                researchers.add(contributor);
            }
        }

        if (deposit.getMetadata().getContributors() == null)deposit.getMetadata().setContributors(new ArrayList<>());

        deposit.getMetadata().getContributors().addAll(researchers);
    }

    private void applyFunding(PlanModel plan, ZenodoDeposit deposit, DepositPluginSettings settings){
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());
        List<ReferenceModel> planGrants = this.getReferenceModelOfType(plan, settings.text(SETTING_GRANTS_TYPE, zenodoServiceProperties.getGrantReferenceCode()));
        List<ReferenceModel> planFunders = this.getReferenceModelOfType(plan, settings.text(SETTING_FUNDERS_TYPE, zenodoServiceProperties.getFunderReferenceCode()));

        List<ZenodoFunding.Funder> funders = new ArrayList<>();
        for (ReferenceModel funderReference : planFunders) {
            if (funderReference == null || funderReference.getLabel() == null) continue;
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
            funders.add(funder);
        }

        // the plan does not link grants to funders: grants go with the first funder, as before, and other funders on their own
        List<ZenodoFunding> fundingList = new ArrayList<>();
        for (ReferenceModel grantReference : planGrants) {
            if (grantReference == null) continue;
            //TODO akis uncomment this, with the part after the second ':' of an openaire-sourced grant's reference
//            ZenodoFunding.Grant grant = this.getGrantFromDB(grantReferenceTail);
            ZenodoFunding.Grant grant = new ZenodoFunding.Grant();
            grant.setTitle(grantReference.getLabel());
//            grant.setNumber(grantReference.getReference());
            ZenodoFunding funding = new ZenodoFunding();
            if (!funders.isEmpty()) funding.setFunder(funders.getFirst());
            funding.setGrant(grant);
            fundingList.add(funding);
        }
        for (int i = fundingList.isEmpty() ? 0 : 1; i < funders.size(); i++) {
            ZenodoFunding funding = new ZenodoFunding();
            funding.setFunder(funders.get(i));
            fundingList.add(funding);
        }

        if (deposit.getMetadata().getFunding() == null)deposit.getMetadata().setFunding(new ArrayList<>());
        deposit.getMetadata().getFunding().addAll(fundingList);
    }

    private void applyCreators(PlanModel plan, ZenodoDeposit deposit, DepositPluginSettings settings){
        if (plan.getUsers() == null) return;

        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        if (deposit.getMetadata().getCreators() == null)deposit.getMetadata().setCreators(new ArrayList<>());
        deposit.getMetadata().getCreators().addAll(this.setUsers(plan, settings));
    }

    private List<ZenodoCreator> setUsers(PlanModel plan, DepositPluginSettings settings){

        List<ReferenceModel> planOrganizations = this.getReferenceModelOfType(plan, settings.text(SETTING_ORGANISATIONS_TYPE, zenodoServiceProperties.getOrganizationReferenceCode()));
        String zenodoAffiliation = settings.text(SETTING_AFFILIATION, zenodoServiceProperties.getAffiliation());

        List<ZenodoCreator> contributors = new ArrayList<>();
        for (PlanUserModel planUser: settings.authors(plan.getUsers())) {

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
                    if (affiliations.stream().noneMatch(x -> x.getName().equalsIgnoreCase(affiliation.getName()))) affiliations.add(affiliation);
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
            if (subjects.stream().noneMatch(x -> x.getSubject().equalsIgnoreCase(subject.getSubject()))) subjects.add(subject);
        }

        if (!subjects.isEmpty()) deposit.getMetadata().setSubjects(subjects);
    }

    private void applyLanguages(PlanModel planModel, ZenodoDeposit deposit, String defaultLanguage) {
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        List<ZenodoLanguage> languages = new ArrayList<>();
        for (String value: this.applyListValue(planModel, SEMANTIC_LANGUAGE)) {
            ULocale locale = new ULocale(value);
            if (locale.getISO3Language().equals(value)) {
                ZenodoLanguage language = new ZenodoLanguage();
                language.setId(value);
                if (languages.stream().noneMatch(x -> x.getId().equalsIgnoreCase(language.getId()))) languages.add(language);
            }

        }
        if (languages.isEmpty() && defaultLanguage != null) {
            ZenodoLanguage language = new ZenodoLanguage();
            language.setId(defaultLanguage.toLowerCase());
            languages.add(language);
        }

        if (!languages.isEmpty()) deposit.getMetadata().setLanguages(languages);
    }

    private void applyReferences(PlanModel planModel, ZenodoDeposit deposit) {
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        List<ZenodoReference> references = new ArrayList<>();
        for (String value: this.applyListValue(planModel, SEMANTIC_REFERENCE)) {
            ZenodoReference reference = new ZenodoReference();
            reference.setReference(value);
            if (references.stream().noneMatch(x -> x.getReference().equalsIgnoreCase(reference.getReference()))) references.add(reference);
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

        if (journal.getTitle() != null || journal.getIssn() != null || journal.getVolume() != null || journal.getIssue() != null || journal.getPages() != null) customFields.setJournal(journal);

        ZenodoDepositCustomFields.Imprint imprint = new ZenodoDepositCustomFields.Imprint();

        List<String> imprintTitle = this.applyListValue(planModel, SEMANTIC_IMPRINT_TITLE);
        if (!imprintTitle.isEmpty()) imprint.setTitle(imprintTitle.getFirst());

        List<String> imprintIsbn = this.applyListValue(planModel, SEMANTIC_IMPRINT_ISBN);
        if (!imprintIsbn.isEmpty()) imprint.setIsbn(imprintIsbn.getFirst());

        List<String> imprintPlace = this.applyListValue(planModel, SEMANTIC_IMPRINT_PLACE);
        if (!imprintPlace.isEmpty()) imprint.setPlace(imprintPlace.getFirst());

        List<String> imprintPagination = this.applyListValue(planModel, SEMANTIC_IMPRINT_PAGINATION);
        if (!imprintPagination.isEmpty()) imprint.setPages(imprintPagination.getFirst());

        if (imprint.getTitle() != null || imprint.getIsbn() != null || imprint.getPlace() != null || imprint.getPages() != null) customFields.setImprint(imprint);

        List<String> thesisUniversity = this.applyListValue(planModel, SEMANTIC_THESIS_UNIVERSITY);
        if (!thesisUniversity.isEmpty()) {
            ZenodoDepositCustomFields.Thesis thesis = new ZenodoDepositCustomFields.Thesis();
            thesis.setUniversity(thesisUniversity.getFirst());
            customFields.setThesis(thesis);
        }
    }

    private List<String> applyListValue(PlanModel planModel, String semanticCode) {
        List<String> fields = new ArrayList<>();

        //plan blueprint semantics
        List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fieldsWithSemantics = this.findSchematicValues(semanticCode, planModel.getPlanBlueprint().getDefinition().getAllField());
        Set<String> values = extractSchematicValues(fieldsWithSemantics, planModel.getProperties().getFieldSets());

        if (!values.isEmpty()) {
            fields.addAll(values.stream().toList());
        }

        if (planModel.getDescriptions() != null) {
            //description template
            for (DescriptionModel descriptionModel: planModel.getDescriptions()) {
                fieldsWithSemantics = this.findSchematicValues(semanticCode, descriptionModel.getDescriptionTemplate().getDefinition().getAllField());
                values = extractSchematicValues(fieldsWithSemantics, descriptionModel.getProperties().getFieldSets());
                if (!values.isEmpty()) {
                    fields.addAll(values.stream().toList());
                }
            }
        }

        return fields;
    }

    private void applyAdditionalTitle(PlanModel planModel, ZenodoDeposit deposit) {
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        List<ZenodoAdditionalTitle> additionalTitles = new ArrayList<>();

        List<String> semantics = List.of(SEMANTIC_ADDITIONAL_TITLE, SEMANTIC_ADDITIONAL_TITLE_TYPE, SEMANTIC_ADDITIONAL_TITLE_LANGUAGE);

        for (FieldSetModel fieldSet : this.blueprintFieldSearcherService.searchFieldSetsBySemantics(planModel.getPlanBlueprint(), semantics)) {
            this.buildAdditionalTitle(fieldSet, planModel.getPlanBlueprint().getDefinition().getAllField(), planModel.getProperties().getFieldSets(), additionalTitles);
        }

        if (planModel.getDescriptions() != null) {
            for (DescriptionModel descriptionModel : planModel.getDescriptions()) {
                for (FieldSetModel fieldSet : this.templateFieldSearcherService.searchFieldSetsBySemantics(descriptionModel.getDescriptionTemplate(), semantics)) {
                    this.buildAdditionalTitle(fieldSet, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), descriptionModel.getProperties().getFieldSets(), additionalTitles);
                }
            }
        }

        if (!additionalTitles.isEmpty()) deposit.getMetadata().setAdditionalTitles(additionalTitles);
    }

    private void buildAdditionalTitle(FieldSetModel fieldSet, List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fieldModels, Map<String, PropertyDefinitionFieldSetModel> fieldSets, List<ZenodoAdditionalTitle> additionalTitles) {
        List<org.opencdmp.commonmodels.models.plandescriptionfields.PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetValue(fieldSet, fieldSets);
        for (org.opencdmp.commonmodels.models.plandescriptionfields.PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {

            ZenodoAdditionalTitle additionalTitle = new ZenodoAdditionalTitle();
            ZenodoAdditionalTitle.Type type = new ZenodoAdditionalTitle.Type();

            FieldModel fieldValue = this.findValueFieldBySemantic(fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_ADDITIONAL_TITLE);
            if (fieldValue != null) {
                FieldModel finalFieldValue = fieldValue;
                String value = this.extractSchematicSingleValue(fieldModels.stream().filter(x-> x.getId().equals(finalFieldValue.getId())).findFirst().orElse(null), fieldValue);
                if (value != null && !value.isBlank()) additionalTitle.setTitle(value);
            }
            // a type or language without a title is not an additional title
            if (additionalTitle.getTitle() == null) continue;

            fieldValue = this.findValueFieldBySemantic(fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_ADDITIONAL_TITLE_TYPE);
            if (fieldValue != null) {
                FieldModel finalFieldValue1 = fieldValue;
                String value = this.extractSchematicSingleValue(fieldModels.stream().filter(x-> x.getId().equals(finalFieldValue1.getId())).findFirst().orElse(null), fieldValue);
                if (value != null && Arrays.stream(ZenodoAdditionalTitleIdType.values()).toList().stream().map(ZenodoAdditionalTitleIdType::getValue).toList().contains(value)) {
                    type.setId(ZenodoAdditionalTitleIdType.of(value));
                } else {
                    type.setId(ZenodoAdditionalTitleIdType.OTHER);
                }
                additionalTitle.setType(type);
            } else {
                type.setId(ZenodoAdditionalTitleIdType.OTHER);
                additionalTitle.setType(type);
            }

            fieldValue = this.findValueFieldBySemantic(fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_ADDITIONAL_TITLE_LANGUAGE);
            if (fieldValue != null) {
                FieldModel finalFieldValue = fieldValue;
                String value = this.extractSchematicSingleValue(fieldModels.stream().filter(x-> x.getId().equals(finalFieldValue.getId())).findFirst().orElse(null), fieldValue);
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
                if (!programmingLanguage.isEmpty()) customFields.setCodeProgrammingLanguage(programmingLanguage);
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

        if (conference.getTitle() != null || conference.getAcronym() != null || conference.getPlace() != null || conference.getDates() != null
                || conference.getUrl() != null || conference.getSession() != null || conference.getSessionPart() != null) customFields.setConference(conference);

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


    /** The publication date as yyyy-MM-dd from a yyyy-MM-dd or yyyy/MM/dd date or an ISO instant; null when it is none of them. */
    private String parsePublicationDate(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        for (String pattern : List.of("yyyy-MM-dd", "yyyy/MM/dd")) {
            try {
                return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern(pattern)).toString();
            } catch (Exception ignored) {
            }
        }
        try {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(Instant.parse(trimmed));
        } catch (Exception e) {
            logger.warn("invalid publication date for value: " + value);
            return null;
        }
    }

    /** The value exported for a select or radio option: its value, or its label when the value is empty. */
    private String optionValueOrLabel(String value, String label) {
        if (value != null && !value.isBlank()) return value;
        if (label != null && !label.isBlank()) return label;
        return null;
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
            license.setLink(this.getReferenceFieldValueFromSemantic(identifier, SEMANTIC_LICENSE_LINK));
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

    private org.opencdmp.commonmodels.models.plandescriptionfields.FieldModel findValueFieldBySemantic(FieldSetModel fieldSet, org.opencdmp.commonmodels.models.plandescriptionfields.PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel, String semantic){
        org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field = this.templateFieldSearcherService.findFieldBySemantic(fieldSet, semantic);
        return field != null ? propertyDefinitionFieldSetItemModel.getFields().getOrDefault(field.getId(), null) : null;
    }

    private List<org.opencdmp.commonmodels.models.plandescriptionfields.PropertyDefinitionFieldSetItemModel> findFieldSetValue(FieldSetModel fieldSetModel, Map<String, PropertyDefinitionFieldSetModel> fieldSets){
        List<org.opencdmp.commonmodels.models.plandescriptionfields.PropertyDefinitionFieldSetItemModel> items = new ArrayList<>();
        if (fieldSetModel == null || fieldSets == null) return items;
        PropertyDefinitionFieldSetModel propertyDefinitionFieldSetModel =  fieldSets.getOrDefault(fieldSetModel.getId(), null);
        if (propertyDefinitionFieldSetModel != null && propertyDefinitionFieldSetModel.getItems() != null) return propertyDefinitionFieldSetModel.getItems();
        return items;
    }

    private String getReferenceFieldValueFromSemantic(ReferenceModel referenceModel, String semantic) {
        if (referenceModel == null || referenceModel.getDefinition() == null ||referenceModel.getDefinition().getFields() == null || referenceModel.getDefinition().getFields().isEmpty()
                || referenceModel.getType() == null || referenceModel.getType().getDefinition().getFields() == null || referenceModel.getType().getDefinition().getFields().isEmpty()
                || semantic == null) return null;

        ReferenceTypeFieldModel referenceTypeFieldModel = referenceModel.getType().getDefinition().getFields().stream().filter(x -> x.getSemantics() != null && !x.getSemantics().isEmpty() && x.getSemantics().contains(semantic)).findFirst().orElse(null);
        if (referenceTypeFieldModel == null || referenceTypeFieldModel.getCode() == null) return null;

        ReferenceFieldModel referenceFieldModel = referenceModel.getDefinition().getFields().stream().filter(x -> x.getCode() != null && x.getCode().equals(referenceTypeFieldModel.getCode())).findFirst().orElse(null);
        if (referenceFieldModel == null) return null;

        return referenceFieldModel.getValue();
    }

}

