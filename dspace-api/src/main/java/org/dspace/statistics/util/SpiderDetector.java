/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import java.io.File;
import java.io.IOException;
import java.util.Set;
<<<<<<< HEAD
import java.util.Collections;
import java.util.regex.Pattern;
=======
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
import javax.servlet.http.HttpServletRequest;

import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SpiderDetector delegates static methods to SpiderDetectorService, which is used to find IP's that are spiders...
 *
 * @author kevinvandevelde at atmire.com
 * @author ben at atmire.com
 * @author Mark Diggory (mdiggory at atmire.com)
 * @author frederic at atmire.com
 */
public class SpiderDetector {

    private static final Logger log = LoggerFactory.getLogger(SpiderDetector.class);

    //Service where all methods get delegated to, this is instantiated by a spring-bean defined in core-services.xml
    private static SpiderDetectorService spiderDetectorService = StatisticsServiceFactory.getInstance().getSpiderDetectorService();

    /**
     * Get an immutable Set representing all the Spider Addresses here
     *
     * @return a set of IP addresses as strings
     */
<<<<<<< HEAD
    private static IPTable table = null;

    /** Collection of regular expressions to match known spiders' agents. */
    private static List<Pattern> agents = Collections.synchronizedList(new ArrayList<Pattern>());

    /** Collection of regular expressions to match known spiders' domain names. */
    private static List<Pattern> domains = Collections.synchronizedList(new ArrayList<Pattern>());
=======
    public static Set<String> getSpiderIpAddresses() {

        spiderDetectorService.loadSpiderIpAddresses();
        return spiderDetectorService.getTable().toSet();
    }
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689

    /**
     * Utility method which reads lines from a file & returns them in a Set.
     *
     * @param patternFile the location of our spider file
     * @return a vector full of patterns
     * @throws IOException could not happen since we check the file be4 we use it
     */
    public static Set<String> readPatterns(File patternFile)
            throws IOException
    {
        return spiderDetectorService.readPatterns(patternFile);
    }

    /**
     * Static Service Method for testing spiders against existing spider files.
     * <p>
     * In future spiders HashSet may be optimized as byte offset array to
     * improve performance and memory footprint further.
     *
     * @param clientIP address of the client.
     * @param proxyIPs comma-list of X-Forwarded-For addresses, or null.
     * @param hostname domain name of host, or null.
     * @param agent User-Agent header value, or null.
     * @return true if the client matches any spider characteristics list.
     */
    public static boolean isSpider(String clientIP, String proxyIPs,
                                   String hostname, String agent)
    {
<<<<<<< HEAD
        // See if any agent patterns match
        if (null != agent)
        {   
            synchronized(agents)
            {
                if (agents.isEmpty())
                    loadPatterns("agents", agents);
            }
            for (Pattern candidate : agents)
            {
		// prevent matcher() invocation from a null Pattern object
                if (null != candidate && candidate.matcher(agent).find())
                {
                    return true;
                }
            }
        }

        // No.  See if any IP addresses match
        if (isUseProxies() && proxyIPs != null) {
            /* This header is a comma delimited list */
            for (String xfip : proxyIPs.split(",")) {
                if (isSpider(xfip))
                {
                    return true;
                }
            }
        }

        if (isSpider(clientIP))
            return true;

        // No.  See if any DNS names match
        if (null != hostname)
        {
            synchronized(domains) 
            {
                if (domains.isEmpty())
                    loadPatterns("domains", domains);
            }
            for (Pattern candidate : domains)
            {
		// prevent matcher() invocation from a null Pattern object
		if (null != candidate && candidate.matcher(hostname).find())
                {
                    return true;
                }
            }
        }

        // Not a known spider.
        return false;
=======
        return spiderDetectorService.isSpider(clientIP, proxyIPs, hostname, agent);
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
    }

    /**
     * Static Service Method for testing spiders against existing spider files.
     *
     * @param request
     * @return true|false if the request was detected to be from a spider.
     */
    public static boolean isSpider(HttpServletRequest request)
    {
        return spiderDetectorService.isSpider(request);
    }

    /**
     * Check individual IP is a spider.
     *
     * @param ip
     * @return if is spider IP
     */
    public static boolean isSpider(String ip) {
        return spiderDetectorService.isSpider(ip);
    }

}