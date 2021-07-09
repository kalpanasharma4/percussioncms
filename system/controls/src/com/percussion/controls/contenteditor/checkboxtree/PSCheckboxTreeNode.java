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
package com.percussion.controls.contenteditor.checkboxtree;

import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Component;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

/**
 * The tree node as used with the checkbox tree applet control.
 */
public class PSCheckboxTreeNode extends DefaultMutableTreeNode
{
   /**
    * Constructs a new checkbox tree node for the supplied parameters.
    * 
    * @param id the node identifier, not <code>null</code> or empty. Should
    *    be unique but uniqueness is not enforced.
    * @param label the nodes display label, not <code>null</code> or empty.
    */
   public PSCheckboxTreeNode(String id, String label)
   {
      setNodeId(id);
      setNodeName(label);
   }
   
   /**
    * Get the renderer for the tree component.
    * 
    * @return the component render, never <code>null</code>.
    */
   public Component getRenderedComponent()
   {
      if (m_selectable)
      {
         JCheckBox box = new JCheckBox(m_nodeName, m_selected);
         box.setContentAreaFilled(false);

         return box;
      }
      else
      {
         return new JLabel(m_nodeName);
      }
   }

   /**
    * Determines if this node is selecable or not.
    * 
    * @return <code>true<code> if the node can be selected, <code>false</code>
    *    otherwise.
    */
   public boolean isSelectable()
   {
      return m_selectable;
   }

   /**
    * gets the selection of the current node.
    * 
    * @return <code>true</code> if this node already selected.
    */
   public boolean isSelected()
   {
      return m_selected;
   }

   /**
    * Sets the selection flag on this node.
    * 
    * @param select <code>true</code> to select this node, <code>false</code>
    *    to de-select it.
    */
   public void setSelected(boolean select)
   {
      m_selected = select;
   }

   /**
    * Toggle the current selection.
    */
   public void toggleSelected()
   {
      setSelected(!isSelected());
   }

   /**
    * Get the node id.
    * 
    * @return the node id, never <code>null</code> or empty.
    */
   public String getNodeId()
   {
      return m_nodeId;
   }

   /**
    * Set a new node id.
    * 
    * @param id the new node id, not <code>null</code> or empty.
    */
   public void setNodeId(String id)
   {
      if (id == null || id.trim().length() == 0)
         throw new IllegalArgumentException(
            "id cannot be null or empty");
      
      m_nodeId = id;
   }

   /**
    * Get the node name.
    * 
    * @return the node name, never <code>null</code> or empty.
    */
   public String getNodeName()
   {
      return m_nodeName;
   }

   /**
    * Set a new node name.
    * 
    * @param name the new node name, not <code>null</code> or empty.
    */
   public void setNodeName(String name)
   {
      if (name == null || name.trim().length() == 0)
         throw new IllegalArgumentException(
            "name cannot be null or empty");
      
      m_nodeName = name;
   }

   /**
    * Set if this item is selectable.
    * 
    * @param selectable <code>true</code> if the item is selectable, 
    *    <code>false</code> otherwise.
    */
   public void setSelectable(boolean selectable)
   {
      m_selectable = selectable;
   }

   /**
    * Is this item selected, <code>true</code> if it is, <code>false</code>
    * otherwise.
    */
   private boolean m_selected = false;

   /**
    * Is this node selectable, <code>true</code> if it is, <code>false</code>
    * otherwise. If so, it will have a checkbox.
    */
   private boolean m_selectable;

   /**
    * The internal id value returned when selected, never <code>null</code>
    * or empty. Should be unique, but this class does not enforce uniqueness.
    */
   private String m_nodeId;

   /**
    * The name or label for this node, never <code>null</code> or empty.
    */
   private String m_nodeName;

   /**
    * Generated serial version id.
    */
   private static final long serialVersionUID = -1276983430140508363L;
}
