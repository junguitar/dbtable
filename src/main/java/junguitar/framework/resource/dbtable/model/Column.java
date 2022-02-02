package junguitar.framework.resource.dbtable.model;

import lombok.Data;

@Data
public class Column {
	private String tableName;
	private String name;
	private String dataType;
	private boolean primaryKey;
	private int length;
	private int scale;
	private boolean ref;
	private String refTableName;
	private String refColumnName;
	private String comment;
}
