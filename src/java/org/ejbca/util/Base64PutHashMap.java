package org.ejbca.util;

import java.util.HashMap;
import java.util.Map;


/** An implementation of HashMap that base64 encodes all String's that you 'put', 
 * it encodes them to form "B64:<base64 encoded string>". It only encodes objects of type String.
 * 
 * @author tomasg
 * @version $Id: Base64PutHashMap.java,v 1.3 2006-06-21 14:54:57 anatom Exp $
 */
public class Base64PutHashMap extends HashMap {
    public Base64PutHashMap() {
        super();
    }
    public Base64PutHashMap(Map m) {
        super(m);
    }
    public Object put(Object key, Object value) {
        if (value == null) {
            return super.put(key, value);
        }
        if (value instanceof String) {
            String s = StringTools.putBase64String((String)value);
            return super.put(key,s);
        }
        return super.put(key, value);
    }
    
}
