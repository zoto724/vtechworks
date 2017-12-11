/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

<<<<<<< HEAD
=======
import org.dspace.content.Item;

>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class ImageMagickPdfThumbnailFilter extends ImageMagickThumbnailFilter {
   @Override
   public InputStream getDestinationStream(Item currentItem, InputStream source, boolean verbose)
        throws Exception
    {
		File f = inputStreamToTempFile(source, "impdfthumb", ".pdf");
		File f2 = null;
	    File f3 = null;
	    try
	    {
<<<<<<< HEAD
		    f2 = getImageFile(f, 0);
		    f3 = getThumbnailFile(f2);
=======
		    f2 = getImageFile(f, 0, verbose);
		    f3 = getThumbnailFile(f2, verbose);
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
		    byte[] bytes = Files.readAllBytes(f3.toPath());
		    return new ByteArrayInputStream(bytes);
	    }
		finally
	    {
		    //noinspection ResultOfMethodCallIgnored
		    f.delete();
		    if (f2 != null)
		    {
			    //noinspection ResultOfMethodCallIgnored
			    f2.delete();
		    }
		    if (f3 != null)
		    {
			    //noinspection ResultOfMethodCallIgnored
			    f3.delete();
		    }
	    }
    }

}
