/*
 * @(#)ImageUtil.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.awt;

import java.awt.*;
import java.awt.image.*;

public class ImageUtil {
	static Component component = new Component(){};
	static MediaTracker tracker = new MediaTracker(component);
	static int sid = 0;

	public static boolean waitForImage(Image image){
		int id;
		synchronized (component){
			id = sid++;
		}
		tracker.addImage(image, id);
		try {
			tracker.waitForID(id);
		} catch (InterruptedException e){
			return false;
		}
		tracker.removeImage(image, id);
		return !tracker.isErrorID(id);
	}

	public static BufferedImage makeBufferedImage(Image image){
		return makeBufferedImage(image, BufferedImage.TYPE_INT_RGB);
	}

	public static BufferedImage makeBufferedImage(Image image, int type){
		if (!waitForImage(image)){
			return null;
		}
		BufferedImage bim = new BufferedImage(image.getWidth(null),
											  image.getHeight(null),
											  type);
		Graphics g = bim.createGraphics();
		g.drawImage(image, 0, 0, null);
		return bim;
	}
}
