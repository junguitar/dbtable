package junguitar.framework.resource.dbtable.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(description = "DB Table DTO")
@Data
public class Table {
	@ApiModelProperty(value = "DB Table Name", position = 1)
	private String name;
	@ApiModelProperty(value = "DB Table Type", position = 2)
	private String type;
	@ApiModelProperty(value = "DB Engine", position = 3)
	private String engine;
	@ApiModelProperty(value = "DB Table Charset", position = 4)
	private String charset;
	@ApiModelProperty(value = "DB Table Collation", position = 5)
	private String collation;
	@ApiModelProperty(value = "The Number of Rows", position = 6)
	private long rows;
	@ApiModelProperty(value = "Whether This Table is Added or Not", position = 7)
	private boolean added;
	@ApiModelProperty(value = "DB Table Comment", position = 8)
	private String comment;
	@ApiModelProperty(value = "DB Table Columns", position = 9)
	private List<Column> columns = new ArrayList<Column>();
}
