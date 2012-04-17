/*
 * @(#)ImageCanvas.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.awt;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.URL;

/**
 * ImageCanvas class is a canvas which displays Image without
 * specifying width and height.
 * 
 * @author	Toyokazu Tomatsu
 * @version	1.1
 */
public class ImageCanvas extends Canvas {
	/**
	 * @serial
	 */
	Image image;

	/**
	 * @serial
	 */
	int width = -1;

	/**
	 * @serial
	 */
	int height = -1;

	/**
	 * @serial
	 */
	double rate_x = 1f;

	/**
	 * @serial
	 */
	double rate_y = 1f;

	/**
	 * @serial
	 */
	private boolean completed = false;

	private boolean error = false;

	/**
	 * @serial
	 */
	private int top;

	/**
	 * @serial
	 */
	private int bottom;

	public ImageCanvas(Image image){
		this(image, -1, -1);
	}

	public ImageCanvas(URL url) {
		this(url, -1, -1);
	}

	public ImageCanvas(String file) throws FileNotFoundException {
		this(file, -1, -1);
	}

	public ImageCanvas(Image image, double rate_x, double rate_y){
		this(image);
		this.rate_x = rate_x;
		this.rate_y = rate_y;
	}

	public ImageCanvas(URL url, double rate_x, double rate_y){
		this(url);
		this.rate_x = rate_x;
		this.rate_y = rate_y;
	}

	public ImageCanvas(String file, double rate_x, double rate_y)
		throws FileNotFoundException {
		this(file);
		this.rate_x = rate_x;
		this.rate_y = rate_y;
	}

	public ImageCanvas(Image image, int width, int height){
		this.image = image;
		this.width = width;
		this.height = height;
		completed = (image.getWidth(this) >= 0 && image.getHeight(this) >= 0);
	}

	public ImageCanvas(URL url, int width, int height) {
		this.image = Toolkit.getDefaultToolkit().getImage(url);
		this.width = width;
		this.height = height;
		completed = (image.getWidth(this) >= 0 && image.getHeight(this) >= 0);
	}

	public ImageCanvas(String file, int width, int height) throws FileNotFoundException {
		if (new File(file).exists()){
			this.image = Toolkit.getDefaultToolkit().getImage(file);
			this.width = width;
			this.height = height;
		} else {
			throw new FileNotFoundException(file);
		}
		completed = (image.getWidth(this) >= 0 && image.getHeight(this) >= 0);
	}

	public synchronized Dimension getPreferredSize(){
		if (width >= 0 && height >= 0){
			return new Dimension(width, height);
		}
		int w = image.getWidth(this);
		int h = image.getHeight(this);
		while (!error && (w < 0 || h < 0)){
			try {
				wait(1000);
			} catch (InterruptedException e){}
			w = image.getWidth(this);
			h = image.getHeight(this);
		}
		if (rate_x == 1f && rate_y == 1f){
			return new Dimension(w, h);
		} else {
			w = (int)Math.round(w * rate_x);
			h = (int)Math.round(h * rate_y);
			return new Dimension(w, h);
		}
	}

	public synchronized void sync(){
		while (!completed && !error){
			try {
				wait(1000);
			} catch (InterruptedException e){}
		}
	}

	public synchronized boolean imageUpdate(Image img, int flags, int x, int y, int w, int h){
		if ((flags & ImageObserver.ERROR) != 0){
			error = true;
			return true;
		}
		boolean b = super.imageUpdate(img, flags, x, y, w, h);
		bottom = y;
		if (!b) completed = true;
		notifyAll();
		return b;
	}

	public void paint(Graphics g){
		int w = getSize().width;
		int h = getSize().height;
		if (!completed){
			g.setClip(0, top, w, bottom - top);
		} else {
			g.setClip(0, 0, w, h);
		}
		if (width >= 0 && height >= 0){
			g.drawImage(image, 0, 0, width, height, this);
		} else if (rate_x == 1f && rate_y == 1f){
			g.drawImage(image, 0, 0, this);
		} else {
			w = getPreferredSize().width;
			h = getPreferredSize().height;
			g.drawImage(image, 0, 0, w, h, this);
		}
		top = bottom;
	}

	public void update(Graphics g){
		paint(g);
	}

	public boolean isError(){
		return error;
	}
}
