package junguitar.framework.resource.dbtable.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaRef {
	private String location;
	private String name;

	public SchemaRef(String name) {
		this.name = name;
	}
}
