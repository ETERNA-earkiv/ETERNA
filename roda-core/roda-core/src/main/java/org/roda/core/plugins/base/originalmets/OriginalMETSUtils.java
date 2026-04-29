/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins.base.originalmets;

import java.nio.file.Path;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.LocalDateTime;

import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;

import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.model.ModelService;
import org.roda.core.storage.StorageService;
import org.roda_project.commons_ip2.model.IPConstants;

public class OriginalMETSUtils {
	
	public static StorageService mainStorageService;
	
	public static void setMainStorageService(StorageService storageService) {
		mainStorageService = storageService;
	}

	public static Path metsPath(ModelService modelService, StoragePath aipPath) 
			throws AuthorizationDeniedException, GenericException, NotFoundException, RequestNotValidException {
		
		// First try to find the METS via main storage service, if not use staging storage service
		Path metsPath = mainStorageService.getDirectAccess(aipPath).getPath().resolve(Path.of(IPConstants.METS_FILE));	
		if (!metsPath.toFile().exists()) {
			metsPath = modelService.getStorage().getDirectAccess(aipPath).getPath().resolve(Path.of(IPConstants.METS_FILE));
		}
		return metsPath;
	}
	
	public static Path metsPath(ModelService modelService, StoragePath aipPath, Path representationPath) 
			throws AuthorizationDeniedException, GenericException, NotFoundException, RequestNotValidException {
		
		// First try to find the METS via main storage service, if not use staging storage service
		Path metsPath = mainStorageService.getDirectAccess(aipPath).getPath().resolve(representationPath).resolve(Path.of(IPConstants.METS_FILE));	
		if (!metsPath.toFile().exists()) {
			metsPath = modelService.getStorage().getDirectAccess(aipPath).getPath().resolve(representationPath).resolve(Path.of(IPConstants.METS_FILE));
		}
		return metsPath;
	}
	
	public static XMLGregorianCalendar currentDateAndTime() throws DatatypeConfigurationException {
		 return DatatypeFactory.newInstance().newXMLGregorianCalendar(LocalDateTime.now().toString());
	}
}
