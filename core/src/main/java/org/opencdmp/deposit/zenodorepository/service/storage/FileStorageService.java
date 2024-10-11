package org.opencdmp.deposit.zenodorepository.service.storage;

public interface FileStorageService {
	String storeFile(byte[] data);

	byte[] readFile(String fileRef);
}
