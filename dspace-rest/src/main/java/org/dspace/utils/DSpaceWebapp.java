/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.utils;

import org.dspace.app.util.AbstractDSpaceWebapp;

/**
 * An MBean to identify this web application.
 *
<<<<<<< HEAD
 * @author Bram Luyten <bram@atmire.com>
=======
 * @author Bram Luyten (bram at atmire dot com)
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
 */
public class DSpaceWebapp
        extends AbstractDSpaceWebapp
{
    public DSpaceWebapp()
    {
        super("REST");
    }

    @Override
    public boolean isUI()
    {
        return false;
    }
}
 
