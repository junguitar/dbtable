package junguitar.framework.resource.dbtable;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import junguitar.framework.resource.dbtable.model.Table;
import junguitar.framework.resource.dbtable.util.CollectionOut;

@RequestMapping("api/framework/dbtables")
public interface DbtableController {

	@GetMapping
	CollectionOut<Table> getCollection(@RequestParam(required = true) String schemaName);

	@GetMapping("info")
	String info(@RequestParam(required = true) String schemaName, @RequestParam(required = false) String sheetPath,
			@RequestParam(required = false) String sheetName,
			@RequestParam(required = false) String dictionaySheetName);

	@GetMapping("columns/dictionaries/info")
	String infoColumnsDictionaries(@RequestParam(required = true) String schemaName,
			@RequestParam(required = false) String sheetPath, @RequestParam(required = false) String sheetName,
			@RequestParam(required = false) String tablesSheetName);

	@GetMapping("comments/info-by-excel")
	String infoCommentListByExcel(@RequestParam(required = true) String sheetPath,
			@RequestParam(required = true) String sheetName);
}
