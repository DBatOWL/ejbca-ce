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

package se.anatom.ejbca.ra;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.DuplicateKeyException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.RemoveException;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;

import se.anatom.ejbca.BaseSessionBean;
import se.anatom.ejbca.JNDINames;
import se.anatom.ejbca.SecConst;
import se.anatom.ejbca.authorization.AuthorizationDeniedException;
import se.anatom.ejbca.authorization.AvailableAccessRules;
import se.anatom.ejbca.authorization.IAuthorizationSessionLocal;
import se.anatom.ejbca.authorization.IAuthorizationSessionLocalHome;
import se.anatom.ejbca.ca.store.ICertificateStoreSessionLocal;
import se.anatom.ejbca.ca.store.ICertificateStoreSessionLocalHome;
import se.anatom.ejbca.common.UserDataVO;
import se.anatom.ejbca.keyrecovery.IKeyRecoverySessionLocal;
import se.anatom.ejbca.keyrecovery.IKeyRecoverySessionLocalHome;
import se.anatom.ejbca.log.Admin;
import se.anatom.ejbca.log.ILogSessionLocal;
import se.anatom.ejbca.log.ILogSessionLocalHome;
import se.anatom.ejbca.log.LogConstants;
import se.anatom.ejbca.log.LogEntry;
import se.anatom.ejbca.ra.exception.NotFoundException;
import se.anatom.ejbca.ra.raadmin.DNFieldExtractor;
import se.anatom.ejbca.ra.raadmin.EndEntityProfile;
import se.anatom.ejbca.ra.raadmin.GlobalConfiguration;
import se.anatom.ejbca.ra.raadmin.IRaAdminSessionLocal;
import se.anatom.ejbca.ra.raadmin.IRaAdminSessionLocalHome;
import se.anatom.ejbca.ra.raadmin.UserDoesntFullfillEndEntityProfile;
import se.anatom.ejbca.util.CertTools;
import se.anatom.ejbca.util.JDBCUtil;
import se.anatom.ejbca.util.StringTools;
import se.anatom.ejbca.util.TemplateMimeMessage;
import se.anatom.ejbca.util.query.BasicMatch;
import se.anatom.ejbca.util.query.IllegalQueryException;
import se.anatom.ejbca.util.query.Query;
import se.anatom.ejbca.util.query.UserMatch;

/**
 * Administrates users in the database using UserData Entity Bean.
 * Uses JNDI name for datasource as defined in env 'Datasource' in ejb-jar.xml.
 *
 * @version $Id: LocalUserAdminSessionBean.java,v 1.100.2.5 2006-01-03 14:31:52 anatom Exp $
 * @ejb.bean
 *   display-name="UserAdminSB"
 *   name="UserAdminSession"
 *   view-type="both"
 *   type="Stateless"
 *   transaction-type="Container"
 *
 * @ejb.transaction type="Required"
 *
 * @weblogic.enable-call-by-reference True
 *
 * @ejb.env-entry
 *  name="DataSource"
 *  type="java.lang.String"
 *  value="${datasource.jndi-name}"
 *
 * @ejb.env-entry
 *   description="Factory class can dynamically link to external implementation class"
 *   name="RMIFactory"
 *   type="java.lang.String"
 *   value="se.walter.cardPersonalization.ra.ejbca.RMIFactoryImpl"
 *
 * @ejb.env-entry
 *   description="Defines the JNDI name of the mail service used"
 *   name="MailJNDIName"
 *   type="java.lang.String"
 *   value="${mail.jndi-name}"

 *  * @ejb.env-entry
 *   description="Defines the sender of the notification message"
 *   name="sender"
 *   type="java.lang.String"
 *   value="${mail.from}"
 *
 * @ejb.env-entry
 *   description="Defines the subject used in the notification message"
 *   name="subject"
 *   type="java.lang.String"
 *   value="${mail.subject}"
 *
 * @ejb.env-entry
 *   description="Defines the actual message of the notification. Use the values $Username, $Password, $CN, $O, $OU, $C, $DATE to indicate which texts that should be replaced (Case insensitive), $NL stands for newline."
 *   name="message"
 *   type="java.lang.String"
 *   value="${mail.message}"
 *
 * @ejb.ejb-external-ref
 *   description="The Certificate Store session bean"
 *   view-type="local"
 *   ejb-name="CertificateStoreSessionLocal"
 *   type="Session"
 *   home="se.anatom.ejbca.ca.store.ICertificateStoreSessionLocalHome"
 *   business="se.anatom.ejbca.ca.store.ICertificateStoreSessionLocal"
 *   link="CertificateStoreSession"
 *
 * @ejb.ejb-external-ref
 *   description="The Log session bean"
 *   view-type="local"
 *   ejb-name="LogSessionLocal"
 *   type="Session"
 *   home="se.anatom.ejbca.log.ILogSessionLocalHome"
 *   business="se.anatom.ejbca.log.ILogSessionLocal"
 *   link="LogSession"
 *
 * @ejb.ejb-external-ref
 *   description="The Authorization session bean"
 *   view-type="local"
 *   ejb-name="AuthorizationSessionLocal"
 *   type="Session"
 *   home="se.anatom.ejbca.authorization.IAuthorizationSessionLocalHome"
 *   business="se.anatom.ejbca.authorization.IAuthorizationSessionLocal"
 *   link="AuthorizationSession"
 *
 * @ejb.ejb-external-ref
 *   description="The Ra Admin session bean"
 *   view-type="local"
 *   ejb-name="RaAdminSessionLocal"
 *   type="Session"
 *   home="se.anatom.ejbca.ra.raadmin.IRaAdminSessionLocalHome"
 *   business="se.anatom.ejbca.ra.raadmin.IRaAdminSessionLocal"
 *   link="RaAdminSession"
 *
 * @ejb.ejb-external-ref
 *   description="The Key Recovery session bean"
 *   view-type="local"
 *   ejb-name="KeyRecoverySessionLocal"
 *   type="Session"
 *   home="se.anatom.ejbca.keyrecovery.IKeyRecoverySessionLocalHome"
 *   business="se.anatom.ejbca.keyrecovery.IKeyRecoverySessionLocal"
 *   link="KeyRecoverySession"
 *
 * @ejb.ejb-external-ref
 *   description="The User entity bean"
 *   view-type="local"
 *   ejb-name="UserDataLocal"
 *   type="Entity"
 *   home="se.anatom.ejbca.ra.UserDataLocalHome"
 *   business="se.anatom.ejbca.ra.UserDataLocal"
 *   link="UserData"
 *
 * @ejb.resource-ref
 *   res-ref-name="mail/DefaultMail"
 *   res-type="javax.mail.Session"
 *   res-auth="Container"
 *
 * @weblogic.resource-description
 *   res-ref-name="mail/DefaultMail"
 *   jndi-name="EjbcaMail"
 *   
 * @ejb.home
 *   extends="javax.ejb.EJBHome"
 *   local-extends="javax.ejb.EJBLocalHome"
 *   local-class="se.anatom.ejbca.ra.IUserAdminSessionLocalHome"
 *   remote-class="se.anatom.ejbca.ra.IUserAdminSessionHome"
 *
 * @ejb.interface
 *   extends="javax.ejb.EJBObject,UserAdminConstants"
 *   local-extends="javax.ejb.EJBLocalObject,UserAdminConstants"
 *   local-class="se.anatom.ejbca.ra.IUserAdminSessionLocal"
 *   remote-class="se.anatom.ejbca.ra.IUserAdminSessionRemote"
 */
public class LocalUserAdminSessionBean extends BaseSessionBean {


    /**
     * The local interface of RaAdmin Session Bean.
     */
    private IRaAdminSessionLocal raadminsession;

    /**
     * The local interface of the certificate store session bean
     */
    private ICertificateStoreSessionLocal certificatesession;

    /**
     * The local interface of the authorization session bean
     */
    private IAuthorizationSessionLocal authorizationsession;

    /**
     * The local interface of the authorization session bean
     */
    private IKeyRecoverySessionLocal keyrecoverysession;

    /**
     * The remote interface of the log session bean
     */
    private ILogSessionLocal logsession;

    private UserDataLocalHome home = null;
    /**
     * Columns in the database used in select
     */
    private static final String USERDATA_COL = "username, subjectDN, subjectAltName, subjectEmail, status, type, clearpassword, timeCreated, timeModified, endEntityprofileId, certificateProfileId, tokenType, hardTokenIssuerId, cAId";

    /**
     * Default create for SessionBean.
     *
     * @throws CreateException if bean instance can't be created
     * @see se.anatom.ejbca.log.Admin
     */
    public void ejbCreate() throws CreateException {
        debug(">ejbCreate()");
        try {
            home = (UserDataLocalHome) getLocator().getLocalHome(UserDataLocalHome.COMP_NAME);

            ILogSessionLocalHome logsessionhome = (ILogSessionLocalHome) getLocator().getLocalHome(ILogSessionLocalHome.COMP_NAME);
            logsession = logsessionhome.create();

            IAuthorizationSessionLocalHome authorizationsessionhome = (IAuthorizationSessionLocalHome) getLocator().getLocalHome(IAuthorizationSessionLocalHome.COMP_NAME);
            authorizationsession = authorizationsessionhome.create();

            IRaAdminSessionLocalHome raadminsessionhome = (IRaAdminSessionLocalHome) getLocator().getLocalHome(IRaAdminSessionLocalHome.COMP_NAME);
            raadminsession = raadminsessionhome.create();

            ICertificateStoreSessionLocalHome certificatesessionhome = (ICertificateStoreSessionLocalHome) getLocator().getLocalHome(ICertificateStoreSessionLocalHome.COMP_NAME);
            certificatesession = certificatesessionhome.create();
            
            IKeyRecoverySessionLocalHome keyrecoverysessionhome = (IKeyRecoverySessionLocalHome) getLocator().getLocalHome(IKeyRecoverySessionLocalHome.COMP_NAME);
            keyrecoverysession = keyrecoverysessionhome.create();

        } catch (Exception e) {
            error("Error creating session bean:", e);
            throw new EJBException(e);
        }

    }

    /**
     * Gets the Global Configuration from ra admin session bean-
     */
    private GlobalConfiguration getGlobalConfiguration(Admin admin) {
        return raadminsession.loadGlobalConfiguration(admin);
    }

    private boolean authorizedToCA(Admin admin, int caid) {
        boolean returnval = false;
        try {
            returnval = authorizationsession.isAuthorizedNoLog(admin, AvailableAccessRules.CAPREFIX + caid);
        } catch (AuthorizationDeniedException e) {
        }
        return returnval;
    }

    private boolean authorizedToEndEntityProfile(Admin admin, int profileid, String rights) {
        boolean returnval = false;
        try {
            if (profileid == SecConst.EMPTY_ENDENTITYPROFILE && (rights.equals(AvailableAccessRules.CREATE_RIGHTS) || rights.equals(AvailableAccessRules.EDIT_RIGHTS)))
                returnval = authorizationsession.isAuthorizedNoLog(admin, "/super_administrator");
            else
                returnval = authorizationsession.isAuthorizedNoLog(admin, AvailableAccessRules.ENDENTITYPROFILEPREFIX + profileid + rights);
        } catch (AuthorizationDeniedException e) {
        }
        return returnval;
    }


    /**
     * Implements IUserAdminSession::addUser.
     * Implements a mechanism that uses UserDataEntity Bean.
     *
     * @param admin                 the administrator pwrforming the action
     * @param username              the unique username.
     * @param password              the password used for authentication.
     * @param subjectdn             the DN the subject is given in his certificate.
     * @param subjectaltname        the Subject Alternative Name to be used.
     * @param email                 the email of the subject or null.
     * @param clearpwd              true if the password will be stored in clear form in the db, otherwise it is
     *                              hashed.
     * @param endentityprofileid    the id number of the end entity profile bound to this user.
     * @param certificateprofileid  the id number of the certificate profile that should be
     *                              generated for the user.
     * @param type                  of user i.e administrator, keyrecoverable and/or sendnotification
     * @param tokentype             the type of token to be generated, one of SecConst.TOKEN constants
     * @param hardwaretokenissuerid , if token should be hard, the id of the hard token issuer,
     *                              else 0.
     * @ejb.interface-method
     */
    public void addUser(Admin admin, String username, String password, String subjectdn, String subjectaltname, String email, boolean clearpwd, int endentityprofileid, int certificateprofileid,
                        int type, int tokentype, int hardwaretokenissuerid, int caid)
            throws AuthorizationDeniedException, UserDoesntFullfillEndEntityProfile, DuplicateKeyException {
        // String used in SQL so strip it
        String dn = CertTools.stringToBCDNString(subjectdn);
        dn = StringTools.strip(dn);
        String newpassword = password;
        debug(">addUser(" + username + ", password, " + dn + ", "+ subjectaltname + ", " + email + ")");
        EndEntityProfile profile = raadminsession.getEndEntityProfile(admin, endentityprofileid);

        if (profile.useAutoGeneratedPasswd() && password == null) {
            // special case used to signal regeneraton of password
            newpassword = profile.getAutoGeneratedPasswd();
        }


        if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
            // Check if user fulfills it's profile.
            try {
                profile.doesUserFullfillEndEntityProfile(username, password, dn, subjectaltname, email, certificateprofileid, clearpwd,
                        (type & SecConst.USER_ADMINISTRATOR) != 0, (type & SecConst.USER_KEYRECOVERABLE) != 0, (type & SecConst.USER_SENDNOTIFICATION) != 0,
                        tokentype, hardwaretokenissuerid, caid);
            } catch (UserDoesntFullfillEndEntityProfile udfp) {
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_ADDEDENDENTITY, "Userdata did not fullfill end entity profile. " + udfp.getMessage());
                throw new UserDoesntFullfillEndEntityProfile(udfp.getMessage());
            }

            // Check if administrator is authorized to add user.
            if (!authorizedToEndEntityProfile(admin, endentityprofileid, AvailableAccessRules.CREATE_RIGHTS)) {
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_ADDEDENDENTITY, "Administrator not authorized.");
                throw new AuthorizationDeniedException("Administrator not authorized to create user.");
            }
        }

        // Check if administrator is authorized to add user to CA.
        if (!authorizedToCA(admin, caid)) {
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_ADDEDENDENTITY, "Administrator not authorized to add user to CA.");
            throw new AuthorizationDeniedException("Administrator not authorized to create user with given CA.");
        }

        try {
            UserDataLocal data1 = home.create(username, newpassword, dn, caid);
            if (subjectaltname != null)
                data1.setSubjectAltName(subjectaltname);

            if (email != null)
                data1.setSubjectEmail(email);

            data1.setType(type);
            data1.setEndEntityProfileId(endentityprofileid);
            data1.setCertificateProfileId(certificateprofileid);
            data1.setTokenType(tokentype);
            data1.setHardTokenIssuerId(hardwaretokenissuerid);

            if (clearpwd) {
                try {
                    if (newpassword == null) {
                        data1.setClearPassword("");
                    } else {
                        data1.setOpenPassword(newpassword);
                    }
                } catch (java.security.NoSuchAlgorithmException nsae) {
                    debug("NoSuchAlgorithmException while setting password for user " + username);
                    throw new EJBException(nsae);
                }
            }
            if ((type & SecConst.USER_SENDNOTIFICATION) != 0) {
                sendNotification(admin, profile, username, newpassword, dn, email, caid);
            }
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_INFO_ADDEDENDENTITY, "");

        } catch (DuplicateKeyException e) {
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_ADDEDENDENTITY, "Entity already exists.");
            throw e;
        } catch (Exception e) {
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_ADDEDENDENTITY, e.getMessage());
            error("AddUser:", e);
            throw new EJBException(e);
        }

        debug("<addUser(" + username + ", password, " + dn + ", " + email + ")");
    } // addUser

    /**
     * Changes data for a user in the database speciefied by username.
     *
     * @param username              the unique username.
     * @param password              the password used for authentication.*
     * @param subjectdn             the DN the subject is given in his certificate.
     * @param subjectaltname        the Subject Alternative Name to be used.
     * @param email                 the email of the subject or null.
     * @param endentityprofileid    the id number of the end entity profile bound to this user.
     * @param certificateprofileid  the id number of the certificate profile that should be generated for the user.
     * @param type                  of user i.e administrator, keyrecoverable and/or sendnotification
     * @param tokentype             the type of token to be generated, one of SecConst.TOKEN constants
     * @param hardwaretokenissuerid if token should be hard, the id of the hard token issuer, else 0.
     * @param caid                  the id of the CA that should be used to issue the users certificate
     * @throws EJBException if a communication or other error occurs.
     * @ejb.interface-method
     */
    public void changeUser(Admin admin, String username, String password, String subjectdn, String subjectaltname, String email, boolean clearpwd, int endentityprofileid, int certificateprofileid,
                           int type, int tokentype, int hardwaretokenissuerid, int status, int caid)
            throws AuthorizationDeniedException, UserDoesntFullfillEndEntityProfile {
        // String used in SQL so strip it
        String dn = CertTools.stringToBCDNString(subjectdn);
        dn = StringTools.strip(dn);
        String newpassword = password;
        boolean statuschanged = false;
        debug(">changeUser(" + username + ", " + dn + ", " + email + ")");
        int oldstatus;
        EndEntityProfile profile = raadminsession.getEndEntityProfile(admin, endentityprofileid);

        if (profile.useAutoGeneratedPasswd() && password != null) {
            // special case used to signal regeneraton of password
            newpassword = profile.getAutoGeneratedPasswd();
        }

        // Check if user fulfills it's profile.
        if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
            try {
                profile.doesUserFullfillEndEntityProfileWithoutPassword(username, dn, subjectaltname, email, certificateprofileid,
                        (type & SecConst.USER_ADMINISTRATOR) != 0, (type & SecConst.USER_KEYRECOVERABLE) != 0, (type & SecConst.USER_SENDNOTIFICATION) != 0,
                        tokentype, hardwaretokenissuerid, caid);
            } catch (UserDoesntFullfillEndEntityProfile udfp) {
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, "Userdata didn'nt fullfill end entity profile. + " + udfp.getMessage());
                throw udfp;
            }
            // Check if administrator is authorized to edit user.
            if (!authorizedToEndEntityProfile(admin, endentityprofileid, AvailableAccessRules.EDIT_RIGHTS)) {
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, "Administrator not authorized");
                throw new AuthorizationDeniedException("Administrator not authorized to edit user.");
            }
        }

        // Check if administrator is authorized to edit user to CA.
        if (!authorizedToCA(admin, caid)) {
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, "Administrator not authorized to edit user with this CA.");
            throw new AuthorizationDeniedException("Administrator not authorized to edit user with given CA.");
        }
        try {
            UserDataPK pk = new UserDataPK(username);
            UserDataLocal data1 = home.findByPrimaryKey(pk);
            data1.setDN(dn);
            if (subjectaltname != null)
                data1.setSubjectAltName(subjectaltname);
            if (email != null)
                data1.setSubjectEmail(email);
            data1.setCaId(caid);
            data1.setType(type);
            data1.setEndEntityProfileId(endentityprofileid);
            data1.setCertificateProfileId(certificateprofileid);
            data1.setTokenType(tokentype);
            data1.setHardTokenIssuerId(hardwaretokenissuerid);
            oldstatus = data1.getStatus();
            if(oldstatus == UserDataConstants.STATUS_KEYRECOVERY && !(status == UserDataConstants.STATUS_KEYRECOVERY || status == UserDataConstants.STATUS_INPROCESS)){
              keyrecoverysession.unmarkUser(admin,username);	
            }
            statuschanged = status != oldstatus;
            data1.setStatus(status);
            data1.setTimeModified((new java.util.Date()).getTime());

            if(newpassword != null){
                if(clearpwd) {
                    try {
                        data1.setOpenPassword(newpassword);
                    } catch (java.security.NoSuchAlgorithmException nsae) {
                        debug("NoSuchAlgorithmException while setting password for user "+username);
                        throw new EJBException(nsae);
                    }
                } else {
                    data1.setPassword(newpassword);
                }
            }

            if ((type & SecConst.USER_SENDNOTIFICATION) != 0 && statuschanged && (status == UserDataConstants.STATUS_NEW || status == UserDataConstants.STATUS_KEYRECOVERY || status == UserDataConstants.STATUS_INITIALIZED)) {

                sendNotification(admin, profile, username, newpassword, dn, email, caid);
            }
            if (statuschanged)
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_INFO_CHANGEDENDENTITY, "New status: " + status);
            else
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_INFO_CHANGEDENDENTITY, "");
        } catch (Exception e) {
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, "");
            error("ChangeUser:", e);
            throw new EJBException(e);
        }
        debug("<changeUser(" + username + ", password, " + dn + ", " + email + ")");
    } // changeUser


    /**
     * Deletes a user from the database. The users certificates must be revoked BEFORE this method is called.
     *
     * @param username the unique username.
     * @throws NotFoundException if the user does not exist
     * @throws RemoveException   if the user could not be removed
     * @ejb.interface-method
     */
    public void deleteUser(Admin admin, String username) throws AuthorizationDeniedException, NotFoundException, RemoveException {
        debug(">deleteUser(" + username + ")");
        // Check if administrator is authorized to delete user.
        int caid = LogConstants.INTERNALCAID;
        try {
            UserDataPK pk = new UserDataPK(username);
            UserDataLocal data1 = home.findByPrimaryKey(pk);
            caid = data1.getCaId();

            if (!authorizedToCA(admin, caid)) {
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_DELETEENDENTITY, "Administrator not authorized to delete user with this CA.");
                throw new AuthorizationDeniedException("Administrator not authorized to delete user with given CA.");
            }

            if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
                if (!authorizedToEndEntityProfile(admin, data1.getEndEntityProfileId(), AvailableAccessRules.DELETE_RIGHTS)) {
                    logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_DELETEENDENTITY, "Administrator not authorized");
                    throw new AuthorizationDeniedException("Administrator not authorized to delete user.");
                }
            }
        } catch (FinderException e) {
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_DELETEENDENTITY, "Could not find username in database");
            throw new NotFoundException("Could not find '" + username + "' in database");
        }
        try {
            UserDataPK pk = new UserDataPK(username);
            home.remove(pk);
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_INFO_DELETEDENDENTITY, "");
        } catch (EJBException e) {
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_DELETEENDENTITY, "Could not remove user from database");
            throw new RemoveException("Could not remove '" + username + "' from database");
        }
        debug("<deleteUser(" + username + ")");
    } // deleteUser

    /**
     * Changes status of a user.
     *
     * @param username the unique username.
     * @param status   the new status, from 'UserData'.
     * @ejb.interface-method
     */
    public void setUserStatus(Admin admin, String username, int status) throws AuthorizationDeniedException, FinderException {
        debug(">setUserStatus(" + username + ", " + status + ")");
        // Check if administrator is authorized to edit user.
        int caid = LogConstants.INTERNALCAID;
        try {
            UserDataPK pk = new UserDataPK(username);
            UserDataLocal data1 = home.findByPrimaryKey(pk);
            caid = data1.getCaId();

            if (!authorizedToCA(admin, caid)) {
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, "Administrator not authorized to change status of user with current CA.");
                throw new AuthorizationDeniedException("Administrator not authorized to set status to user with given CA.");
            }


            if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
                if (!authorizedToEndEntityProfile(admin, data1.getEndEntityProfileId(), AvailableAccessRules.EDIT_RIGHTS)) {
                    logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, "Administrator not authorized to change status");
                    throw new AuthorizationDeniedException("Administrator not authorized to edit user.");
                }
            }

            if(data1.getStatus() == UserDataConstants.STATUS_KEYRECOVERY && !(status == UserDataConstants.STATUS_KEYRECOVERY || status == UserDataConstants.STATUS_INPROCESS || status == UserDataConstants.STATUS_INITIALIZED)){
                keyrecoverysession.unmarkUser(admin,username);	
            }
            
            data1.setStatus(status);
            data1.setTimeModified((new java.util.Date()).getTime());
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_INFO_CHANGEDENDENTITY, ("New status : " + status));

        } catch (FinderException e) {
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, "Couldn't find username in database.");
            throw e;
        }

        debug("<setUserStatus(" + username + ", " + status + ")");
    } // setUserStatus

    /**
     * Sets a new password for a user.
     *
     * @param admin    the administrator pwrforming the action
     * @param username the unique username.
     * @param password the new password for the user, NOT null.
     * @ejb.interface-method
     */
    public void setPassword(Admin admin, String username, String password) throws UserDoesntFullfillEndEntityProfile, AuthorizationDeniedException, FinderException {
        setPassword(admin, username, password, false);
    } // setPassword

    /**
     * Sets a clear text password for a user.
     *
     * @param admin    the administrator pwrforming the action
     * @param username the unique username.
     * @param password the new password to be stored in clear text. Setting password to 'null'
     *                 effectively deletes any previous clear text password.
     * @ejb.interface-method
     */
    public void setClearTextPassword(Admin admin, String username, String password) throws UserDoesntFullfillEndEntityProfile, AuthorizationDeniedException, FinderException {
        setPassword(admin, username, password, true);
    } // setClearTextPassword

    /**
     * Sets a password, hashed or clear text, for a user.
     *
     * @param admin     the administrator pwrforming the action
     * @param username  the unique username.
     * @param password  the new password to be stored in clear text. Setting password to 'null'
     *                  effectively deletes any previous clear text password.
     * @param cleartext true gives cleartext password, false hashed
     */
    private void setPassword(Admin admin, String username, String password, boolean cleartext) throws UserDoesntFullfillEndEntityProfile, AuthorizationDeniedException, FinderException {
        debug(">setPassword(" + username + ", hiddenpwd), " + cleartext);
        // Find user
        String newpasswd = password;
        UserDataPK pk = new UserDataPK(username);
        UserDataLocal data = home.findByPrimaryKey(pk);
        int caid = data.getCaId();

        EndEntityProfile profile = raadminsession.getEndEntityProfile(admin, data.getEndEntityProfileId());

        if (profile.useAutoGeneratedPasswd())
            newpasswd = profile.getAutoGeneratedPasswd();

        if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
            // Check if user fulfills it's profile.
            try {
                profile.doesPasswordFulfillEndEntityProfile(password, true);
            } catch (UserDoesntFullfillEndEntityProfile ufe) {
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, "Clearpassword didn't fullfill end entity profile.");
                throw ufe;
            }

            // Check if administrator is authorized to edit user.
            if (!authorizedToEndEntityProfile(admin, data.getEndEntityProfileId(), AvailableAccessRules.EDIT_RIGHTS)) {
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, "Administrator isn't authorized to change clearpassword.");
                throw new AuthorizationDeniedException("Administrator not authorized to edit user.");
            }
        }

        if (!authorizedToCA(admin, caid)) {
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, "Administrator not authorized to change password of user with current CA.");
            throw new AuthorizationDeniedException("Administrator not authorized to set cleartext password to user with given CA.");
        }

        try {
            if ((newpasswd == null) && (cleartext)) {
                data.setClearPassword("");
                data.setTimeModified((new java.util.Date()).getTime());
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_INFO_CHANGEDENDENTITY, "Clearpassword changed.");
            } else {
                if (cleartext) {
                    data.setOpenPassword(newpasswd);
                } else {
                    data.setPassword(newpasswd);
                }
                data.setTimeModified((new java.util.Date()).getTime());
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_INFO_CHANGEDENDENTITY, "Clearpassword changed.");
            }
        } catch (java.security.NoSuchAlgorithmException nsae) {
            debug("NoSuchAlgorithmException while setting password for user " + username);
            throw new EJBException(nsae);
        }
        debug("<setPassword(" + username + ", hiddenpwd), " + cleartext);
    } // setPassword

    /**
     * Verifies a password for a user.
     *
     * @param admin    the administrator pwrforming the action
     * @param username the unique username.
     * @param password the password to be verified.
     * @ejb.interface-method
     */
    public boolean verifyPassword(Admin admin, String username, String password) throws UserDoesntFullfillEndEntityProfile, AuthorizationDeniedException, FinderException {
        debug(">verifyPassword(" + username + ", hiddenpwd)");
        boolean ret = false;
        // Find user
        UserDataPK pk = new UserDataPK(username);
        UserDataLocal data = home.findByPrimaryKey(pk);
        int caid = data.getCaId();

        if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
            // Check if administrator is authorized to edit user.
            if (!authorizedToEndEntityProfile(admin, data.getEndEntityProfileId(), AvailableAccessRules.EDIT_RIGHTS)) {
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, "Administrator isn't authorized to verify password.");
                throw new AuthorizationDeniedException("Administrator not authorized to verify user.");
            }
        }

        if (!authorizedToCA(admin, caid)) {
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_CHANGEDENDENTITY, "Administrator not authorized to verify password of user with current CA.");
            throw new AuthorizationDeniedException("Administrator not authorized to verify password for user with given CA.");
        }

        try {
            ret = data.comparePassword(password);
        } catch (java.security.NoSuchAlgorithmException nsae) {
            debug("NoSuchAlgorithmException while verifying password for user " + username);
            throw new EJBException(nsae);
        }
        debug("<verifyPassword(" + username + ", hiddenpwd)");
        return ret;
    } // verifyPassword

    /**
     * Method that revokes a user.
     *
     * @param username the username to revoke.
     * @ejb.interface-method
     */
    public void revokeUser(Admin admin, String username, int reason) throws AuthorizationDeniedException, FinderException {
        debug(">revokeUser(" + username + ")");
        UserDataPK pk = new UserDataPK(username);
        UserDataLocal data;
        try {
            data = home.findByPrimaryKey(pk);
        } catch (ObjectNotFoundException oe) {
            throw new EJBException(oe);
        }

        int caid = data.getCaId();
        if (!authorizedToCA(admin, caid)) {
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_REVOKEDENDENTITY, "Administrator not authorized to revoke user with given CA.");
            throw new AuthorizationDeniedException("Administrator not authorized to revoke user with given CA.");
        }

        if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
            if (!authorizedToEndEntityProfile(admin, data.getEndEntityProfileId(), AvailableAccessRules.REVOKE_RIGHTS)) {
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_REVOKEDENDENTITY, "Administrator not authorized");
                throw new AuthorizationDeniedException("Not authorized to revoke user : " + username + ".");
            }
        }

        Collection publishers = this.certificatesession.getCertificateProfile(admin, data.getCertificateProfileId()).getPublisherList();
        setUserStatus(admin, username, UserDataConstants.STATUS_REVOKED);
        certificatesession.setRevokeStatus(admin, username, publishers, reason);
        logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_INFO_REVOKEDENDENTITY, "");
        debug("<revokeUser()");
    } // revokeUser

    /**
     * Method that revokes a certificate.
     *
     * @param certserno the serno of certificate to revoke.
     * @param username  the username to revoke.
     * @param reason    the reason of revokation.
     * @ejb.interface-method
     */
    public void revokeCert(Admin admin, BigInteger certserno, String issuerdn, String username, int reason) throws AuthorizationDeniedException, FinderException {
        debug(">revokeCert(" + certserno + ", IssuerDN: " + issuerdn + ", username, " + username + ")");
        UserDataPK pk = new UserDataPK(username);
        UserDataLocal data;
        try {
            data = home.findByPrimaryKey(pk);
        } catch (ObjectNotFoundException oe) {
            throw new EJBException(oe);
        }

        int caid = data.getCaId();
        if (!authorizedToCA(admin, caid)) {
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_REVOKEDENDENTITY, "Administrator not authorized to revoke certificates of this CA.");
            throw new AuthorizationDeniedException("Administrator not authorized to revoke certificate of user with given CA.");
        }

        if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
            if (!authorizedToEndEntityProfile(admin, data.getEndEntityProfileId(), AvailableAccessRules.REVOKE_RIGHTS)) {
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_ERROR_REVOKEDENDENTITY, "Administrator not authorized");
                throw new AuthorizationDeniedException("Not authorized to revoke user : " + username + ".");
            }
        }
        Collection publishers = this.certificatesession.getCertificateProfile(admin, data.getCertificateProfileId()).getPublisherList();
        // revoke certificate in database and all publishers
        certificatesession.setRevokeStatus(admin, issuerdn, certserno, publishers, reason);

        if (certificatesession.checkIfAllRevoked(admin, username)) {
            setUserStatus(admin, username, UserDataConstants.STATUS_REVOKED);
            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_INFO_REVOKEDENDENTITY, "");
        }
        debug("<revokeCert()");
    } // revokeCert

    /**
     * Finds a user.
     *
     * @param admin the administrator performing the action
     * @param username username.
     * @return UserDataVO or null if the user is not found.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public UserDataVO findUser(Admin admin, String username) throws FinderException, AuthorizationDeniedException {
        debug(">findUser(" + username + ")");
        UserDataPK pk = new UserDataPK(username);
        UserDataLocal data;
        try {
            data = home.findByPrimaryKey(pk);
        } catch (ObjectNotFoundException oe) {
            return null;
        }

        if (!authorizedToCA(admin, data.getCaId())) {
            throw new AuthorizationDeniedException("Administrator not authorized to view user with given CA.");
        }

        if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
            // Check if administrator is authorized to view user.
            if (!authorizedToEndEntityProfile(admin, data.getEndEntityProfileId(), AvailableAccessRules.VIEW_RIGHTS))
                throw new AuthorizationDeniedException("Administrator not authorized to view user.");
        }

        UserDataVO ret = new UserDataVO(data.getUsername(), data.getSubjectDN(), data.getCaId(), data.getSubjectAltName(), data.getSubjectEmail(), data.getStatus()
                , data.getType(), data.getEndEntityProfileId(), data.getCertificateProfileId()
                , new java.util.Date(data.getTimeCreated()), new java.util.Date(data.getTimeModified())
                , data.getTokenType(), data.getHardTokenIssuerId(), data.getExtendedInformation());
        ret.setPassword(data.getClearPassword());
        debug("<findUser(" + username + ")");
        return ret;
    } // findUser

    /**
     * Finds a user by its subjectDN.
     *
     * @param subjectdn
     * @return UserDataVO or null if the user is not found.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public UserDataVO findUserBySubjectDN(Admin admin, String subjectdn, String issuerdn) throws AuthorizationDeniedException {
        debug(">findUserBySubjectDN(" + subjectdn + ")");
        String bcdn = CertTools.stringToBCDNString(subjectdn);
        // String used in SQL so strip it
        String dn = StringTools.strip(bcdn);
        debug("Looking for users with subjectdn: " + dn + ", issuerdn : " + issuerdn);
        UserDataVO returnval = null;

        UserDataLocal data = null;

        if (!authorizedToCA(admin, issuerdn.hashCode())) {
            throw new AuthorizationDeniedException("Administrator not authorized to view user with given CA.");
        }

        try {
            data = home.findBySubjectDN(dn, issuerdn.hashCode());
        } catch (FinderException e) {
            log.debug("Cannot find user with DN='" + dn + "'");
        }
        if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
            // Check if administrator is authorized to view user.
            if (!authorizedToEndEntityProfile(admin, data.getEndEntityProfileId(), AvailableAccessRules.VIEW_RIGHTS))
                throw new AuthorizationDeniedException("Administrator not authorized to view user.");
        }

        if (data != null) {
            returnval = new UserDataVO(data.getUsername(), data.getSubjectDN(), data.getCaId(), data.getSubjectAltName(), data.getSubjectEmail(), data.getStatus()
                    , data.getType(), data.getEndEntityProfileId(), data.getCertificateProfileId()
                    , new java.util.Date(data.getTimeCreated()), new java.util.Date(data.getTimeModified())
                    , data.getTokenType(), data.getHardTokenIssuerId(), data.getExtendedInformation());

            returnval.setPassword(data.getClearPassword());
        }
        debug("<findUserBySubjectDN(" + subjectdn + ")");
        return returnval;
    } // findUserBySubjectDN

    /**
     * Finds a user by its Email.
     *
     * @param email
     * @return UserDataVO or null if the user is not found.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public Collection findUserByEmail(Admin admin, String email) throws AuthorizationDeniedException {
        debug(">findUserByEmail(" + email + ")");
        debug("Looking for user with email: " + email);
        ArrayList returnval = new ArrayList();

        Collection result = null;
        try {
            result = home.findBySubjectEmail(email);
        } catch (FinderException e) {
            log.debug("Cannot find user with Email='" + email + "'");
        }

        Iterator iter = result.iterator();
        while (iter.hasNext()) {
            UserDataLocal data = (UserDataLocal) iter.next();

            if (getGlobalConfiguration(admin).getEnableEndEntityProfileLimitations()) {
                // Check if administrator is authorized to view user.
                if (!authorizedToEndEntityProfile(admin, data.getEndEntityProfileId(), AvailableAccessRules.VIEW_RIGHTS))
                    break;
            }

            if (!authorizedToCA(admin, data.getCaId())) {
                break;
            }

            UserDataVO user = new UserDataVO(data.getUsername(), data.getSubjectDN(), data.getCaId(), data.getSubjectAltName(), data.getSubjectEmail(), data.getStatus()
                    , data.getType(), data.getEndEntityProfileId(), data.getCertificateProfileId()
                    , new java.util.Date(data.getTimeCreated()), new java.util.Date(data.getTimeModified())
                    , data.getTokenType(), data.getHardTokenIssuerId(), data.getExtendedInformation());
            user.setPassword(data.getClearPassword());
            returnval.add(user);
        }
        debug("<findUserByEmail(" + email + ")");
        return returnval;
    } // findUserBySubjectDN

    /**
     * Method that checks if user with specified users certificate exists in database and is set as administrator.
     *
     * @param subjectdn
     * @throws AuthorizationDeniedException if user isn't an administrator.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public void checkIfCertificateBelongToAdmin(Admin admin, BigInteger certificatesnr, String issuerdn) throws AuthorizationDeniedException {
        debug(">checkIfCertificateBelongToAdmin(" + certificatesnr + ")");
        String username = certificatesession.findUsernameByCertSerno(admin, certificatesnr, issuerdn);

        UserDataLocal data = null;
        if (username != null) {
            UserDataPK pk = new UserDataPK(username);
            try {
                data = home.findByPrimaryKey(pk);
            } catch (FinderException e) {
                log.debug("Cannot find user with username='" + username + "'");
            }
        }

        if (data != null) {
            int type = data.getType();
            if ((type & SecConst.USER_ADMINISTRATOR) == 0) {
                logsession.log(admin, data.getCaId(), LogEntry.MODULE_RA, new java.util.Date(), null, null, LogEntry.EVENT_ERROR_ADMINISTRATORLOGGEDIN, "Certificate didn't belong to an administrator.");
                throw new AuthorizationDeniedException("Your certificate does not belong to an administrator.");
            }
        } else {
            logsession.log(admin, LogConstants.INTERNALCAID, LogEntry.MODULE_RA, new java.util.Date(), null, null, LogEntry.EVENT_ERROR_ADMINISTRATORLOGGEDIN, "Certificate didn't belong to any user.");
            throw new AuthorizationDeniedException("Your certificate does not belong to any user.");
        }

        debug("<checkIfCertificateBelongToAdmin()");
    } // checkIfCertificateBelongToAdmin


    /**
     * Finds all users with a specified status.
     *
     * @param status the status to look for, from 'UserData'.
     * @return Collection of UserDataVO
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public Collection findAllUsersByStatus(Admin admin, int status) throws FinderException {
        debug(">findAllUsersByStatus(" + status + ")");
        debug("Looking for users with status: " + status);

        Query query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_STATUS, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(status));
        Collection returnval = null;

        try {
            returnval = query(admin, query, false, null, null, false);
        } catch (IllegalQueryException e) {
        }
        debug("found " + returnval.size() + " user(s) with status=" + status);
        debug("<findAllUsersByStatus(" + status + ")");
        return returnval;
    }
    /**
     * Finds all users registered to a specified ca.
     *
     * @param caid the caid of the CA, from 'UserData'.
     * @return Collection of UserDataVO
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
     public Collection findAllUsersByCaId(Admin admin, int caid) throws FinderException {
         debug(">findAllUsersByCaId("+caid+")");
         debug("Looking for users with caid: " + caid);
         
         Query query = new Query(Query.TYPE_USERQUERY);
         query.add(UserMatch.MATCH_WITH_CA, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(caid));
         Collection returnval = null;
         
         try{
           returnval = query(admin, query, false, null, null, false);  
         }catch(IllegalQueryException e){}
         debug("found "+returnval.size()+" user(s) with caid="+caid);
         debug("<findAllUsersByCaId("+caid+")");
         return returnval;         
     }


    /**
     * Finds all users and returns the first MAXIMUM_QUERY_ROWCOUNT.
     *
     * @return Collection of UserDataVO
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public Collection findAllUsersWithLimit(Admin admin) throws FinderException {
        debug(">findAllUsersWithLimit()");
        Collection returnval = null;
        try {
            returnval = query(admin, null, true, null, null, false);
        } catch (IllegalQueryException e) {
        }
        debug("<findAllUsersWithLimit()");
        return returnval;
    }

    /**
     * Finds all users with a specified status and returns the first MAXIMUM_QUERY_ROWCOUNT.
     *
     * @param status the new status, from 'UserData'.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public Collection findAllUsersByStatusWithLimit(Admin admin, int status, boolean onlybatchusers) throws FinderException {
        debug(">findAllUsersByStatusWithLimit()");

        Query query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_STATUS, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(status));
        Collection returnval = null;

        try {
            returnval = query(admin, query, false, null, null, onlybatchusers);
        } catch (IllegalQueryException e) {
        }

        debug("<findAllUsersByStatusWithLimit()");
        return returnval;
    }

    /**
     * Starts an external service that may be needed bu user administration.
     *
     * @ejb.interface-method
     */
    public void startExternalService(String[] args) {
        debug(">startService()");
        try {
            String className = getLocator().getString("java:comp/env/RMIFactory");
            RMIFactory rmiFactory = (RMIFactory) Class.forName(className).newInstance();
            rmiFactory.startConnection(args);
            debug(">startService()");
        } catch (Exception e) {
            error("Error starting external service.", e);
            throw new EJBException("Error starting external service", e);
        }
    } // startExternalService

    /**
     * Method to execute a customized query on the ra user data. The parameter query should be a legal Query object.
     *
     * @param query                  a number of statments compiled by query class to a SQL 'WHERE'-clause statment.
     * @param caauthorizationstring  is a string placed in the where clause of SQL query indication which CA:s the administrator is authorized to view.
     * @param endentityprofilestring is a string placed in the where clause of SQL query indication which endentityprofiles the administrator is authorized to view.
     * @return a collection of UserDataVO. Maximum size of Collection is defined i IUserAdminSessionRemote.MAXIMUM_QUERY_ROWCOUNT
     * @throws IllegalQueryException when query parameters internal rules isn't fullfilled.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     * @see se.anatom.ejbca.util.query.Query
     */
    public Collection query(Admin admin, Query query, String caauthorizationstring, String endentityprofilestring) throws IllegalQueryException {
        return query(admin, query, true, caauthorizationstring, endentityprofilestring, false);
    }

    /**
     * Help function used to retrieve user information. A query parameter of null indicates all users.
     * If caauthorizationstring or endentityprofilestring are null then the method will retrieve the information
     * itself.
     */
    private Collection query(Admin admin, Query query, boolean withlimit, String caauthorizationstr, String endentityprofilestr, boolean onlybatchusers) throws IllegalQueryException {
        debug(">query(): withlimit(not implemented)="+withlimit);
        boolean authorizedtoanyprofile = true;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String caauthorizationstring = StringTools.strip(caauthorizationstr);
        String endentityprofilestring = StringTools.strip(endentityprofilestr);
        ArrayList returnval = new ArrayList();
        GlobalConfiguration globalconfiguration = getGlobalConfiguration(admin);
        RAAuthorization raauthorization = null;
        String caauthstring = caauthorizationstring;
        String endentityauth = endentityprofilestring;
        String sqlquery = "select " + USERDATA_COL + " from UserData where ";


        // Check if query is legal.
        if (query != null && !query.isLegalQuery())
            throw new IllegalQueryException();

        if (query != null)
            sqlquery = sqlquery + query.getQueryString();

        if (caauthorizationstring == null || endentityprofilestring == null) {
            raauthorization = new RAAuthorization(admin, raadminsession, authorizationsession);
            caauthstring = raauthorization.getCAAuthorizationString();
            if (globalconfiguration.getEnableEndEntityProfileLimitations())
                endentityauth = raauthorization.getEndEntityProfileAuthorizationString();
            else
                endentityauth = "";
        }

        if (!caauthstring.trim().equals("") && query != null)
            sqlquery = sqlquery + " AND " + caauthstring;
        else
            sqlquery = sqlquery + caauthstring;


        if (globalconfiguration.getEnableEndEntityProfileLimitations()) {
            if (caauthstring.trim().equals("") && query == null)
                sqlquery = sqlquery + endentityauth;
            else
                sqlquery = sqlquery + " AND " + endentityauth;

            if (endentityauth == null || endentityauth.trim().equals("")) {
                authorizedtoanyprofile = false;
            }
        }

        try {
            if (authorizedtoanyprofile) {
                // Construct SQL query.
                con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);

                ps = con.prepareStatement(sqlquery);

                // Execute query.
                rs = ps.executeQuery();

                // Assemble result.
                while (rs.next() && returnval.size() <= UserAdminConstants.MAXIMUM_QUERY_ROWCOUNT) {
                	// TODO add support for extended information.
                    UserDataVO data = new UserDataVO(rs.getString(1), rs.getString(2), rs.getInt(14), rs.getString(3), rs.getString(4), rs.getInt(5), rs.getInt(6)
                            , rs.getInt(10), rs.getInt(11), new java.util.Date(rs.getLong(8)), new java.util.Date(rs.getLong(9)), rs.getInt(12), rs.getInt(13),
							null);
                    data.setPassword(rs.getString(7));

                    if (!onlybatchusers || (data.getPassword() != null && data.getPassword().length() > 0))
                        returnval.add(data);
                }
            }
            debug("<query()");
            return returnval;

        } catch (Exception e) {
            throw new EJBException(e);
        } finally {
            JDBCUtil.close(con, ps, rs);
        }

    } // query

    /**
     * Methods that checks if a user exists in the database having the given endentityprofileid. This function is mainly for avoiding
     * desyncronisation when a end entity profile is deleted.
     *
     * @param endentityprofileid the id of end entity profile to look for.
     * @return true if endentityprofileid exists in userdatabase.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public boolean checkForEndEntityProfileId(Admin admin, int endentityprofileid) {
        debug(">checkForEndEntityProfileId()");
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int count = 1; // return true as default.

        Query query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_ENDENTITYPROFILE, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(endentityprofileid));

        try {
            // Construct SQL query.
            con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
            ps = con.prepareStatement("select COUNT(*) from UserData where " + query.getQueryString());
            // Execute query.
            rs = ps.executeQuery();
            // Assemble result.
            if (rs.next()) {
                count = rs.getInt(1);
            }
            debug("<checkForEndEntityProfileId()");
            return count > 0;

        } catch (Exception e) {
            throw new EJBException(e);
        } finally {
            JDBCUtil.close(con, ps, rs);
        }


    }

    /**
     * Methods that checks if a user exists in the database having the given certificateprofileid. This function is mainly for avoiding
     * desyncronisation when a certificateprofile is deleted.
     *
     * @param certificateprofileid the id of certificateprofile to look for.
     * @return true if certificateproileid exists in userdatabase.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public boolean checkForCertificateProfileId(Admin admin, int certificateprofileid) {
        debug(">checkForCertificateProfileId()");
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int count = 1; // return true as default.

        Query query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_CERTIFICATEPROFILE, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(certificateprofileid));

        try {
            // Construct SQL query.
            con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
            ps = con.prepareStatement("select COUNT(*) from UserData where " + query.getQueryString());
            // Execute query.
            rs = ps.executeQuery();
            // Assemble result.
            if (rs.next()) {
                count = rs.getInt(1);
            }
            debug("<checkForCertificateProfileId()");
            return count > 0;

        } catch (Exception e) {
            throw new EJBException(e);
        } finally {
            JDBCUtil.close(con, ps, rs);
        }
    } // checkForCertificateProfileId

    /**
     * Methods that checks if a user exists in the database having the given caid. This function is mainly for avoiding
     * desyncronisation when a CAs is deleted.
     *
     * @param caid the id of CA to look for.
     * @return true if caid exists in userdatabase.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public boolean checkForCAId(Admin admin, int caid) {
        debug(">checkForCAId()");
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int count = 1; // return true as default.

        Query query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_CA, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(caid));

        try {
            // Construct SQL query.
            con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
            ps = con.prepareStatement("select COUNT(*) from UserData where " + query.getQueryString());
            // Execute query.
            rs = ps.executeQuery();
            // Assemble result.
            if (rs.next()) {
                count = rs.getInt(1);
            }
            debug("<checkForCAId()");
            return count > 0;

        } catch (Exception e) {
            throw new EJBException(e);
        } finally {
            JDBCUtil.close(con, ps, rs);
        }
    } // checkForCAId


    /**
     * Methods that checks if a user exists in the database having the given hard token profile id. This function is mainly for avoiding
     * desyncronisation when a hard token profile is deleted.
     *
     * @param profileid of hardtokenprofile to look for.
     * @return true if proileid exists in userdatabase.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public boolean checkForHardTokenProfileId(Admin admin, int profileid) {
        debug(">checkForHardTokenProfileId()");
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int count = 1; // return true as default.

        Query query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_TOKEN, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(profileid));

        try {
            // Construct SQL query.
            con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
            ps = con.prepareStatement("select COUNT(*) from UserData where " + query.getQueryString());
            // Execute query.
            rs = ps.executeQuery();
            // Assemble result.
            if (rs.next()) {
                count = rs.getInt(1);
            }
            debug("<checkForHardTokenProfileId()");
            return count > 0;

        } catch (Exception e) {
            throw new EJBException(e);
        } finally {
            JDBCUtil.close(con, ps, rs);
        }
    } // checkForHardTokenProfileId


    private void sendNotification(Admin admin, EndEntityProfile profile, String username, String password, String dn, String email, int caid) {
        debug(">sendNotification: user="+username+", email="+email);
        try {
            if (email == null) {
                throw new Exception("Notification cannot be sent to user where email field is null");
            }

            String mailJndi = getLocator().getString("java:comp/env/MailJNDIName");
            Session mailSession = getLocator().getMailSession(mailJndi);
            DNFieldExtractor dnfields = new DNFieldExtractor(dn, DNFieldExtractor.TYPE_SUBJECTDN);
            HashMap params = new HashMap();
            params.put("USERNAME", username);
            params.put("PASSWORD", password);
            params.put("CN", dnfields.getField(DNFieldExtractor.CN, 0));
            params.put("O", dnfields.getField(DNFieldExtractor.O, 0));
            params.put("OU", dnfields.getField(DNFieldExtractor.OU, 0));
            params.put("C", dnfields.getField(DNFieldExtractor.C, 0));
            params.put("NL", System.getProperty("line.separator"));
            String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date());
            params.put("DATE", date);

            Message msg = new TemplateMimeMessage(params, mailSession);
            msg.setFrom(new InternetAddress(profile.getNotificationSender()));
            msg.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(email, false));
            msg.setSubject(profile.getNotificationSubject());
            msg.setContent(profile.getNotificationMessage(), "text/plain");
            msg.setHeader("X-Mailer", "JavaMailer");
            msg.setSentDate(new Date());
            Transport.send(msg);

            logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(), username, null, LogEntry.EVENT_INFO_NOTIFICATION, "Notification to " + email + " sent successfully.");
        } catch (Exception e) {
            error("Error when sending notification to " + email, e);
            try{
                logsession.log(admin, caid, LogEntry.MODULE_RA, new java.util.Date(),username, null, LogEntry.EVENT_ERROR_NOTIFICATION, "Error when sending notification to " + email );
            }catch(Exception f){
                throw new EJBException(f);
            }
        }
        debug("<sendNotification: user="+username+", email="+email);
    } // sendNotification

    /**
     * Method checking if username already exists in database.
     *
     * @return true if username already exists.
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public boolean existsUser(Admin admin, String username) {
        boolean returnval = true;

        try {
            home.findByPrimaryKey(new UserDataPK(username));
        } catch (FinderException fe) {
            returnval = false;
        }

        return returnval;
    }

} // LocalUserAdminSessionBean
