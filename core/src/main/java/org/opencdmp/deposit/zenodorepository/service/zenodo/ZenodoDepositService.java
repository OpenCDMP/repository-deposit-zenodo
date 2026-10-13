package org.opencdmp.deposit.zenodorepository.service.zenodo;

import org.opencdmp.commonmodels.models.deposit.DepositPreviewModel;
import org.opencdmp.commonmodels.models.deposit.PreviewFieldOption;
import org.opencdmp.depositbase.repository.DepositConfiguration;
import org.opencdmp.depositbase.repository.PlanDepositModel;

import java.util.List;

public interface ZenodoDepositService {
	String deposit(PlanDepositModel planDepositModel);

	DepositPreviewModel preview(PlanDepositModel planDepositModel);

	List<PreviewFieldOption> searchVocabulary(String fieldCode, String like);

	DepositConfiguration getConfiguration();

	String authenticate(String code);

	String getLogo();
}
