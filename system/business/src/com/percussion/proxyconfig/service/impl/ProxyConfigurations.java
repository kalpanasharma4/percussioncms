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
// Generated on: 2010.12.21 at 04:40:48 PM EST 
//

package com.percussion.proxyconfig.service.impl;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder =
{"proxyConfigs"
})
@XmlRootElement(name = "ProxyConfigurations")
public class ProxyConfigurations
{

   @XmlElement(name = "ProxyConfig")
   protected List<ProxyConfig> proxyConfigs;

   /**
    * Gets the value of the proxyConfigs property. It holds all configurations
    * from the proxy configuration file.
    * 
    * <p>
    * This accessor method returns a copy of the live list.
    * 
    * <p>
    * Objects of the following type(s) are allowed in the list
    * {@link ProxyConfig }
    * 
    */
   public List<ProxyConfig> getConfigs()
   {
      if (proxyConfigs == null)
      {
         proxyConfigs = new ArrayList<ProxyConfig>();
      }

      List<ProxyConfig> copy = new ArrayList<ProxyConfig>(proxyConfigs.size());

      for (ProxyConfig config : proxyConfigs)
      {
         copy.add(((ProxyConfig) config.clone()));
      }
      return copy;
   }

   public void setConfigs(List<ProxyConfig> configurations)
   {
      proxyConfigs = new ArrayList<ProxyConfig>();

      for (ProxyConfig config : configurations)
      {
         this.proxyConfigs.add((ProxyConfig) config.clone());
      }
   }
   
}
