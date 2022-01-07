package com.github.sahlaysta.bleco.ui;

import java.util.List;

import javax.swing.AbstractListModel;

// jlist model for the search result list
final class GUISearcherListModel extends AbstractListModel<Object> {
	private static final long serialVersionUID = 1L;

	private List<?> listContent;

	@Override
	public Object getElementAt(int index) {
		return listContent.get(index);
	}
	@Override
	public int getSize() {
		return listContent == null ? 0 : listContent.size();
	}
	
	
	//Display search operation
	void set(List<?> searchResult) {
		this.listContent = searchResult;
		refresh();
	}
	void refresh() {
		fireContentsChanged(this, 0, 0);
	}
}