package com.superdashi.gosper.logging;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;

public interface LogAppender<T> {

	static LogAppender<StringBuilder> over(StringBuilder sb) {
		if (sb == null) throw new IllegalArgumentException("null sb");
		return new LogAppender<StringBuilder>() {
			@Override public LogAppender<StringBuilder> append(byte    v) { sb.append(v); return this; }
			@Override public LogAppender<StringBuilder> append(short   v) { sb.append(v); return this; }
			@Override public LogAppender<StringBuilder> append(int     v) { sb.append(v); return this; }
			@Override public LogAppender<StringBuilder> append(long    v) { sb.append(v); return this; }
			@Override public LogAppender<StringBuilder> append(boolean v) { sb.append(v); return this; }
			@Override public LogAppender<StringBuilder> append(char    v) { sb.append(v); return this; }
			@Override public LogAppender<StringBuilder> append(float   v) { sb.append(v); return this; }
			@Override public LogAppender<StringBuilder> append(double  v) { sb.append(v); return this; }
			@Override public LogAppender<StringBuilder> append(Object  v) { sb.append(v); return this; }
			@Override public LogAppender<StringBuilder> append(String  v) { sb.append(v); return this; }
			@Override public LogAppender<StringBuilder> append(char[]  v) { sb.append(v); return this; }
			@Override public LogAppender<StringBuilder> append(char[] v, int offset, int len) { sb.append(v, offset, len); return this; }
			@Override public LogAppender<StringBuilder> append(CharSequence v, int offset, int len) { sb.append(v, offset, offset + len); return this; }
			@Override public LogAppender<StringBuilder> append(CharSequence v) { sb.append(v); return this; }
			@Override public StringBuilder underlying() { return sb; }
		};
	}

	static LogAppender<Writer> over(Writer w) {
		if (w == null) throw new IllegalArgumentException("null w");
		return new LogAppender<Writer>() {
			@Override public LogAppender<Writer> append(char v) throws IOException { w.append(v); return this; }
			@Override public LogAppender<Writer> append(String v) throws IOException { w.append(v); return this; }
			@Override public LogAppender<Writer> append(char[] v) throws IOException { w.write(v); return this; }
			@Override public LogAppender<Writer> append(char[] v, int offset, int len) throws IOException { w.write(v, offset, len); return this; }
			@	Override public LogAppender<Writer> append(CharSequence v) throws IOException { w.append(v); return this; }
			@Override public LogAppender<Writer> append(CharSequence v, int offset, int len) throws IOException {
				if (v instanceof String) {
					w.write((String) v, offset, len);
				} else {
					w.append(v, offset, offset + len);
				}
				return this;
			}
			@Override public Writer underlying() { return w; }
		};
	}

	static LogAppender<PrintStream> over(PrintStream ps) {
		if (ps == null) throw new IllegalArgumentException("null ps");
		return new LogAppender<PrintStream>() {
			@Override public LogAppender<PrintStream> appendNewline() { ps.println(); return this;}
			@Override public LogAppender<PrintStream> append(byte    v) { ps.print(v); return this; }
			@Override public LogAppender<PrintStream> append(short   v) { ps.print(v); return this; }
			@Override public LogAppender<PrintStream> append(int     v) { ps.print(v); return this; }
			@Override public LogAppender<PrintStream> append(long    v) { ps.print(v); return this; }
			@Override public LogAppender<PrintStream> append(boolean v) { ps.print(v); return this; }
			@Override public LogAppender<PrintStream> append(char    v) { ps.print(v); return this; }
			@Override public LogAppender<PrintStream> append(float   v) { ps.print(v); return this; }
			@Override public LogAppender<PrintStream> append(double  v) { ps.print(v); return this; }
			@Override public LogAppender<PrintStream> append(Object  v) { ps.print(v); return this; }
			@Override public LogAppender<PrintStream> append(String  v) { ps.print(v); return this; }
			@Override public LogAppender<PrintStream> append(char[]  v) { ps.print(v); return this; }
			@Override public LogAppender<PrintStream> append(char[] v, int offset, int len) { ps.print(new String(v, offset, len)); return this; }
			@Override public LogAppender<PrintStream> append(CharSequence v, int offset, int len) { ps.append(v, offset, offset + len); return this; }
			@Override public LogAppender<PrintStream> append(CharSequence v) { ps.append(v); return this; }
			@Override public PrintStream underlying() { return ps; }
		};
	}

	static LogAppender<PrintWriter> over(PrintWriter pw) {
		if (pw == null) throw new IllegalArgumentException("null pw");
		return new LogAppender<PrintWriter>() {
			@Override public LogAppender<PrintWriter> appendNewline() { pw.println(); return this;}
			@Override public LogAppender<PrintWriter> append(byte    v) { pw.print(v); return this; }
			@Override public LogAppender<PrintWriter> append(short   v) { pw.print(v); return this; }
			@Override public LogAppender<PrintWriter> append(int     v) { pw.print(v); return this; }
			@Override public LogAppender<PrintWriter> append(long    v) { pw.print(v); return this; }
			@Override public LogAppender<PrintWriter> append(boolean v) { pw.print(v); return this; }
			@Override public LogAppender<PrintWriter> append(char    v) { pw.print(v); return this; }
			@Override public LogAppender<PrintWriter> append(float   v) { pw.print(v); return this; }
			@Override public LogAppender<PrintWriter> append(double  v) { pw.print(v); return this; }
			@Override public LogAppender<PrintWriter> append(Object  v) { pw.print(v); return this; }
			@Override public LogAppender<PrintWriter> append(String  v) { pw.print(v); return this; }
			@Override public LogAppender<PrintWriter> append(char[]  v) { pw.print(v); return this; }
			@Override public LogAppender<PrintWriter> append(char[] v, int offset, int len) { pw.print(new String(v, offset, len)); return this; }
			@Override public LogAppender<PrintWriter> append(CharSequence v, int offset, int len) { pw.append(v, offset, offset + len); return this; }
			@Override public LogAppender<PrintWriter> append(CharSequence v) { pw.append(v); return this; }
			@Override public PrintWriter underlying() { return pw; }
		};
	}

	static LogAppender<Appendable> over(Appendable a) {
		if (a == null) throw new IllegalArgumentException("null a");
		return new LogAppender<Appendable>() {
			@Override public LogAppender<Appendable> append(char v) throws IOException { a.append(v); return this; }
			@Override public LogAppender<Appendable> append(String v) throws IOException { a.append(v); return this; }
			@Override public LogAppender<Appendable> append(CharSequence v) throws IOException { a.append(v); return this; }
			@Override public LogAppender<Appendable> append(CharSequence v, int offset, int len) throws IOException {
				a.append(v, offset, offset + len); return this;
			}
			@Override public Appendable underlying() { return a; }
		};
	}

	default LogAppender<T> appendNewline() throws IOException {
		return append(Logging.lineSeparator);
	}

	default LogAppender<T> append(byte v) throws IOException {
		return append(Byte.toString(v));
	}

	default LogAppender<T> append(short v) throws IOException {
		return append(Short.toString(v));
	}

	default LogAppender<T> append(int v) throws IOException {
		return append(Integer.toString(v));
	}

	default LogAppender<T> append(long v) throws IOException {
		return append(Long.toString(v));
	}

	default LogAppender<T> append(boolean v) throws IOException {
		return append(Boolean.toString(v));
	}

	default LogAppender<T> append(char v) throws IOException {
		return append(Character.toString(v));
	}

	default LogAppender<T> append(float v) throws IOException {
		return append(Float.toString(v));
	}

	default LogAppender<T> append(double v) throws IOException {
		return append(Double.toString(v));
	}

	default LogAppender<T> append(Object obj) throws IOException {
		return append(obj == null ? "null" : obj.toString());
	}

	default LogAppender<T> append(String v) throws IOException {
		return append((CharSequence) v);
	}

	default LogAppender<T> append(char[] v) throws IOException {
		return append(new String(v));
	}

	default LogAppender<T> append(char[] v, int offset, int len) throws IOException {
		return append(new String(v, offset, len));
	}

	default LogAppender<T> append(CharSequence v, int offset, int len) throws IOException {
		return append(v.subSequence(offset, offset + len));
	}

	LogAppender<T> append(CharSequence v) throws IOException;

	T underlying();
}
