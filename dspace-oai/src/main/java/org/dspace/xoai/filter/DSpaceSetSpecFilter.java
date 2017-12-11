/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import com.lyncode.xoai.dataprovider.core.ReferenceSet;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.filter.results.SolrFilterResult;
import org.dspace.xoai.services.api.CollectionsService;
import org.dspace.xoai.services.api.HandleResolver;


/**
 *
 * @author Lyncode Development Team (dspace at lyncode dot com)
 */
public class DSpaceSetSpecFilter extends DSpaceFilter
{
    private static final Logger log = LogManager.getLogger(DSpaceSetSpecFilter.class);

    private final String setSpec;
    private final HandleResolver handleResolver;
    private final CollectionsService collectionsService;

    public DSpaceSetSpecFilter(CollectionsService collectionsService, HandleResolver handleResolver, String spec)
    {
        this.collectionsService = collectionsService;
        this.handleResolver = handleResolver;
        this.setSpec = spec;
    }

    @Override
<<<<<<< HEAD
    public DatabaseFilterResult buildDatabaseQuery(Context context)
    {
        if (setSpec.startsWith("col_"))
        {
            try
            {
                DSpaceObject dso = handleResolver.resolve(setSpec.replace("col_", "").replace("_", "/"));
		if(dso != null){
	                return new DatabaseFilterResult(
        	                "EXISTS (SELECT tmp.* FROM collection2item tmp WHERE tmp.resource_id=i.item_id AND collection_id = ?)",
                        dso.getID());
		}
            }
            catch (Exception ex)
            {
                log.error(ex.getMessage(), ex);
            }
        }
        else if (setSpec.startsWith("com_"))
        {
            try
            {
                DSpaceObject dso = handleResolver.resolve(setSpec.replace("com_", "").replace("_", "/"));
		if(dso != null){
                	List<Integer> list = collectionsService.getAllSubCollections(dso.getID());
	                String subCollections = StringUtils.join(list.iterator(), ",");
        	        return new DatabaseFilterResult(
                	        "EXISTS (SELECT tmp.* FROM collection2item tmp WHERE tmp.resource_id=i.item_id AND collection_id IN ("
                                + subCollections + "))");
		}
            }
            catch (Exception e)
            {
                log.error(e.getMessage(), e);
            }
        }
        return new DatabaseFilterResult();
    }

    @Override
=======
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
    public boolean isShown(DSpaceItem item)
    {
        for (ReferenceSet s : item.getSets())
            if (s.getSetSpec().equals(setSpec))
                return true;
        return false;
    }

    @Override
    public SolrFilterResult buildSolrQuery()
    {
        if (setSpec.startsWith("col_"))
        {
            try
            {
                return new SolrFilterResult("item.collections:"
                        + ClientUtils.escapeQueryChars(setSpec));
            }
            catch (Exception ex)
            {
                log.error(ex.getMessage(), ex);
            }
        }
        else if (setSpec.startsWith("com_"))
        {
            try
            {
                return new SolrFilterResult("item.communities:"
                        + ClientUtils.escapeQueryChars(setSpec));
            }
            catch (Exception e)
            {
                log.error(e.getMessage(), e);
            }
        }
        return new SolrFilterResult();
    }

}
