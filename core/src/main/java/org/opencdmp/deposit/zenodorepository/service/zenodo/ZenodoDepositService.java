package org.opencdmp.deposit.zenodorepository.service.zenodo;

import org.opencdmp.commonmodels.models.plan.PlanModel;
import org.opencdmp.depositbase.repository.DepositConfiguration;

public interface ZenodoDepositService {
	String deposit(PlanModel planDepositModel, String zenodoToken) throws Exception;

	DepositConfiguration getConfiguration();

	String authenticate(String code);

	String getLogo();
}
