package junguitar.framework.resource.dbtable.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class Column {
	@ApiModelProperty(value = "DB Table Name of This Column", position = 1)
	private String tableName;
	@ApiModelProperty(value = "Column Name", position = 2)
	private String name;
	@ApiModelProperty(value = "Column Data Type", position = 3)
	private String dataType;
	@ApiModelProperty(value = "Whether This Column is Primary Key or Not", position = 4)
	private boolean primaryKey;
	@ApiModelProperty(value = "Column Length by This Data Type", position = 5)
	private int length;
	@ApiModelProperty(value = "Column Scale by This Data Type", position = 6)
	private int scale;
	@ApiModelProperty(value = "Whether This Column Refers Other Column or Not", position = 7)
	private boolean ref;
	@ApiModelProperty(value = "Reference Table Name", position = 8)
	private String refTableName;
	@ApiModelProperty(value = "Reference Column Name", position = 9)
	private String refColumnName;
	@ApiModelProperty(value = "Whether This Column is Added or Not", position = 10)
	private boolean added;
	@ApiModelProperty(value = "Column Comment", position = 11)
	private String comment;
}
