package junguitar.framework.resource.dbtable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import junguitar.framework.resource.dbtable.model.Table;
import junguitar.framework.resource.dbtable.service.DbtableService;
import junguitar.framework.resource.dbtable.service.comment.CommentService;
import junguitar.framework.resource.dbtable.service.table.TableService;
import junguitar.framework.resource.dbtable.util.CollectionOut;

@RestController
public class DbtableControllerImpl implements DbtableController {
	@Autowired
	private DbtableService service;

	@Autowired
	private TableService tableService;

	@Autowired
	private CommentService commentService;

	@Override
	public CollectionOut<Table> getCollection(String schemaName) {
		return tableService.getColletion(schemaName);
	}

	@Override
	public String info(String schemaName, String sheetPath, String sheetName, String dictionaySheetName) {
		return service.info(schemaName, sheetPath, sheetName, dictionaySheetName);
	}

	@Override
	public String infoColumnsDictionaries(String schemaName, String sheetPath, String sheetName,
			String tablesSheetName) {
		return service.infoColumnsDictionaries(schemaName, sheetPath, sheetName, tablesSheetName);
	}

	@Override
	public String infoCommentListByExcel(String sheetPath, String sheetName) {
		return commentService.infoListByExcel(sheetPath, sheetName);
	}

//	@PutMapping("comments/by-excel")
//	public void putCommentListByExcel(String path,
//			String sheetName) {
//		commentService.putCommentListByExcel(path, sheetName);
//	}

}
