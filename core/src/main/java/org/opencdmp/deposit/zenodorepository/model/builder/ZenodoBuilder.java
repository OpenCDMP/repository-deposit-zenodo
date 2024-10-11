package org.opencdmp.deposit.zenodorepository.model.builder;

import gr.cite.tools.exception.MyApplicationException;
import gr.cite.tools.logging.LoggerService;
import org.opencdmp.commonmodels.enums.PlanAccessType;
import org.opencdmp.commonmodels.enums.PlanUserRole;
import org.opencdmp.commonmodels.models.PlanUserModel;
import org.opencdmp.commonmodels.models.description.*;
import org.opencdmp.commonmodels.models.descriptiotemplate.DefinitionModel;
import org.opencdmp.commonmodels.models.descriptiotemplate.fielddata.RadioBoxDataModel;
import org.opencdmp.commonmodels.models.descriptiotemplate.fielddata.SelectDataModel;
import org.opencdmp.commonmodels.models.plan.PlanBlueprintValueModel;
import org.opencdmp.commonmodels.models.plan.PlanModel;
import org.opencdmp.commonmodels.models.planblueprint.SectionModel;
import org.opencdmp.commonmodels.models.planreference.PlanReferenceModel;
import org.opencdmp.commonmodels.models.reference.ReferenceFieldModel;
import org.opencdmp.commonmodels.models.reference.ReferenceModel;
import org.opencdmp.deposit.zenodorepository.configuration.funder.FunderProperties;
import org.opencdmp.deposit.zenodorepository.configuration.identifier.IdentifierProperties;
import org.opencdmp.deposit.zenodorepository.configuration.pid.PidProperties;
import org.opencdmp.deposit.zenodorepository.enums.ZenodoAccessRight;
import org.opencdmp.deposit.zenodorepository.model.*;
import org.opencdmp.deposit.zenodorepository.service.zenodo.ZenodoDepositServiceImpl;
import org.opencdmp.deposit.zenodorepository.service.zenodo.ZenodoServiceProperties;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ZenodoBuilder {
    private static final LoggerService logger = new LoggerService(LoggerFactory.getLogger(ZenodoDepositServiceImpl.class));

    private static final String UPLOAD_TYPE = "publication";
    private static final String PUBLICATION_TYPE = "datamanagementplan";
    private static final String IS_IDENTICAL_TO = "isIdenticalTo";
    private static final String CONTRIBUTOR_TYPE_RESEARCHER = "Researcher";
    private static final String CONTRIBUTOR_TYPE_PROJECT_MANAGER = "ProjectMember";
    private static final String SEMANTIC_PUBLICATION_DATE = "zenodo.publication_date";

    private final PidProperties pidProperties;
    private final IdentifierProperties identifierProperties;
    private final FunderProperties funderProperties;
    private final ZenodoServiceProperties zenodoServiceProperties;

    @Autowired
    public ZenodoBuilder(PidProperties pidProperties, IdentifierProperties identifierProperties, FunderProperties funderProperties, ZenodoServiceProperties zenodoServiceProperties){
        this.pidProperties = pidProperties;
        this.identifierProperties = identifierProperties;
        this.funderProperties = funderProperties;
	    this.zenodoServiceProperties = zenodoServiceProperties;
    }

    public ZenodoDeposit build(PlanModel plan) {
        ZenodoDeposit deposit = new ZenodoDeposit();
        this.applyZenodoRelator(plan, deposit);
        deposit.getMetadata().setTitle(plan.getLabel());
        deposit.getMetadata().setUploadType(UPLOAD_TYPE);
        deposit.getMetadata().setPublicationType(PUBLICATION_TYPE);
        deposit.getMetadata().setDescription((plan.getDescription() != null && !plan.getDescription().isEmpty() ? plan.getDescription() : "<p></p>"));
        deposit.getMetadata().setVersion(String.valueOf(plan.getVersion()));
        String zenodoCommunity = zenodoServiceProperties.getCommunity();
        if(zenodoCommunity != null && !zenodoCommunity.isEmpty()) {
            if (deposit.getMetadata().getCommunities() == null) deposit.getMetadata().setCommunities(new ArrayList<>());
            ZenodoCommunity community = new ZenodoCommunity();
            community.setIdentifier(zenodoCommunity);
            deposit.getMetadata().getCommunities().add(community);
        }

        org.opencdmp.commonmodels.models.planblueprint.FieldModel fieldOfSemantic = this.getFieldOfSemantic(plan, SEMANTIC_PUBLICATION_DATE);
        if (fieldOfSemantic != null){
            PlanBlueprintValueModel planBlueprintValueModel = this.getPlanBlueprintValue(plan, fieldOfSemantic.getId());
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
        this.applyAccessRight(plan, deposit);
        this.applyIsIdenticalTo(plan, deposit);
        this.applyLicenses(plan, deposit);
        this.applyResearchers(plan, deposit);
        this.applyGrants(plan, deposit);
        this.applyContributors(plan, deposit);
        this.applyCreators(plan, deposit);

        return deposit;
    }

    private PlanBlueprintValueModel getPlanBlueprintValue(PlanModel plan, UUID id){
        if (plan == null || plan.getProperties() == null || plan.getProperties().getPlanBlueprintValues() == null) return null;
        return plan.getProperties().getPlanBlueprintValues().stream().filter(x-> x.getFieldId().equals(id)).findFirst().orElse(null);
    }
    
    private org.opencdmp.commonmodels.models.planblueprint.FieldModel getFieldOfSemantic(PlanModel plan, String semanticKey){
        if (plan == null || plan.getPlanBlueprint() == null || plan.getPlanBlueprint().getDefinition() == null || plan.getPlanBlueprint().getDefinition().getSections() == null) return null;
        for (SectionModel sectionModel : plan.getPlanBlueprint().getDefinition().getSections()){
            if (sectionModel.getFields() != null){
                org.opencdmp.commonmodels.models.planblueprint.FieldModel fieldModel = sectionModel.getFields().stream().filter(x-> x.getSemantics() != null && x.getSemantics().contains(semanticKey)).findFirst().orElse(null);
                if (fieldModel != null) return fieldModel;
            }
        }
        return null;
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

    private Set<String> extractSchematicValues(List<org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel> fields, PropertyDefinitionModel propertyDefinition, List<String> acceptedPidTypes) {
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
                        if (valueField.getDateValue() != null) values.add(DateTimeFormatter.ISO_DATE.format(valueField.getDateValue()));
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
                                if (referenceModel.getType().getCode().equals(zenodoServiceProperties.getOrganizationReferenceCode()) || referenceModel.getType().getCode().equals(zenodoServiceProperties.getResearcherReferenceCode())) {
                                    if (referenceModel.getReference() != null && !referenceModel.getReference().isBlank()) {
                                        values.add(referenceModel.getReference());
                                    }
                                } else {
                                    String pid = referenceModel.getDefinition().getFields().stream().filter(x -> x.getCode() != null && x.getCode().equals(this.pidProperties.getFields().getPidName())).map(ReferenceFieldModel::getValue).findFirst().orElse(null);
                                    String pidType = referenceModel.getDefinition().getFields().stream().filter(x -> x.getCode() != null && x.getCode().equals(this.pidProperties.getFields().getPidTypeName())).map(ReferenceFieldModel::getValue).findFirst().orElse(null);
                                    if (pid != null && !pid.isBlank() && pidType != null && !pidType.isBlank() && acceptedPidTypes.contains(pidType)) {
                                        values.add(pid);
                                    }
                                }
                            }
                        }
                    }
                    case INTERNAL_ENTRIES_DESCRIPTIONS, INTERNAL_ENTRIES_PlANS, UPLOAD -> throw new MyApplicationException("Invalid type " + field.getData().getFieldType());
                    default -> throw new MyApplicationException("Invalid type " + field.getData().getFieldType());
                }
            }
        }
        return values;
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
            for(String relatedId: this.identifierProperties.getRelated()){
                List<org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel> fields = this.findSchematicValues(relatedId, descriptionModel.getDescriptionTemplate().getDefinition());
                Set<String> values = extractSchematicValues(fields, descriptionModel.getProperties(), acceptedPidTypes);
                for(String value: values){
                    ZenodoRelator relator = new ZenodoRelator();
                    relator.setRelation(relatedId.substring(relatedId.lastIndexOf(".") + 1));
                    relator.setIdentifier(value);
                    relatedIdentifiers.add(relator);
                }
            }
        }
        deposit.getMetadata().setRelatedIdentifiers(relatedIdentifiers);
    }
    
    private void applyAccessRight(PlanModel plan, ZenodoDeposit deposit){
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());
        
        if (plan.getAccessType() == null) {
            deposit.getMetadata().setAccessRight(ZenodoAccessRight.RESTRICTED);
            deposit.getMetadata().setAccessConditions("");
        } else {
            if (plan.getAccessType().equals(PlanAccessType.Public)) {
                Instant publicationDate = plan.getFinalizedAt();
                if (publicationDate == null) publicationDate = Instant.now().minusSeconds(1);

                if (publicationDate.isBefore(Instant.now())) {
                    deposit.getMetadata().setAccessRight(ZenodoAccessRight.OPEN);
                } else {
                    deposit.getMetadata().setAccessRight(ZenodoAccessRight.EMBARGOED);
                    deposit.getMetadata().setEmbargoDate(publicationDate.toString());
                }
            } else {
                deposit.getMetadata().setAccessRight(ZenodoAccessRight.RESTRICTED);
                deposit.getMetadata().setAccessConditions("");
            }
        }
    }

    private void applyIsIdenticalTo(PlanModel plan, ZenodoDeposit deposit){
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());
        
        if (plan.getAccessType().equals(PlanAccessType.Public)) {
            ZenodoRelator relator = new ZenodoRelator();
            relator.setIdentifier(zenodoServiceProperties.getDomain() + "/external/zenodo/" + plan.getId().toString());
            relator.setRelation(IS_IDENTICAL_TO);
            if (deposit.getMetadata().getRelatedIdentifiers() == null)deposit.getMetadata().setRelatedIdentifiers(new ArrayList<>());
            deposit.getMetadata().getRelatedIdentifiers().add(relator);
        }
    }

    private void applyLicenses(PlanModel plan, ZenodoDeposit deposit){
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());
       
        List<ReferenceModel> planLicenses = this.getReferenceModelOfType(plan, zenodoServiceProperties.getLicensesReferenceCode());
        if (!planLicenses.isEmpty()) {
            for (ReferenceModel planLicense : planLicenses) {
                if (planLicense != null && planLicense.getReference() != null && !planLicense.getReference().isBlank()) {
                    deposit.getMetadata().setLicense(planLicense.getReference());
                    break;
                }
            }
        }
    }

    private void applyResearchers(PlanModel plan, ZenodoDeposit deposit){
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        List<ZenodoContributor> researchers = new ArrayList<>();
        List<ReferenceModel> planResearchers = this.getReferenceModelOfType(plan, zenodoServiceProperties.getResearcherReferenceCode());
        if (planResearchers != null && !planResearchers.isEmpty()) {
            for (ReferenceModel researcher : planResearchers) {
                ZenodoContributor contributor = new ZenodoContributor();
                contributor.setName(researcher.getLabel());
                contributor.setType(CONTRIBUTOR_TYPE_RESEARCHER);
                contributor.setAffiliation(researcher.getSource());
                if (researcher.getSource().equalsIgnoreCase(zenodoServiceProperties.getOrcidResearcherSourceCode())) {
                    contributor.setOrcid(researcher.getReference());
                }
                researchers.add(contributor);
            }
        }

        if (deposit.getMetadata().getContributors() == null)deposit.getMetadata().setContributors(new ArrayList<>());

        deposit.getMetadata().getContributors().addAll(researchers);
    }

    private void applyGrants(PlanModel plan, ZenodoDeposit deposit){
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());
        List<ReferenceModel> planGrants = this.getReferenceModelOfType(plan, zenodoServiceProperties.getGrantReferenceCode());
        List<ReferenceModel> planFunders = this.getReferenceModelOfType(plan, zenodoServiceProperties.getFunderReferenceCode());

        if (!planGrants.isEmpty()) {
            ReferenceModel depositGrant = planGrants.stream().filter(x-> x.getSource().equalsIgnoreCase(zenodoServiceProperties.getOpenaireGrantSourceCode())).findFirst().orElse(null);

            if (depositGrant != null) {
                String grantReferenceTail = depositGrant.getReference().split(":")[2];
                List<FunderProperties.DoiFunder> doiFunders = this.funderProperties.getAvailable();
                if (!planFunders.isEmpty()) {
                    ReferenceModel depositFunder = planFunders.getFirst();
                    FunderProperties.DoiFunder doiFunder = doiFunders.stream()
                            .filter(doiFunder1 -> depositFunder.getLabel().contains(doiFunder1.getFunder()) || doiFunder1.getFunder().contains(depositFunder.getLabel()))
                            .findFirst().orElse(null);
                    if (doiFunder != null) {
                        String finalId = doiFunder.getDoi() + "::" + grantReferenceTail;
                        ZenodoGrant grant = new ZenodoGrant();
                        grant.setId(finalId);
                        if (deposit.getMetadata().getGrants() == null)deposit.getMetadata().setGrants(new ArrayList<>());
                        deposit.getMetadata().getGrants().add(grant);
                    }
                }
            }
        }
    }

    private void applyContributors(PlanModel plan, ZenodoDeposit deposit){
        if (plan.getUsers() == null) return;
        
        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());
        
        List<ReferenceModel> planOrganizations = this.getReferenceModelOfType(plan, zenodoServiceProperties.getOrganizationReferenceCode());
        String zenodoAffiliation = zenodoServiceProperties.getAffiliation();
        
        List<ZenodoContributor> contributors = new ArrayList<>();
        for (PlanUserModel planUser: plan.getUsers()) {
            ZenodoContributor contributor = new ZenodoContributor();
            contributor.setName(planUser.getUser().getName());
            contributor.setType(CONTRIBUTOR_TYPE_PROJECT_MANAGER);
            if (planOrganizations != null && !planOrganizations.isEmpty()) {
                contributor.setAffiliation(planOrganizations.stream().map(ReferenceModel::getLabel).collect(Collectors.joining(", ")));
            } else {
                if(zenodoAffiliation != null && !zenodoAffiliation.isEmpty()) {
                    contributor.setAffiliation(zenodoAffiliation);
                }
            }
            contributors.add(contributor);
        }
        if (deposit.getMetadata().getContributors() == null)deposit.getMetadata().setContributors(new ArrayList<>());

        deposit.getMetadata().getContributors().addAll(contributors);
      
    }

    private void applyCreators(PlanModel plan, ZenodoDeposit deposit){
        if (plan.getUsers() == null) return;

        if (deposit.getMetadata() == null) deposit.setMetadata(new ZenodoDepositMetadata());

        List<ReferenceModel> planOrganizations = this.getReferenceModelOfType(plan, zenodoServiceProperties.getOrganizationReferenceCode());
        String zenodoAffiliation = zenodoServiceProperties.getAffiliation();
        
        ZenodoCreator creator = new ZenodoCreator();
        PlanUserModel planModel = plan.getUsers().stream().filter(planUser -> planUser.getRole().equals(PlanUserRole.Owner)).findFirst().orElse(null);
        if (planModel == null || planModel.getUser() == null) return;
        
        creator.setName(planModel.getUser().getName());
        if (planOrganizations != null && !planOrganizations.isEmpty()) {
            creator.setAffiliation(planOrganizations.stream().map(ReferenceModel::getLabel).collect(Collectors.joining(", ")));
        } else {
            if(zenodoAffiliation != null && !zenodoAffiliation.isEmpty()) {
                creator.setAffiliation(zenodoAffiliation);
            }
        }
        if (deposit.getMetadata().getCreators() == null)deposit.getMetadata().setCreators(new ArrayList<>());
        deposit.getMetadata().getCreators().add(creator);
    }
}

