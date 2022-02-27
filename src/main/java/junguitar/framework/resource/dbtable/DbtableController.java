package junguitar.framework.resource.dbtable;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import junguitar.framework.resource.dbtable.dto.Table;
import junguitar.framework.resource.dbtable.util.CollectionOut;

@Api(protocols = "http, https", tags = "DB Table APIs", description = "DB Table APIs those are provided in this url: v1/framework/dbtables")
@RequestMapping("v1/framework/dbtables")
public interface DbtableController {
	static final String SCHEMA_NAME = "Schema Name";
	static final String EXTERNAL_SCHEMA_NAME = "External Schema Name";
	static final String SHEET_PATH = "Spread Sheet (Excel File) Path";
	static final String TABLES_SHEET_NAME = "Tables Sheet Name  in the Excel File";
	static final String DICTIONARY_SHEET_NAME = "Dictionary Sheet Name in the Excel File";

	@ApiOperation(value = "Get DB Tables")
	@GetMapping
	CollectionOut<Table> getCollection(@RequestParam(required = true) @ApiParam(value = SCHEMA_NAME) String schemaName);

	@ApiOperation(value = "Get Information of DB Tables Differences between 2 DB Schemas")
	@GetMapping("info-diff")
	String infoDiff(@RequestParam(required = true) @ApiParam(value = SCHEMA_NAME) String schemaName,
			@RequestParam(required = true) @ApiParam(value = EXTERNAL_SCHEMA_NAME) String externalSchemaName);

	@ApiOperation(value = "Get Information of All DB Tables in a DB Schema")
	@GetMapping("info")
	String info(@RequestParam(required = true) @ApiParam(value = SCHEMA_NAME) String schemaName,
			@RequestParam(required = false) @ApiParam(value = SHEET_PATH) String sheetPath,
			@RequestParam(required = false) @ApiParam(value = TABLES_SHEET_NAME) String sheetName,
			@RequestParam(required = false) @ApiParam(value = DICTIONARY_SHEET_NAME) String dictionaySheetName);

	@ApiOperation(value = "Get Information of All Columns those are used by DB Tables in a DB Schema")
	@GetMapping("columns/dictionaries/info")
	String infoColumnsDictionaries(@RequestParam(required = true) @ApiParam(value = SCHEMA_NAME) String schemaName,
			@RequestParam(required = false) @ApiParam(value = SHEET_PATH) String sheetPath,
			@RequestParam(required = false) @ApiParam(value = DICTIONARY_SHEET_NAME) String sheetName,
			@RequestParam(required = false) @ApiParam(value = TABLES_SHEET_NAME) String tablesSheetName);

	@ApiOperation(value = "Get Information of All Comments those are written in Excel File")
	@GetMapping("comments/info-by-excel")
	String infoCommentListByExcel(@RequestParam(required = true) @ApiParam(value = SHEET_PATH) String sheetPath,
			@RequestParam(required = true) @ApiParam(value = TABLES_SHEET_NAME) String sheetName);

}
