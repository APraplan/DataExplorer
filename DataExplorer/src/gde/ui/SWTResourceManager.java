/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024 Winfried Bruegmann
****************************************************************************************/
package gde.ui;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

import gde.GDE;
import gde.config.Settings;
import gde.device.IDevice;
import gde.log.Level;

/**
 * Class to manage SWT resources (Font, Color, Image and Cursor)
 * There are no restrictions on the use of this code.
 *
 * You may change this code and your changes will not be overwritten,
 * but if you change the version number below then this class will be
 * completely overwritten by Jigloo.
 * #SWTResourceManager:version1.70#
 */

/**
 * Initial created by Jigloo plug-in, add some methods to cache additional image data created
 * @author Winfried Brügmann
 */
public class SWTResourceManager {
	private static Logger log = Logger.getLogger(SWTResourceManager.class.getName());

	static int accessCounter = 0;

	static HashMap<String, Object> resources = new HashMap<String, Object>();
	static HashMap<String, Integer> widgets = new HashMap<String, Integer>();
	static Vector<Widget> users = new Vector<Widget>();
	static SWTResourceManager instance = new SWTResourceManager();

	private static DisposeListener disposeListener = new DisposeListener() {
		@Override
		public void widgetDisposed(DisposeEvent e) {
			users.remove(e.getSource());
			if (widgets.get(e.getSource().getClass().getSimpleName()) != null) {
				int newCount = widgets.get(e.getSource().getClass().getSimpleName())-1;
				if (newCount > 0) {
					widgets.put(e.getSource().getClass().getSimpleName(), (widgets.get(e.getSource().getClass().getSimpleName()) - 1));
				}
				else {
					widgets.remove(e.getSource().getClass().getSimpleName());
				}
			}
			if (users.size() == 0)
				dispose();

			//listResourceStatus(); // debug resource housekeeping
		}
	};

	/**
	 * This method should be called by *all* Widgets which use resources
	 * provided by this SWTResourceManager. When widgets are disposed,
	 * they are removed from the "users" Vector, and when no more
	 * registered Widgets are left, all resources are disposed.
	 * <P>
	 * If this method is not called for all Widgets then it should not be called
	 * at all, and the "dispose" method should be explicitly called after all
	 * resources are no longer being used.
	 */
	public static void registerResourceUser(Widget widget) {
		if (users.contains(widget))
			return;
		users.add(widget);
		if (widgets.get(widget.getClass().getSimpleName()) == null) {
			widgets.put(widget.getClass().getSimpleName(), 1);
		}
		else {
			widgets.put(widget.getClass().getSimpleName(), (widgets.get(widget.getClass().getSimpleName())+1));
		}
		widget.addDisposeListener(disposeListener);
	}

//	private static void checkAccess() {
//		if (accessCounter++ % 10 == 0) {
//			listStatus();
//		}
//	}

	public static void listResourceStatus(String callIdentifier) {
		Iterator<String> it = resources.keySet().iterator();
		log.log(Level.WARNING, callIdentifier + ": number collected resources = " + resources.size());
		int numFonts = 0, numColors = 0, numImage = 0, numCursor = 0, numMenu = 0;
		while (it.hasNext()) {
			Object resource = resources.get(it.next());
			if (resource instanceof Font) {
				 ++numFonts;
			}
			else if (resource instanceof Color) {
				 ++numColors;
			}
			else if (resource instanceof Image) {
				 ++numImage;
			}
			else if (resource instanceof Cursor) {
				 ++numCursor;
			}
			else if (resource instanceof Menu) {
				 ++numMenu;
			}
		}
		log.log(Level.WARNING, users.size() + " widgets, " + numFonts + " font, " + numColors + " colors, " + numImage + " images, " + numCursor +  " cursors " + numMenu +  " menus ");
		StringBuffer sb = new StringBuffer();
		for (String key : widgets.keySet()) {
			sb.append(key).append(GDE.STRING_BLANK).append(widgets.get(key)).append(GDE.STRING_COMMA);
		}
		log.log(Level.WARNING, sb.toString());

	}

	public static void dispose() {
		Iterator<String> it = resources.keySet().iterator();
		while (it.hasNext()) {
			Object resource = resources.get(it.next());
			if (resource instanceof Font)
				 ((Font) resource).dispose();
			else if (resource instanceof Color)
				 ((Color) resource).dispose();
			else if (resource instanceof Image)
				 ((Image) resource).dispose();
			else if (resource instanceof Cursor)
				 ((Cursor) resource).dispose();
		}
		resources.clear();
	}


	/**
	 * use this method to debug context menu resource housekeeping, place a breakpoint at dispose implementation line
	 * p.e. this.popupMenu = SWTResourceManager.getMenu("MeasurementContextMenu", this.channelConfigMeasurementPropertiesTabFolder.getShell(), SWT.POP_UP);
	 * @param implClassName
	 * @param shell
	 * @param style
	 * @return menu
	 */
	public static Menu getMenu(final String implClassName, Shell shell, int style) {
		String name = "MENU:" + implClassName; //$NON-NLS-1$
		if (resources.containsKey(name) && !((Menu)resources.get(name)).isDisposed())
			return (Menu) resources.get(name);
		if(resources.containsKey(name) && ((Menu)resources.get(name)).isDisposed())
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "menu isDisposed = " + implClassName); //$NON-NLS-1$

		Menu menu = new Menu(shell, style);
		menu.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent disposeevent) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "menu.widgetDisposed " + implClassName); //$NON-NLS-1$
				resources.remove(implClassName);
			}
		});
		resources.put(implClassName, menu);

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new menu created for " + implClassName); //$NON-NLS-1$
		return menu;
	}

	public static Font getFont(FontData fd) {
		return getFont(fd.getName(), fd.getHeight(), fd.getStyle(), false, false);
	}

	/**
	 * get the given font in style
	 * @param control
	 * @param style SWT.BOLD, SWT.NORMAL
	 * @return the controls font in given style
	 */
	public static Font getFont(Control control, int style) {
		FontData[] fd = control.getFont().getFontData();
		fd[0].setStyle(style);
		if (fd[0].getHeight() > 10) fd[0].setHeight(10); // limit default font size to 10
		return getFont(fd[0]);
	}

	/**
	 * get the given font in style
	 * @param control
	 * @param style SWT.BOLD, SWT.NORMAL
	 * @return the controls font in given style
	 */
	public static Font getFont(Control control, int size, int style) {
		FontData[] fd = control.getFont().getFontData();
		fd[0].setHeight(size);
		fd[0].setStyle(style);
		return getFont(fd[0]);
	}

	/**
	 * get the given font in style
	 * @param gc
	 * @param style SWT.BOLD, SWT.NORMAL
	 * @return the controls font in given style
	 */
	public static Font getFont(GC gc, int style) {
		FontData[] fd = gc.getFont().getFontData();
		fd[0].setStyle(style);
		if (fd[0].getHeight() > 10) fd[0].setHeight(10); // limit default font size to 10
		return getFont(fd[0]);
	}

	public static Font getFont(String name, int size, int style) {
		return getFont(name, size, style, false, false);
	}

	public static Font getFont(String name, int size, int style, boolean strikeout, boolean underline) {
		String fontName = name + GDE.STRING_DASH + size + GDE.STRING_DASH + style + GDE.STRING_DASH + strikeout + GDE.STRING_DASH + underline;
		if (resources.containsKey(fontName)) {
			return (Font) resources.get(fontName);
		}
		FontData fd = new FontData(name, size, style);
		if (strikeout || underline) {
			try {
				Class<?> lfCls = Class.forName("org.eclipse.swt.internal.win32.LOGFONT"); //$NON-NLS-1$
				Object lf = FontData.class.getField("data").get(fd); //$NON-NLS-1$
				if (lf != null && lfCls != null) {
					if (strikeout)
						lfCls.getField("lfStrikeOut").set(lf, Byte.valueOf((byte) 1)); //$NON-NLS-1$
					if (underline)
						lfCls.getField("lfUnderline").set(lf, Byte.valueOf((byte) 1)); //$NON-NLS-1$
				}
			} catch (Throwable e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		Font font = new Font(Display.getDefault(), fd);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new font created = " + fontName); //$NON-NLS-1$
		resources.put(fontName, font);
		return font;
	}

	public static Image getImage(String url, Control widget) {
		Image img = getImage(url);
		if(img != null && widget != null)
			img.setBackground(widget.getBackground());
		return img;
	}

	public static Image getImage(int x, int y) {
		String key = "IMAGE:" + x + GDE.STRING_UNDER_BAR + y; //$NON-NLS-1$
		try {
			if (resources.containsKey(key))
				return (Image) resources.get(key);
			Image img = new Image(Display.getDefault(), x, y);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new image created = " + key); //$NON-NLS-1$
			resources.put(key, img);
			return img;
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
	}

	public static Image getImage(int x, int y, String imgKey) {
		String key = "IMAGE:" + x + GDE.STRING_UNDER_BAR + y + GDE.STRING_UNDER_BAR + imgKey; //$NON-NLS-1$
		try {
			if (resources.containsKey(key))
				return (Image) resources.get(key);
			Image img = new Image(Display.getDefault(), x, y);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new image created = " + key); //$NON-NLS-1$
			resources.put(key, img);
			return img;
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * create an image containing a icon at left hand side and a text
	 * @param pt size of the image
	 * @param imageURL image to be placed left hand side, image must fit the given size
	 * @param text to be placed centered in the remaining space
	 * @return specified image
	 */
	public static Image getImage(Point pt, String imageURL, String text) {
		String key = "IMAGE:" + pt.x + GDE.STRING_UNDER_BAR + pt.y + GDE.STRING_UNDER_BAR + imageURL + GDE.STRING_UNDER_BAR + text; //$NON-NLS-1$
		try {
			if (resources.containsKey(key))
				return (Image) resources.get(key);

			Image buttonImage = SWTResourceManager.getImage(imageURL);
			Image img = SWTResourceManager.getImage(pt.x, pt.y);
			GC gc = new GC(img);
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			gc.fillRoundRectangle(0, 0, pt.x, pt.y, 3, 3);
			gc.drawImage(buttonImage, 0,0);
			Point textExtend = gc.textExtent(text);
			gc.drawText(text, buttonImage.getBounds().width + (pt.x - buttonImage.getBounds().width - textExtend.x) / 2, (pt.y - textExtend.y) / 2);
			gc.dispose();
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new image created = " + key); //$NON-NLS-1$
			resources.put(key, img);
			return img;

		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
	}

	public static Image getRotatedImage(Image image, int style, String imgKey) {
		Image resultImg = null;

		// Use the image's data to create a rotated image's data
		ImageData sd = image.getImageData();
		boolean up = (style & SWT.UP) == SWT.UP;
		String key = "IMAGE:" + sd.width + GDE.STRING_UNDER_BAR + sd.height + GDE.STRING_UNDER_BAR + style + GDE.STRING_UNDER_BAR + imgKey; //$NON-NLS-1$

		try {
			if (resources.containsKey(key)) {
				resultImg = (Image) resources.get(key);
			}
			else {
				ImageData dd = new ImageData(sd.height, sd.width, sd.depth, sd.palette);
				// Determine which way to rotate, depending on up or down
				int dx = 0, dy = 0;
				// Run through the horizontal pixels
				for (int sx = 0; sx < sd.width; sx++) {
					// Run through the vertical pixels
					for (int sy = 0; sy < sd.height; sy++) {
						// Determine where to move pixel to in destination image data
						dx = up ? sy : sd.height - sy - 1;
						dy = up ? sd.width - sx - 1 : sx;

						// Swap the x, y source data to y, x in the destination
						dd.setPixel(dx, dy, sd.getPixel(sx, sy));
					}
				}
				// Create the vertical image
				Image vertical = new Image(Display.getDefault(), dd);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new image created = " + key); //$NON-NLS-1$
				resources.put(key, vertical);
				resultImg = vertical;
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return resultImg;
	}

	public static Image getImage(ImageData imageData) {
		String key = "IMAGE_DATA:" + imageData.height + GDE.STRING_UNDER_BAR + imageData.width + GDE.STRING_UNDER_BAR + imageData.depth; //$NON-NLS-1$
		try {
			if (resources.containsKey(key))
				return (Image) resources.get(key);
			Image img = new Image(Display.getDefault(), imageData);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new image created = " + key); //$NON-NLS-1$
			resources.put(key, img);
			return img;
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
	}

	public static Image getImage(ImageData imageData, String imgKey) {
		String key = "IMAGE_DATA:" + imageData.height + GDE.STRING_UNDER_BAR + imageData.width + GDE.STRING_UNDER_BAR + imgKey ; //$NON-NLS-1$
		try {
			if (resources.containsKey(key))
				return (Image) resources.get(key);
			Image img = new Image(Display.getDefault(), imageData);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new image created = " + key); //$NON-NLS-1$
			resources.put(key, img);
			return img;
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
	}

	public static Image getImage(ImageData imageData, String imgKey, int newWidth, int newHeight, boolean forceRefresh) {
		String key = "IMAGE_DATA:" + newWidth + GDE.STRING_UNDER_BAR + newHeight + GDE.STRING_UNDER_BAR + imgKey;
		try {
			if (resources.containsKey(key) && !forceRefresh)
				return (Image) resources.get(key);
			if (forceRefresh)
				resources.remove(key);
			Image img = new Image(Display.getDefault(), imageData.scaledTo(newWidth, newHeight));
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new image created = " + key); //$NON-NLS-1$
			resources.put(key, img);
			return img;
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
	}

	public static Image getImage(String url) {
		String tmpUrl = null;
		try {
			tmpUrl = url.replace('\\', '/');
			if (tmpUrl.startsWith(GDE.STRING_FILE_SEPARATOR_UNIX))
				tmpUrl = tmpUrl.substring(1);
			if (resources.containsKey(tmpUrl))
				return (Image) resources.get(tmpUrl);
			Image img = new Image(Display.getDefault(), instance.getClass().getClassLoader().getResourceAsStream(tmpUrl));
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new image created = " + tmpUrl); //$NON-NLS-1$
			resources.put(tmpUrl, img);
			return img;
		} catch (Exception e) {
			log.log(Level.SEVERE, instance.getClass().getName() + " - " + tmpUrl);
			log.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
	}

	public static Image getImage(IDevice activeDeviceInstance, String url) {
		String tmpUrl = null;
		try {
			tmpUrl = url.replace('\\', '/');
			if (tmpUrl.startsWith(GDE.STRING_FILE_SEPARATOR_UNIX))
				tmpUrl = tmpUrl.substring(1);
			if (resources.containsKey(tmpUrl))
				return (Image) resources.get(tmpUrl);
			Image img = new Image(Display.getDefault(), activeDeviceInstance.getClass().getClassLoader().getResourceAsStream(tmpUrl));
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new image created = " + tmpUrl); //$NON-NLS-1$
			resources.put(tmpUrl, img);
			return img;
		} catch (Exception e) {
			try {
				log.log(Level.SEVERE, activeDeviceInstance.getName() + " - " + tmpUrl + " not found");
				Image img = new Image(Display.getDefault(), new FileInputStream(Settings.getDevicesPath() + url.substring(url.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX))));
				if (log.isLoggable(Level.FINE))
				 {
					Settings.getInstance();
					log.log(Level.FINE, "new image created = " + Settings.getDevicesPath() + url.substring(url.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX))); //$NON-NLS-1$
				}
				resources.put(tmpUrl, img);
				return img;
			}
			catch (Throwable t) {
				Settings.getInstance();
				log.log(Level.SEVERE, activeDeviceInstance.getName() + " - " + Settings.getDevicesPath() + url.substring(url.lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX)));
				log.log(Level.SEVERE, e.getMessage(), e);
				return getImage(activeDeviceInstance, "resource/NoDevicePicture.jpg");
			}
		}
	}

	public static Color getColor(String rgb) {
		String name = "COLOR:" + rgb; //$NON-NLS-1$
		if (resources.containsKey(name)) return (Color) resources.get(name);
		Color color = new Color(Display.getDefault(), Integer.parseInt(rgb.split(GDE.STRING_COMMA)[0]), Integer.parseInt(rgb.split(GDE.STRING_COMMA)[1]),
				Integer.parseInt(rgb.split(GDE.STRING_COMMA)[2]));
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new color created = " + name); //$NON-NLS-1$
		resources.put(name, color);
		return color;
	}

	public static Color getColor(int swtColor) {
		String name = "COLOR:" + swtColor; //$NON-NLS-1$
		if (resources.containsKey(name))
			return (Color) resources.get(name);
		Color color = Display.getDefault().getSystemColor(swtColor);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new color created = " + name); //$NON-NLS-1$
		resources.put(name, color);
		return color;
	}

	public static Color getColor(int red, int green, int blue) {
		String name = "COLOR:" + red + GDE.STRING_COMMA + green + GDE.STRING_COMMA + blue; //$NON-NLS-1$
		if (resources.containsKey(name))
			return (Color) resources.get(name);
		Color color = new Color(Display.getDefault(), red, green, blue);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new color created = " + name); //$NON-NLS-1$
		resources.put(name, color);
		return color;
	}

	public static Color getColor(Color inColor) {
		String name = "COLOR:" + inColor.getRed() + GDE.STRING_COMMA + inColor.getGreen() + GDE.STRING_COMMA + inColor.getBlue();
		if (resources.containsKey(name))
			return (Color) resources.get(name);
		Color color = new Color(Display.getDefault(), inColor.getRed(), inColor.getGreen(), inColor.getBlue());
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new color created = " + name); //$NON-NLS-1$
		resources.put(name, color);
		return color;
	}

	public static Pattern getPattern(float x1, float y1, float x2, float y2, int swtColor1, int alpha1, int swtColor2, int alpha2) {
		String name = "PATTERN:" + x1 + GDE.STRING_COMMA + y1 + GDE.STRING_COMMA + x2 + GDE.STRING_COMMA + y2 + swtColor1 + GDE.STRING_COMMA + alpha1 + GDE.STRING_COMMA + swtColor2 + GDE.STRING_COMMA + alpha2; //$NON-NLS-1$
		if (resources.containsKey(name))
			return (Pattern) resources.get(name);
		Pattern pattern = new Pattern(Display.getDefault(), x1, y1, x2, y2, SWTResourceManager.getColor(swtColor1), alpha1, SWTResourceManager.getColor(swtColor2), alpha2);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new pattern created = " + name); //$NON-NLS-1$
		resources.put(name, pattern);
		return pattern;
	}

	public static Pattern getPattern(float x1, float y1, float x2, float y2, int swtColor1, int swtColor2) {
		String name = "PATTERN:" + x1 + GDE.STRING_COMMA + y1 + GDE.STRING_COMMA + x2 + GDE.STRING_COMMA + y2 + swtColor1 + GDE.STRING_COMMA + swtColor2; //$NON-NLS-1$
		if (resources.containsKey(name))
			return (Pattern) resources.get(name);
		Pattern pattern = new Pattern(Display.getDefault(), x1, y1, x2, y2, SWTResourceManager.getColor(swtColor1), SWTResourceManager.getColor(swtColor2));
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new pattern created = " + name); //$NON-NLS-1$
		resources.put(name, pattern);
		return pattern;
	}

	public static Cursor getCursor(int type) {
		String name = "CURSOR:" + type; //$NON-NLS-1$
		if (resources.containsKey(name))
			return (Cursor) resources.get(name);
		Cursor cursor = new Cursor(Display.getDefault(), type);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new cursor created = " + name); //$NON-NLS-1$
		resources.put(name, cursor);
		return cursor;
	}

	public static Cursor getCursor(String url) {
		try {
			String tmpUrl = url.replace('\\', '/');
			if (tmpUrl.startsWith(GDE.STRING_FILE_SEPARATOR_UNIX)) tmpUrl = tmpUrl.substring(1);
			if (resources.containsKey(tmpUrl)) return (Cursor) resources.get(tmpUrl);
			ImageData imgCur = new ImageData(instance.getClass().getClassLoader().getResourceAsStream(tmpUrl));
			Cursor cursor = new Cursor(Display.getDefault(), imgCur, imgCur.width/2, imgCur.height/2);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new cursor created = " + tmpUrl); //$NON-NLS-1$
			resources.put(url, cursor);
			return cursor;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
	}

//	public static GC getGC(Image img) {
//		String name = "GC_IMAGE:" + img.hashCode(); //$NON-NLS-1$
//		if (resources.containsKey(name))
//			return (GC) resources.get(name);
//		GC gc = new GC(img);
//		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new GC created = " + name); //$NON-NLS-1$
//		resources.put(name, gc);
//		return gc;
//	}
//
//	public static GC getGC(Display display) {
//		String name = "GC_IMAGE:" + display.hashCode(); //$NON-NLS-1$
//		if (resources.containsKey(name))
//			return (GC) resources.get(name);
//		GC gc = new GC(display);
//		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new GC created = " + name); //$NON-NLS-1$
//		resources.put(name, gc);
//		return gc;
//	}
//
//	public static GC getGC(Canvas canvas, String descriptorKey) {
//		String name = "GC_CANVAS:" + descriptorKey; //$NON-NLS-1$
//		if (resources.containsKey(name))
//			return (GC) resources.get(name);
//		GC gc = new GC(canvas);
//		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "new GC created = " + name); //$NON-NLS-1$
//		resources.put(name, gc);
//		return gc;
//	}
}
