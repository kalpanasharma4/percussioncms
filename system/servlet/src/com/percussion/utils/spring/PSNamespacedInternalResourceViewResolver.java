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

package com.percussion.utils.spring;

import java.util.Locale;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * An internal resource view resolver that responds ONLY to views 
 * that begin with the specified namespace. 
 * Unlike the standard internal resource view resolver, this resolver 
 * can be chained, as it will not respond to any view names
 * which do not begin with the namespace
 * 
 * @author jasonchu
 * @author davidbenua
 * 
 */
public class PSNamespacedInternalResourceViewResolver extends InternalResourceViewResolver {

    private String m_namespace;

    /**
     *
     * 
     * @see
     * org.springframework.web.servlet.view.UrlBasedViewResolver#loadView(java
     * .lang.String, java.util.Locale)
     */
    @Override
    protected View loadView(String viewName, Locale locale) throws Exception {
        if (m_namespace == null)
            throw new IllegalStateException("namespace must be assigned");

        // only handle requests whose view name is prefixed with a specific
        // namespace
        if (viewName.startsWith(m_namespace)) {
            return super.loadView(viewName.substring(m_namespace.length()), locale);
        }
        return null;
    }

    /**
     * Gets the namespace. 
     * @return the namespace
     */
    public String getNamespace() {
        return m_namespace;
    }

    /**
     * Sets the namespace.
     * @param namespace
     *            the namespace to set
     */
    public void setNamespace(String namespace) {
        m_namespace = namespace;
    }
}
