package org.ejbca.core.model.ca.publisher;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CustomPublisherPropertyTest {

    @Test
    public void classNamePartsFormattedCorrectly() {
        assertEquals("enable-12345 enable-12345-value1 enable-12345-value2",
                CustomPublisherProperty.conditionalEnableToClassNames("12345", Arrays.asList("value1", "value2")));
    }

}
