package com.opencms.workplace;

import org.w3c.dom.*;
import org.xml.sax.*;

import com.opencms.core.*;
import com.opencms.template.*;
import com.opencms.file.*;

import java.util.*;


/**
 * Class for building workplace labels. <BR>
 * Called by CmsXmlTemplateFile for handling the special XML tag <code>&lt;LABEL&gt;</code>.
 * 
 * @author Michael Emmerich
 * @version $Revision: 1.4 $ $Date: 2000/01/27 15:01:43 $
 */
public class CmsLabel extends A_CmsWpElement implements I_CmsWpElement, I_CmsWpConstants  {    
    
    /**
     * Handling of the <CODE>&lt;LABEL&gt;</CODE> tags.
     * Calls the user method <code>elementTag</code> that has to be
     * defined in the XML template class. 
     * 
     * @param XML element containing the <code>&lt;LABEL&gt;</code> tag.
     * @param callingObject Reference to the object requesting the node processing.
     * @param userObj Customizable user object that will be passed through to handling and user methods.
     * @return Result of user method <code>templateElement()</code>.
     * @exception CmsException
     */
    public Object handleSpecialWorkplaceTag(A_CmsObject cms, Element n, Object callingObject, Hashtable parameters, CmsXmlLanguageFile lang) throws CmsException {
        String labelValue = n.getAttribute(C_LABEL_VALUE);
        String outputValue=null;
        
        CmsXmlWpLabelDefFile labeldef = getLabelDefinitions(cms);
        outputValue = lang.getLanguageValue(labelValue);
        
        String result = labeldef.getLabel(outputValue);
        return result; 
    }                    
}
