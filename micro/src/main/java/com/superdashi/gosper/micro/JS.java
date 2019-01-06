package com.superdashi.gosper.micro;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.superdashi.gosper.graphdb.Order;
import com.superdashi.gosper.graphdb.PartRef;
import com.superdashi.gosper.graphdb.Selector;
import com.superdashi.gosper.item.Flavor;
import com.superdashi.gosper.item.Image;
import com.superdashi.gosper.item.Info;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.item.Priority;
import com.superdashi.gosper.item.Qualifier;
import com.superdashi.gosper.item.ScreenClass;
import com.superdashi.gosper.item.ScreenColor;
import com.superdashi.gosper.item.Value;
import com.superdashi.gosper.item.ValueOrder;
import com.superdashi.gosper.micro.FormModel.FieldType;
import com.superdashi.gosper.util.Time;

//NOTE: order important
final class JS {

	private static final ScriptEngineManager manager;
	static {
		//TODO hacky
		System.setProperty("nashorn.args", "--language=es6");
		manager = new ScriptEngineManager();
	}

	private static ScriptEngine staticEngine = newEngine();
	private static Method callMethod;
	static {
		Method method;
		try {
			Class<?> clss = Class.forName("jdk.nashorn.api.scripting.JSObject");
			method = clss.getMethod("call", Object.class, Object[].class);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
		callMethod = method;
	}

	static ScriptEngine newEngine() {
		return manager.getEngineByName("nashorn");
	}

	static Object invokeFunction(Object fn, Object... args) {
		try {
			return callMethod.invoke(fn, null, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private static Object jsClass(String className) {
		try {
			return staticEngine.eval("Java.type('" + className + "')");
		} catch (ScriptException e) {
			throw new RuntimeException("failed to wrap java class " + className + " as a JS object");
		}
	}

	private static void recordJsClass(String name, Class<?> clss) {
		String className = clss.getTypeName();
		Bindings bindings = manager.getBindings();
		if (bindings.containsKey(name)) throw new IllegalArgumentException("duplicate name " + name + " from class " + className);
		bindings.put(name, jsClass(className));
	}

	private static void recordJsClass(Class<?> clss) {
		String className = clss.getTypeName();
		int i = className.lastIndexOf('.');
		int j = className.lastIndexOf('$');
		if (i == -1) throw new IllegalArgumentException("unqualified class");
		String name = className.substring(Math.max(i,j) + 1);
		recordJsClass(name, clss);
	}

	// WHAT CLASSES NEED TO BE ADDED?
	// Classes that have constructors or static methods/constants that applications need to access.

	static {
		// item
		recordJsClass(Flavor          .class); // enum
		recordJsClass(Image           .class); // cons - needed?
		recordJsClass(Info            .class); // stat cons
		recordJsClass(Item            .class); // stat cons
		recordJsClass(Priority        .class); // enum
		recordJsClass(Qualifier       .class); // stat cons
		recordJsClass(ScreenClass     .class); // enum
		recordJsClass(ScreenColor     .class); // enum
		recordJsClass(Value           .class); // stat cons
		recordJsClass(ValueOrder      .class); // stat cons

		// micro
		recordJsClass(Action          .class); // stat cons
		recordJsClass(ActionsModel    .class); // enums (to be removed with enums) TODO
		//FACTORY recordJsClass(ActivityContext .class); // statics
		//FACTORY recordJsClass(ActivityData    .class); // streaming
		recordJsClass(ActivityMode    .class); // enum
		recordJsClass(ActivityOutput  .class); // cons
		recordJsClass(Background      .class); // stat cons
		recordJsClass(BlockingActivity.class); //TODO needs work to be usable from JS
		recordJsClass(CardDesign      .class); // cons
		recordJsClass(Content         .class); // stat cons
		//FACTORY recordJsClass(DataInput       .class); // streaming
		recordJsClass(DataOutput      .class); // cons
		//FACTORY recordJsClass(DeferredActivity.class); // streaming
		recordJsClass(DisplayColumns  .class); // cons
		//TODO will JS be able to define Indicators?
		recordJsClass(FieldType       .class); // enum
		recordJsClass(ItemContents    .class); // stat cons
		//FACTORY recordJsClass(Layout          .class); // enums, stat cons
		//FACTORY recordJsClass(Location        .class); // stat cons
		//FACTORY recordJsClass(Regex           .class); // stat cons
		//FACTORY recordJsClass(Rows            .class); // stat cons
		//FACTORY recordJsClass(Table.Row       .class); // cons

		//graph
		recordJsClass(Selector        .class); // stat cons
		recordJsClass(Order           .class); // stat cons
		recordJsClass(PartRef         .class); // stat cons

		// util
		recordJsClass(Time            .class);

		manager.getBindings().put("factory", new JSFactory());
	}

	JS() {}

	public static class JSFactory implements Factory {

		@Override
		//TODO can we make this type safe to a degree
		public <S, T> Function<S, T> asMapping(Object fn) {
			return x -> (T) JS.invokeFunction(fn, x);
		}

		@Override
		public Rows asRows(Object obj) {
			Rows rows = ((Invocable) staticEngine).getInterface(obj, Rows.class);
			if (rows == null) throw new IllegalArgumentException("object could not be converted to Rows");
			return rows;
		}

	}
}
