/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.cesecore.keys;

/**
 * This exception is thrown when an error is encountered when trying to create a key.
 * 
 * Based on KeyCreationException.java 124 2011-01-20 14:41:21Z tomas
 * 
 * @version $Id$
 *
 */
public class KeyCreationException extends RuntimeException {

    private static final long serialVersionUID = 6589133117806842102L;

    public KeyCreationException() {

    }

    public KeyCreationException(String arg0) {
        super(arg0);
    }

    public KeyCreationException(Throwable arg0) {
        super(arg0);
    }

    public KeyCreationException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

}
