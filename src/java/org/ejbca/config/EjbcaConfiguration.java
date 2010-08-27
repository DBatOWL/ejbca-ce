/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package org.ejbca.config;

import org.apache.log4j.Logger;

/**
 * This file handles configuration from ejbca.properties
 */
public class EjbcaConfiguration {
	
	private static final Logger log = Logger.getLogger(EjbcaConfiguration.class);
	
	/**
	 * Check if EJBCA is running in production
	 */
	public static boolean getIsInProductionMode() {
		String value = ConfigurationHolder.getString("ejbca.productionmode", "true");
		if ("true".equalsIgnoreCase(value) || "ca".equalsIgnoreCase(value) || "ocsp".equalsIgnoreCase(value)) {
			return true;
		}
		return false;
	}

	/**
	 * Password used to protect CA keystores in the database.
	 */
	public static String getCaKeyStorePass() {
		return ConfigurationHolder.getExpandedString("ca.keystorepass", "foo123");
	}

	/**
	 * Password used to protect XKMS keystores in the database.
	 */
	public static String getCaXkmsKeyStorePass() {
		return ConfigurationHolder.getExpandedString("ca.xkmskeystorepass", "foo123");
	}

	/**
	 * Password used to protect CMS keystores in the database.
	 */
	public static String getCaCmsKeyStorePass() {
		return ConfigurationHolder.getExpandedString("ca.cmskeystorepass", "foo123");
	}

	/**
	 * The length in octets of certificate serial numbers generated. 8 octets is a 64 bit serial number.
	 */
	public static int getCaSerialNumberOctetSize() {
		String value = ConfigurationHolder.getString("ca.serialnumberoctetsize", "8");
		if (!value.equals("8") && !value.equals("4") ) {
			value = "8";
		}
		return Integer.parseInt(value);
	}

	/**
	 * The date and time from which an expire date of a certificate is to be considered to be too far in the future.
	 */
	public static String getCaTooLateExpireDate() {
		return ConfigurationHolder.getExpandedString("ca.toolateexpiredate", "");
	}

	/**
	 * The language that should be used internally for logging, exceptions and approval notifications.
	 */
	public static String getInternalResourcesPreferredLanguage() {
		return ConfigurationHolder.getExpandedString("intresources.preferredlanguage", "EN");
	}
	
	/**
	 * The language used internally if a resource not found in the preferred language
	 */
	public static String getInternalResourcesSecondaryLanguage() {
		return ConfigurationHolder.getExpandedString("intresources.secondarylanguage", "SE");
	}
	
	/**
	 * How long an request should stay valid
	 */
	public static long getApprovalDefaultRequestValidity() {
		long value = 28800L;
		try {
			value = Long.parseLong(ConfigurationHolder.getString("approval.defaultrequestvalidity", ""+value));
		} catch( NumberFormatException e ) {
			log.warn("\"approval.defaultrequestvalidity\" is not a decimal number. Using default value: " + value);
		}
		return value*1000L;
	}

	/**
	 * How long an approved request should stay valid
	 */
	public static long getApprovalDefaultApprovalValidity() {
		long value = 28800L;
		try {
			value = Long.parseLong(ConfigurationHolder.getString("approval.defaultapprovalvalidity", ""+value));
		} catch( NumberFormatException e ) {
			log.warn("\"approval.defaultapprovalvalidity\" is not a decimal number. Using default value: " + value);
		}
		return value*1000L;
	}

	/**
	 * Excluded classes from approval.
	 */
	public static String getApprovalExcludedClasses() {
		return ConfigurationHolder.getExpandedString("approval.excludedClasses", "");
	}

	/**
	 * Determines if log4j should be initialized explicitly, needed for glassfish, oracle
	 */
	public static String getLoggingLog4jConfig() {
		return ConfigurationHolder.getExpandedString("logging.log4j.config", "false");
	}

	/**
	 * Parameter specifying amount of free memory (Mb) before alarming
	 */
	public static long getHealthCheckAmountFreeMem() {
		long value = 1;
		try {
			value = Long.parseLong(ConfigurationHolder.getString("healthcheck.amountfreemem", ConfigurationHolder.getString("ocsphealthcheck.amountfreemem", ""+value)));
		} catch( NumberFormatException e ) {
			log.warn("\"healthcheck.amountfreemem\" or \"ocsphealthcheck.amountfreemem\" is not a decimal number. Using default value: " + value);
		}
		return value*1024L*1024L;
	}

	/**
	 * Parameter specifying database test query string. Used to check that the database is operational.
	 */
	public static String getHealthCheckDbQuery() {
		return ConfigurationHolder.getExpandedString("healthcheck.dbquery", ConfigurationHolder.getExpandedString("ocsphealthcheck.dbquery", "Select 1 From CertificateData where fingerprint='XX'"));
	}
	
	/**
	 * Parameter to specify location of file containing information about maintenance
	 */
	public static String getHealthCheckAuthorizedIps() {
		return ConfigurationHolder.getExpandedString("healthcheck.authorizedips", ConfigurationHolder.getExpandedString("ocsphealthcheck.authorizedips", "127.0.0.1"));
	}
	
	/**
	 * Parameter to specify if the check of CA tokens should actually perform a signature test on the CA token.
	 */
	public static boolean getHealthCheckCaTokenSignTest() {
		return "true".equalsIgnoreCase(ConfigurationHolder.getString("healthcheck.catokensigntest", "false"));
	}

	/**
	 * Parameter to specify location of file containing information about maintenance
	 */
	public static String getHealthCheckMaintenanceFile() {
		return ConfigurationHolder.getExpandedString("healthcheck.maintenancefile", ConfigurationHolder.getExpandedString("ocsphealthcheck.maintenancefile", ""));
	}
	
	/**
	 * Parameter to configure name of maintenance property.
	 */
	public static String getHealthCheckMaintenancePropertyName() {
		return ConfigurationHolder.getExpandedString("healthcheck.maintenancepropertyname", ConfigurationHolder.getExpandedString("ocsphealthcheck.maintenancepropertyname", "DOWN_FOR_MAINTENANCE"));
	}
	
	/**
	 * Sets pre-defined EC curve parameters for the implicitlyCA facility.
	 */
	public static String getEcdsaImplicitlyCaQ() {
		return ConfigurationHolder.getExpandedString("ecdsa.implicitlyca.q", "883423532389192164791648750360308885314476597252960362792450860609699839");
	}
	
	/**
	 * Sets pre-defined EC curve parameters for the implicitlyCA facility.
	 */
	public static String getEcdsaImplicitlyCaA() {
		return ConfigurationHolder.getExpandedString("ecdsa.implicitlyca.a", "7fffffffffffffffffffffff7fffffffffff8000000000007ffffffffffc");
	}
	
	/**
	 * Sets pre-defined EC curve parameters for the implicitlyCA facility.
	 */
	public static String getEcdsaImplicitlyCaB() {
		return ConfigurationHolder.getExpandedString("ecdsa.implicitlyca.b", "6b016c3bdcf18941d0d654921475ca71a9db2fb27d1d37796185c2942c0a");
	}
	
	/**
	 * Sets pre-defined EC curve parameters for the implicitlyCA facility.
	 */
	public static String getEcdsaImplicitlyCaG() {
		return ConfigurationHolder.getExpandedString("ecdsa.implicitlyca.g", "020ffa963cdca8816ccc33b8642bedf905c3d358573d3f27fbbd3b3cb9aaaf");
	}
	
	/**
	 * Sets pre-defined EC curve parameters for the implicitlyCA facility.
	 */
	public static String getEcdsaImplicitlyCaN() {
		return ConfigurationHolder.getExpandedString("ecdsa.implicitlyca.n", "883423532389192164791648750360308884807550341691627752275345424702807307");
	}
	
	/**
	 * Specifies if the DN order should be constructed in forward (standard and default) or reverse order.
	 * @deprecated use soft configuration on CA instead
	 */
	public static boolean getCertToolsDnOrderReverse() {
		return "true".equalsIgnoreCase(ConfigurationHolder.getString("certtools.dnorderreverse", "false"));
	}
	
    /**
     * Flag indicating if the BC provider should be removed before installing it again. When developing and re-deploying alot
     * this is needed so you don't have to restart JBoss all the time. 
     * In production it may cause failures because the BC provider may get removed just when another thread wants to use it.
     * Therefore the default value is false. 
     */
	public static boolean getDevelopmentProviderInstallation() {
		return "true".equalsIgnoreCase(ConfigurationHolder.getString("development.provider.installation", "false"));
	}
	
	/**
	 * Parameter to specify if retrieving CAInfo and CA from CAAdminSession should be cached, and in that case for how long.
	 */
	public static long getCacheCaTimeInCaAdminSession() {
		long time = -1; // don't cache at all is the default
		try {
			time = Long.valueOf(ConfigurationHolder.getString("caadmin.cachecainfo", "-1"));
		} catch (NumberFormatException e) {
			log.error("Invalid value in caadmin.cachecainfo, must be decimal number (milliseconds to cache CA info): "+e.getMessage());
		}
		return time;
	}

	/**
	 * Parameter to specify if retrieving endEntity profiles in RAAdminSession should be cached, and in that case for how long.
	 */
	public static long getCacheEndEntityProfileTime() {
		long time = 1000; // cache 1 second is the default
		try {
			time = Long.valueOf(ConfigurationHolder.getString("raadmin.cacheprofiles", "1000"));
		} catch (NumberFormatException e) {
			log.error("Invalid value in raadmin.cacheprofiles, must be decimal number (milliseconds to cache EndEntity profiles): "+e.getMessage());
		}
		return time;
	}

	/**
	 * Parameter to specify if retrieving GlobalConfiguration (in RAAdminSession) should be cached, and in that case for how long.
	 */
	public static long getCacheGlobalConfigurationTime() {
		long time = 30000; // cache 30 seconds is the default
		try {
			time = Long.valueOf(ConfigurationHolder.getString("globalconfiguration.cachetime", "30000"));
		} catch (NumberFormatException e) {
			log.error("Invalid value in globalconfiguration.cachetime, must be decimal number (milliseconds to cache global configuration): "+e.getMessage());
		}
		return time;
	}

	/**
	 * Parameter to specify if retrieving Authorization Access Rules (in AuthorizationSession) should be cached, and in that case for how long.
	 */
	public static long getCacheAuthorizationTime() {
		long time = 30000; // cache 30 seconds is the default
		try {
			time = Long.valueOf(ConfigurationHolder.getString("authorization.cachetime", "30000"));
		} catch (NumberFormatException e) {
			log.error("Invalid value in authorization.cachetime, must be decimal number (milliseconds to cache authorization): "+e.getMessage());
		}
		return time;
	}

}
