package org.qubership.integration.platform.engine.jms.weblogic;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jndi.JndiTemplate;

import javax.naming.NamingException;

@Slf4j
public class JndiDestinationResolver implements DestinationResolver {
    public static final String CONTAINER_PREFIX = "java:comp/env/";

    @Getter
    @Setter
    private JndiTemplate jndiTemplate = new JndiTemplate();

    @Override
    public Destination resolveDestinationName(Session session, String destinationName, boolean pubSubDomain)
            throws JMSException {

        /*
         * Destination dest = this.destinationCache.get(destinationName);
         * if (dest != null) {
         * validateDestination(dest, destinationName, pubSubDomain);
         * }
         * else {
         */
        Destination dest;
        try {
            dest = lookup(destinationName, Destination.class);
            // validateDestination(dest, destinationName, pubSubDomain);
        } catch (NamingException ex) {
            /*
             * if (logger.isDebugEnabled()) {
             * logger.debug("Destination [" + destinationName + "] not found in JNDI", ex);
             * }
             */
            /*
             * if (this.fallbackToDynamicDestination) {
             * dest = this.dynamicDestinationResolver.resolveDestinationName(session,
             * destinationName, pubSubDomain);
             * } else {
             */
            throw new RuntimeException(
                    "Destination [" + destinationName + "] not found in JNDI", ex);
            // }
        }
        /*
         * if (this.cache) {
         * this.destinationCache.put(destinationName, dest);
         * }
         */
        // }
        return dest;
    }

    protected <T> T lookup(String jndiName, Class<T> requiredType) throws NamingException {
        String convertedName = convertJndiName(jndiName);
        T jndiObject;
        try {
            jndiObject = getJndiTemplate().lookup(convertedName, requiredType);
        } catch (NamingException ex) {
            if (!convertedName.equals(jndiName)) {
                // Try fallback to originally specified name...
                /*
                 * if (logger.isDebugEnabled()) {
                 * logger.debug("Converted JNDI name [" + convertedName +
                 * "] not found - trying original name [" + jndiName + "]. " + ex);
                 * }
                 */
                jndiObject = getJndiTemplate().lookup(jndiName, requiredType);
            } else {
                throw ex;
            }
        }
        /*
         * if (logger.isDebugEnabled()) {
         * logger.debug("Located object with JNDI name [" + convertedName + "]");
         * }
         */
        return jndiObject;
    }

    protected String convertJndiName(String jndiName) {
        // Prepend container prefix if not already specified and no other scheme given.
        if (isResourceRef() && !jndiName.startsWith(CONTAINER_PREFIX) && jndiName.indexOf(':') == -1) {
            jndiName = CONTAINER_PREFIX + jndiName;
        }
        return jndiName;
    }

    public boolean isResourceRef() {
        return false; // this.resourceRef;
    }

}
