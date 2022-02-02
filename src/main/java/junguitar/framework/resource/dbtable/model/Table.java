package junguitar.framework.resource.dbtable.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Table {
	private String name;
	private String type;
	private long rows;
	private boolean added;
	private String comment;
	private List<Column> columns = new ArrayList<Column>();
}
