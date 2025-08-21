package org.opencdmp.deposit.zenodorepository.service.zenodo;

import org.opencdmp.depositbase.repository.DepositConfiguration;
import org.opencdmp.depositbase.repository.PlanDepositModel;

public interface ZenodoDepositService {
	String deposit(PlanDepositModel planDepositModel) throws Exception;

	DepositConfiguration getConfiguration();

	String authenticate(String code);

	String getLogo();
}
