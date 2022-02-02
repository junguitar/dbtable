package junguitar.framework.resource.dbtable.util;

public class DbtableUtils {

	public static void appendRow(StringBuilder buf, Object... values) {
		int i = 0;
		for (Object value : values) {
			buf.append(i++ == 0 ? "\r\n" : "\t");
			if (value != null) {
				buf.append(value);
			}
		}
	}

}
