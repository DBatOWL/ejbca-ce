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
package org.cesecore.configuration;

// To allow future expansion
public class GdprConfiguration {
    
    private boolean redactSubjectDnAndSan;

    public GdprConfiguration(boolean redactSubjectDnAndSan) {
        this.setRedactSubjectDnAndSan(redactSubjectDnAndSan);
    }

    public boolean isRedactSubjectDnAndSan() {
        return redactSubjectDnAndSan;
    }

    public void setRedactSubjectDnAndSan(boolean redactSubjectDnAndSan) {
        this.redactSubjectDnAndSan = redactSubjectDnAndSan;
    }

    @Override
    public String toString() {
        return "GdprConfiguration [redactSubjectDnAndSan=" + redactSubjectDnAndSan + "]";
    }
    
}
