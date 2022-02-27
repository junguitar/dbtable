package junguitar.framework.resource.dbtable.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class Table {
	@ApiModelProperty(value = "DB Table Name", position = 1)
	private String name;
	@ApiModelProperty(value = "DB Table Type", position = 2)
	private String type;
	@ApiModelProperty(value = "The Number of Rows", position = 3)
	private long rows;
	@ApiModelProperty(value = "Whether This Table is Added or Not", position = 4)
	private boolean added;
	@ApiModelProperty(value = "DB Table Comment", position = 5)
	private String comment;
	@ApiModelProperty(value = "DB Table Columns", position = 6)
	private List<Column> columns = new ArrayList<Column>();
}
