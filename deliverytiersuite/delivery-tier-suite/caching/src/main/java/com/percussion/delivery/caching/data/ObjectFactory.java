/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percusssion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-661 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.06.14 at 09:48:41 AM ART 
//


package com.percussion.delivery.caching.data;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.percussion.delivery.caching.data package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _CacheConfig_QNAME = new QName("", "CacheConfig");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.percussion.delivery.caching.data
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link PSCacheRegion }
     * 
     */
    public PSCacheRegion createPSCacheRegion() {
        return new PSCacheRegion();
    }

    /**
     * Create an instance of {@link PSCacheConfig }
     * 
     */
    public PSCacheConfig createPSCacheConfig() {
        return new PSCacheConfig();
    }

    /**
     * Create an instance of {@link PSCacheProviderProperty }
     * 
     */
    public PSCacheProviderProperty createPSCacheProviderProperty() {
        return new PSCacheProviderProperty();
    }

    /**
     * Create an instance of {@link PSCacheProvider }
     * 
     */
    public PSCacheProvider createPSCacheProvider() {
        return new PSCacheProvider();
    }

    /**
     * Create an instance of {@link PSCacheProviderProperties }
     * 
     */
    public PSCacheProviderProperties createPSCacheProviderProperties() {
        return new PSCacheProviderProperties();
    }

    /**
     * Create an instance of {@link PSCacheWebProperties }
     * 
     */
    public PSCacheWebProperties createPSCacheWebProperties() {
        return new PSCacheWebProperties();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PSCacheConfig }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "CacheConfig")
    public JAXBElement<PSCacheConfig> createCacheConfig(PSCacheConfig value) {
        return new JAXBElement<PSCacheConfig>(_CacheConfig_QNAME, PSCacheConfig.class, null, value);
    }

}
