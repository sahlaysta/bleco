package com.github.sahlaysta.bleco.ui;

import java.awt.Component;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JOptionPane;

//lazy utility class for java swing message boxes
final class GUIMessageBox {
	
	static void showMsg(
			Component comp,
			String title,
			String message) {
		JOptionPane.showMessageDialog(
			comp,
			message,
			title,
			JOptionPane.INFORMATION_MESSAGE);
	}
	
	static void showErrorMsg(
			Component comp,
			String title,
			String message) {
		JOptionPane.showMessageDialog(
			comp,
			message,
			title,
			JOptionPane.ERROR_MESSAGE);
	}
	
	static void showErrorMsg(
			Component comp,
			String title,
			Throwable e) {
		JOptionPane.showMessageDialog(
			comp,
			stackTraceToString(e),
			title,
			JOptionPane.ERROR_MESSAGE);
	}
	
	private static String
	stackTraceToString(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
}