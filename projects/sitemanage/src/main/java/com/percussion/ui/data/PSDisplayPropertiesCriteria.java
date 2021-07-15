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
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */
package com.percussion.ui.data;

import com.percussion.pathmanagement.data.PSPathItem;

import java.util.List;

/**
 * @author miltonpividori
 *
 */
public class PSDisplayPropertiesCriteria
{
    private List<PSPathItem> items;
    
    private PSSimpleDisplayFormat format;
    
    private boolean isDisplayFormatRequired = true;
    
    public PSDisplayPropertiesCriteria(List<PSPathItem> items, PSSimpleDisplayFormat format)
    {
        this.items = items;
        this.format = format;
    }

    public List<PSPathItem> getItems()
    {
        return items;
    }

    public PSSimpleDisplayFormat getFormat()
    {
        return format;
    }

    public boolean isDisplayFormatRequired()
    {
        return isDisplayFormatRequired;
    }

    public void setDisplayFormatRequired(boolean isDisplayFormatRequired)
    {
        this.isDisplayFormatRequired = isDisplayFormatRequired;
    }
}
