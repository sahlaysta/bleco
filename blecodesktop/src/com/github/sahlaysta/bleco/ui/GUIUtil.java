package com.github.sahlaysta.bleco.ui;

import java.awt.Component;
import java.awt.Container;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.JMenu;

//Utility class around java swing
final class GUIUtil {
	
	//Gets all components and sub components of a Container
	static Set<Component> getAllComponents(Container container) {
		return addAllComponents(container, new HashSet<>());
	}
	private static Set<Component>
	addAllComponents(Container container, Set<Component> set) {
		set.add(container);
		for (Component c: container.getComponents()) {
			set.add(c);
			if (c instanceof Container)
				addAllComponents((Container)c, set);
			if (c instanceof JMenu)
				for (Component c2: ((JMenu)c)
						.getMenuComponents())
					set.add(c2);
		}
		return set;
	}
	
	//For each Component in a Container
	static void
	forEachComponent(Container container, Consumer<Component> c) {
		getAllComponents(container).forEach(c);
	}
	
	//Finds a Component from a Container based on predicate
	static <T extends Component> T
	getComponent(Container container, Predicate<T> p) {
		for (Component c: getAllComponents(container)) {
			try {
				@SuppressWarnings("unchecked")
				T cast = (T)c;
				if (p.test(cast))
					return cast;
			} catch (ClassCastException e) {
				continue;
			}
		}
		return null;
	}
}