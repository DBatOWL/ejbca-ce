package se.anatom.ejbca.authorization;

/**
 * For docs, see AccessRulesDataBean
 **/

public interface AuthorizationTreeUpdateDataLocal extends javax.ejb.EJBLocalObject {
    // public methods

    /** 
     * Method used check if a reconstruction of authorization tree is needed in the
     * authorization beans. It is used to avoid desyncronisation of authorization structures 
     * in a distibuted environment.
     *
     * @param currrentauthorizationtreeupdatenumber indicates which authorizationtreeupdatenumber is currently used.
     * @returns true if update is needed.
     */
    
    public boolean updateNeccessary(int currentauthorizationtreeupdatenumber);

    
    /** 
     * Method returning the newest authorizationtreeupdatenumber. Should be used after each 
     * time the authorization tree is built. 
     *
     * @returns the newest accessruleset number.
     */

    public int getAuthorizationTreeUpdateNumber();
    
    /** 
     * Method incrementing the authorizationtreeupdatenumber and thereby signaling 
     * to other beans that they should reconstruct their accesstrees. 
     *
     */    
    
    public void incrementAuthorizationTreeUpdateNumber();
    
}

