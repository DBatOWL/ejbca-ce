package org.ejbca.webtest.scenario;

import org.ejbca.webtest.WebTestBase;
import org.ejbca.webtest.helper.*;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.WebDriver;

import java.util.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EcaQa305_AdministratorRolesSupervisor extends WebTestBase {

    //Classes
    private static CertificateProfileHelper certificateProfileHelper;
    private static EndEntityProfileHelper endEntityProfileHelper;
    private static AdminRolesHelper adminRolesHelper;

    //Test Data
    private static class TestData {
        static final String CERTIFICATE_PROFILE_NAME_SUPERVISOR = "Supervisor";
        static final String CERTIFICATE_PROFILE_NAME_ENDUSER = "ENDUSER";
        static final List<String> SELECTED_AVAILABLE_BIT_LENGTHS = new ArrayList<>(Arrays.asList("1024 bits", "2048 bits", "4096 bits"));
        static final String VALIDITY_INPUT = "1y";
        static final String CA_NAME = "ManagementCA";
        static final String SELECTED_EXTENDED_KEY_USAGE = "Client Authentication";
        static final String END_ENTITY_PROFILE = "Supervisor";
        static final String ROLE_NAME = "Supervisor";
        static final String ROLE_TEMPLATE = "Supervisors";
    }

    @BeforeClass
    public static void init() {
        // super
        beforeClass(true, null);
        WebDriver webDriver = getWebDriver();
        // Init helpers
        certificateProfileHelper = new CertificateProfileHelper(webDriver);
        endEntityProfileHelper = new EndEntityProfileHelper(webDriver);
        adminRolesHelper = new AdminRolesHelper(webDriver);
    }

    @AfterClass
    public static void exit() {
        // super
        afterClass();
    }

    @After
    public void afterTest() {
        // Remove generated artifacts
        removeCertificateProfileByName(TestData.CERTIFICATE_PROFILE_NAME_SUPERVISOR);
        removeEndEntityProfileByName(TestData.END_ENTITY_PROFILE);
        removeAdministratorRoleByName(TestData.ROLE_NAME);
    }

    @Test
    public void test1_CreateCertificateProfile() {
        //When
        certificateProfileHelper.openPage(getAdminWebUrl());
        certificateProfileHelper.cloneCertificateProfile(TestData.CERTIFICATE_PROFILE_NAME_ENDUSER, TestData.CERTIFICATE_PROFILE_NAME_SUPERVISOR);
        //Then
        certificateProfileHelper.assertCertificateProfileNameExists(TestData.CERTIFICATE_PROFILE_NAME_SUPERVISOR);
    }

    @Test
    public void test2_EditCertificateProfileAndAssertSaved() {
        //when
        certificateProfileHelper.openPage(getAdminWebUrl());
        certificateProfileHelper.cloneCertificateProfile(TestData.CERTIFICATE_PROFILE_NAME_ENDUSER, TestData.CERTIFICATE_PROFILE_NAME_SUPERVISOR);
        certificateProfileHelper.openEditCertificateProfilePage(TestData.CERTIFICATE_PROFILE_NAME_SUPERVISOR);
        certificateProfileHelper.editAvailableBitLengthsInCertificateProfile(TestData.SELECTED_AVAILABLE_BIT_LENGTHS);
        certificateProfileHelper.fillValidity(TestData.VALIDITY_INPUT);
        certificateProfileHelper.triggerPermissionsKeyUsageOverride();
        certificateProfileHelper.triggerX509v3ExtensionsUsagesKeyUsageNonRepudiation();
        certificateProfileHelper.selectExtendedKeyUsage(TestData.SELECTED_EXTENDED_KEY_USAGE);
        certificateProfileHelper.selectAvailableCa(TestData.CA_NAME);
        //then
        certificateProfileHelper.saveCertificateProfile();
    }

    @Test
    public void test3_AddEndEntityProfileAndAssertSaved() {
        //when
        endEntityProfileHelper.openPage(getAdminWebUrl());
        //then
        endEntityProfileHelper.addEndEntityProfile(TestData.END_ENTITY_PROFILE);
        endEntityProfileHelper.assertEndEntityProfileNameExists(TestData.END_ENTITY_PROFILE);
    }

    @Test
    public void test4_EditEndEntityProfileAndAssertSaved() {
        //when
        certificateProfileHelper.openPage(getAdminWebUrl());
        certificateProfileHelper.addCertificateProfile(TestData.CERTIFICATE_PROFILE_NAME_SUPERVISOR);
        endEntityProfileHelper.openPage(getAdminWebUrl());
        endEntityProfileHelper.addEndEntityProfile(TestData.END_ENTITY_PROFILE);
        endEntityProfileHelper.openEditEndEntityProfilePage(TestData.END_ENTITY_PROFILE);
        endEntityProfileHelper.selectDefaultCp(TestData.CERTIFICATE_PROFILE_NAME_SUPERVISOR);
        endEntityProfileHelper.selectAvailableCp(TestData.CERTIFICATE_PROFILE_NAME_SUPERVISOR);
        endEntityProfileHelper.selectDefaultCa(TestData.CA_NAME);
        endEntityProfileHelper.selectAvailableCa(TestData.CA_NAME);
        //then
        endEntityProfileHelper.saveEndEntityProfile();
    }

    @Test
    public void test5_OpenAndAssertRolesManagementPageIsOpen() {
        adminRolesHelper.openPage(getAdminWebUrl());
    }

    @Test
    public void test6_AddRoleAndAssertExistsAndMessageDisplayed() {
        //when
        adminRolesHelper.openPage(getAdminWebUrl());
        //then
        adminRolesHelper.addRole(TestData.ROLE_NAME);
    }

    @Test
    public void test7_OpenEditAccessRulesAndAssertDisplayed() {
        //when
        adminRolesHelper.openPage(getAdminWebUrl());
        adminRolesHelper.addRole(TestData.ROLE_NAME);
        //then
        adminRolesHelper.openEditAccessRulesPage(TestData.ROLE_NAME);
    }

    @Test
    public void test8_AssertRoleTemplateSupervisorExists() {
        //when
        adminRolesHelper.openPage(getAdminWebUrl());
        adminRolesHelper.addRole(TestData.ROLE_NAME);
        adminRolesHelper.openEditAccessRulesPage(TestData.ROLE_NAME);
        adminRolesHelper.selectRoleTemplate(TestData.ROLE_TEMPLATE);
        adminRolesHelper.assertAuthorizedCAsIsEnabled(true);
        adminRolesHelper.assertEndEntityRulesHasViewEndEntitiesAndViewHistorySelected();
        adminRolesHelper.assertEndEntityProfilesIsEnabled(true);
        adminRolesHelper.assertValidatorsIsEnabled(true);
        adminRolesHelper.assertInternalKeybindingRulesIsEnabled(true);
        adminRolesHelper.assertOtherRulesHasAllSelected();
        adminRolesHelper.saveAccessRule();
        //then
        adminRolesHelper.openPage(getAdminWebUrl());
        adminRolesHelper.openEditAccessRulesPage(TestData.ROLE_NAME);
        adminRolesHelper.assertRoleTemplateHasSelectedName(TestData.ROLE_TEMPLATE);
    }

    @Test
    public void test9_UpdateCAAndEndEntityProfileInRoleAndAssertSaved() {
        //when
        adminRolesHelper.openPage(getAdminWebUrl());
        adminRolesHelper.addRole(TestData.ROLE_NAME);
        adminRolesHelper.openEditAccessRulesPage(TestData.ROLE_NAME);
        adminRolesHelper.selectRoleTemplate("Supervisors");
        adminRolesHelper.selectAvailableSingleCa("All");
        adminRolesHelper.selectAvailableSingleEndEntityProfile("All");
        //then
        adminRolesHelper.saveAccessRule();
    }

}
