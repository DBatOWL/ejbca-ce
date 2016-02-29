/*************************************************************************
 *                                                                       *
 *  EJBCA - Proprietary Modules: Enterprise Certificate Authority        *
 *                                                                       *
 *  Copyright (c), PrimeKey Solutions AB. All rights reserved.           *
 *  The use of the Proprietary Modules are subject to specific           * 
 *  commercial license terms.                                            *
 *                                                                       *
 *************************************************************************/

package se.primekey.ejbca.autoenroll;

/**
 * @author Daniel Horn, SiO2 Corp.
 * 
 * @version $Id$
 */
public class PKCS10Info
{

    private String certificateTemplateName;
    private String certificateTemplateOid;

    public PKCS10Info()
    {
    }

    public void setCertificateTemplateName(String certificateTemplateName)
    {
        this.certificateTemplateName = certificateTemplateName;
    }

    public void setCertificateTemplateOid(String certificateTemplateOid)
    {
        // Remove leading tabs and surrounding brackets that may appear in the string.
        int index = certificateTemplateOid.indexOf(']');
        String temp = certificateTemplateOid.substring(0, index);
        temp = temp.replace("\t", "");
        temp = temp.replace("[", "");

        this.certificateTemplateOid = temp;
    }

    public String getCertificateTemplateName()
    {
        return certificateTemplateName;
    }

    public String getCertificateTemplateOid()
    {
        return certificateTemplateOid;
    }

    String getEJBCACertificateProfileName(MSTemplateToEJBCAProfileMap map)
    {
        String retval = null;

        if (null != this.certificateTemplateOid)
        {
            retval = map.getProperty(this.certificateTemplateOid, null);
        }

        if (null == retval)
        {
            if (null != this.certificateTemplateName)
            {
                retval = map.getProperty(this.certificateTemplateName, null);
            }
        }

        return retval;
    }

    @Override
    public String toString()
    {
        return "PKCS10Info:\n\tTemplate Name: [" + certificateTemplateName + "]\n\tTemplate OID: [" + certificateTemplateOid + "]";
    }

    public String toMessage()
    {
        StringBuilder sb = new StringBuilder("PKCS10 Request with certificate template ");
        if (null != certificateTemplateName)
        {
            sb.append(certificateTemplateName);
        }
        else if (null != certificateTemplateOid)
        {
            sb.append(certificateTemplateOid);
        }

        return sb.toString();
    }
}
