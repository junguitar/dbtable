package junguitar.framework.dbtable.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Table {
	private String name;
	private List<Column> columns = new ArrayList<Column>();
}
