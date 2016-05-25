package dlg.parser.binder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("rawtypes")
public class BindedClasses {
	
	private static Map<String, BindedClass> binders = new ConcurrentHashMap<String, BindedClass>();
	
	public static boolean containsClassBinder(Class clazz) {
		return binders.containsKey(clazz.getName());
	}
	
	public synchronized static BindedClass getBinder(Class clazz) {
		if (!containsClassBinder(clazz)) {
			BindedClass binder = new BindedClass(clazz);
			binders.put(clazz.getName(), binder);
		}
		return binders.get(clazz.getName());
	}

}
