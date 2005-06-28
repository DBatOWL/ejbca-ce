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
 
package se.anatom.ejbca.admin;


/**
 * Implements the CA command line interface
 *
 * @version $Id: ca.java,v 1.40.2.1 2005-06-28 13:03:14 herrvendil Exp $
 */
public class ca extends BaseCommand {
    /**
     * Main
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            IAdminCommand cmd = CaAdminCommandFactory.getCommand(args);

            if (cmd != null) {
                cmd.execute();
            } else {
                System.out.println(
                    "Usage: CA info | init | listcas | getrootcert | createcrl | getcrl |  listexpired | exportprofiles | importprofiles | importca | importcert | republish");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());            
            //e.printStackTrace();
            System.exit(-1);
        }
    }
}


//ca
