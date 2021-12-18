package com.github.sahlaysta.bleco;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;

/** Class representing properties of a window such as size and width.
 * Used for preferences serialization */
public class WindowProperties {
	
	//Constructor
	/** True if the window is maximized */
	public final boolean maximized;
	/** The X position of the window */
	public final int x;
	/** The Y position of the window */
	public final int y;
	/** The width of the window's size */
	public final int width;
	/** The height of the window's size */
	public final int height;
	/** Construct a WindowProperties object manually defining each property */
	public WindowProperties(boolean maximized, int x, int y, int width, int height) {
		this.maximized = maximized;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	//Non-static methods
	/** Apply this WindowProperties object to a Frame */
	public void apply(Frame frame) {
		frame.setLocation(x, y);
		frame.setSize(width, height);
		if (maximized)
			frame.setExtendedState(Frame.MAXIMIZED_BOTH);
	}
	
	//Static methods
	/** Create a WindowProperties object from a Frame, using the previously created WindowProperties object,
	 * <code>previousWindowProperties</code>.<br><br>
	 * 
	 * This overload takes the previous WindowProperties object in case the Frame is maximized, so that the
	 * size and position <i>before the window was maximized</i> may be preserved.<br><br>
	 * 
	 * Passing null <code>previousWindowProperties</code> will return a WindowProperties object created from
	 * the passed Frame <code>frame</code> */
	public static final WindowProperties fromFrame(Frame frame, WindowProperties previousWindowProperties) {
		int state = frame.getExtendedState();
		if (previousWindowProperties != null && state == Frame.MAXIMIZED_BOTH)
			return new WindowProperties(
					true,
					previousWindowProperties.x,
					previousWindowProperties.y,
					previousWindowProperties.width,
					previousWindowProperties.height);
		else {
			Point p = frame.getLocation();
			Dimension d = frame.getSize();
			return new WindowProperties(
					false,
					p.x,
					p.y,
					d.width,
					d.height);
		}
	}
	
	/** Create a WindowProperties object from a Frame */
	public static final WindowProperties fromFrame(Frame frame) {
		Point p = frame.getLocation();
		Dimension d = frame.getSize();
		return new WindowProperties(
				frame.getExtendedState() == Frame.MAXIMIZED_BOTH,
				p.x,
				p.y,
				d.width,
				d.height);
	}
}