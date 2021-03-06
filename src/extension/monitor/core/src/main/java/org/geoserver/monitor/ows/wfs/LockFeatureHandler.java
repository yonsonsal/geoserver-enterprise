/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wfs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.xml.EMFUtils;

public class LockFeatureHandler extends WFSRequestObjectHandler {

    public LockFeatureHandler(MonitorConfig config, Catalog catalog) {
        super("net.opengis.wfs.LockFeatureType", config, catalog);
    }

    @Override
    public List<String> getLayers(Object request) {
        @SuppressWarnings("unchecked")
        List<Object> locks = (List<Object>) EMFUtils.get((EObject)request, "lock");
        if (locks == null) {
            return null;
        }
        
        List<String> layers = new ArrayList<String>();
        for (Object lock : locks) {
            layers.add(toString(EMFUtils.get((EObject)lock, "typeName")));
        }
        
        return layers;
    }
    

    @SuppressWarnings("unchecked")
    @Override
    protected List<Object> getElements(Object request) {
        return (List<Object>) OwsUtils.get(request, "lock");
    }

}
