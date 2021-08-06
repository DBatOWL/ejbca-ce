/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.core.model.ca.publisher;

import java.io.Serializable;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.Validate;

/**
 * Helper class for UIs that want to present a nice view of the configurable properties of a Custom Publisher.
 * 
 * All properties are interpreted as String values.
 * 
 * @version $Id$
 */
public class CustomPublisherProperty implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int UI_TEXTINPUT = 0;
    public static final int UI_SELECTONE = 1;
    public static final int UI_BOOLEAN   = 2;
    public static final int UI_TEXTOUTPUT = 3;
    public static final int UI_TEXTINPUT_PASSWORD = 4;

    private final String name;
    private final int type;
    private final List<String> options;
    private final List<String> optionTexts;
    String value;
    
    // unique ID to identify this control
    private String enabledGroupId = UUID.randomUUID().toString();
    
    // enable this control when the control identified by <key> has <value>, disable otherwise
    private final HashMap<String, HashSet<String>> enabledWhenControlHasValue = new HashMap<>();

    /** set this control to only be enabled when controllingControl has value = value */
    public void enableWhenControlHasValue(final CustomPublisherProperty controllingControl, final String value) {
        if (!enabledWhenControlHasValue.containsKey(controllingControl.enabledGroupId)) {
            enabledWhenControlHasValue.put(controllingControl.enabledGroupId, new HashSet<>());
        }
        enabledWhenControlHasValue.get(controllingControl.enabledGroupId).add(Base64.getEncoder().encodeToString(value.getBytes()));
    }
    
    /**
     * Representation of a property where the user can select from a list of choices.
     *  
     * @param name name of the property
     * @param type one of CustomPublisherProperty.UI_* constants (only UI_SELECTONE makes sense in the current implementation)
     * @param options a list of selectable values
     * @param optionTexts a list of tests to apply to show the user for each of selectable values
     * @param value the current value of this property
     */
    public CustomPublisherProperty(final String name, final int type, final List<String> options, final List<String> optionTexts, final String value) {
        this.name = name;
        this.type = type;
        this.options = options;
        this.optionTexts = optionTexts;
        this.value = value;
    }
    
    /**
     * Representation of a property where the user can select from a list of choices.
     *  
     * @param name name of the property
     * @param type one of CustomPublisherProperty.UI_* constants (only UI_TEXTINPUT or UI_BOOLEAN makes sense in the current implementation)
     * @param value the current value of this property
     */
    public CustomPublisherProperty(final String name, final int type, final String value) {
        this.name = name;
        this.type = type;
        this.options = null;
        this.optionTexts = null;
        this.value = value;
    }
    
    /** @return the current value of this property (as String) */
    public String getValue() {
        return value;
    }
    
    /** Set the current value of this property (as String) */
    public void setValue(String value) {
        if (value != null) {
            value = value.trim();
        }
        this.value = value;
    }

    /** @return the name of this property */
    public String getName() { return name; }
    /** @return one of the CustomPublisherProperty.UI_* constants */
    public int getType() { return type; }
    /** @return a List of values this property can have or null if this does not apply to the type */
    public List<String> getOptions() { return options; }
    /** @return a List of user-friendly texts corresponding to the values this property can have or null if this does not apply to the type */
    public List<String> getOptionTexts() { return optionTexts; }
    
    /**
     * Return a class string in the format of "enable-ID1 enable-ID1-value1 enable-ID1-value2 ..."
     */
    public static String conditionalEnableToClassNames(final String controllingControlId, final Collection<String> enablingValues) {
        Validate.notNull(enablingValues);
        Validate.notEmpty(enablingValues);
        
        return "enable-" + controllingControlId + " " + 
            enablingValues.stream()
                .map(v -> "enable-" + controllingControlId + "-" + v)
                .collect(Collectors.joining(" " ));
    }

    /**
     * Get a unique CSS class set that can be used in javascript to find this control when enabling/disabling.  
     * The CSS class set will look like:
     * "enable-ID1 enable-ID1-value1 enable-ID1-value2... enable-ID2 enable-ID2-value1 ..."
     * 
     * This allows java script to look for all controls that should be enabled based on the control with id = ID
     * and then enable them if the controlling control has one of the selected values.  value1, value2, ... 
     * should be safe as class id parts (they are base64'ed elsewhere).
     */
    public String getCssClass() {
        org.apache.log4j.Logger.getLogger(CustomPublisherProperty.class).warn("getting cssClass for " + this.name);
        org.apache.log4j.Logger.getLogger(CustomPublisherProperty.class).warn(enabledWhenControlHasValue);
        if (enabledWhenControlHasValue.isEmpty()) {
            return "";
        } else {
            //@formatter:off
            return enabledWhenControlHasValue.keySet().stream()
                    .map(c -> conditionalEnableToClassNames(c, enabledWhenControlHasValue.get(c)))
                    .collect(Collectors.joining());
            //@formatter:on
        }
    }

    public String getEnabledGroupId() {
        org.apache.log4j.Logger.getLogger(CustomPublisherProperty.class).warn("getEnabledGroupId for " + this.name + " = " + enabledGroupId);
        return enabledGroupId;
    }
}
