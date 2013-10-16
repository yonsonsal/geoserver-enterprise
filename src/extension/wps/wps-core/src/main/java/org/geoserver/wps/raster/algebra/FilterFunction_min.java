package org.geoserver.wps.raster.algebra;

/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2005-2008, Open Source Geospatial Foundation (OSGeo)
 *    
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

//this code is autogenerated - you shouldnt be modifying it!
import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;

/**
 * 
 *
 * @source $URL$
 */
public class FilterFunction_min extends FunctionExpressionImpl {

    public static FunctionName NAME = new FunctionNameImpl(
            "min2",
            parameter("minimum", Double.class),
            parameter("double",Double.class,2,Integer.MAX_VALUE));

    public FilterFunction_min() {
        super("min2");
        functionName = NAME;
    }
    
    @Override
    public int getArgCount() {
        return -1;
    }
    
    public Object evaluate(Object feature) {
        throw new UnsupportedOperationException();
    }
}
