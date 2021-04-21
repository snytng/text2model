package snytng.astah.plugin.text2model;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.editor.ActivityDiagramEditor;
import com.change_vision.jude.api.inf.editor.TransactionManager;
import com.change_vision.jude.api.inf.exception.InvalidEditingException;
import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.model.IActivityDiagram;
import com.change_vision.jude.api.inf.model.IPartition;
import com.change_vision.jude.api.inf.presentation.INodePresentation;
import com.change_vision.jude.api.inf.project.ProjectAccessor;

public class ActivityDiagramWriter implements DiagramWriter {

	/**
	 * logger
	 */
	static final Logger logger = Logger.getLogger(ActivityDiagramWriter.class.getName());
	static {
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.CONFIG);      
		logger.addHandler(consoleHandler);
		logger.setUseParentHandlers(false);
	}

	private ProjectAccessor projectAccessor = null;

	private ActivityDiagramWriter() {
		try {
			projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
		} catch (Exception e){
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}

	private static ActivityDiagramWriter instance = null;

	public static ActivityDiagramWriter getInstance(){
		if(instance == null){
			instance = new ActivityDiagramWriter();
		}
		return instance;
	}


	private boolean createNewActivityDiagram = false;
	
	public void setCreateNewActivityDiagram(boolean b){
		this.createNewActivityDiagram = b;
	}

	public void removeAllAction(IActivityDiagram activityDiagram){
		
		lastAction = null;
		createNewActivityDiagram = false;

		try {
			
			// ActivityDiagramEditorを取得する。
			ActivityDiagramEditor ade = projectAccessor.getDiagramEditorFactory().getActivityDiagramEditor();

			TransactionManager.beginTransaction();
		
			Arrays.stream(activityDiagram.getPresentations())
			.forEach(p -> {
				try {
					ade.deletePresentation(p);
				}catch(InvalidEditingException ex){
					logger.log(Level.WARNING, ex.getMessage(), ex);
				}
			});

			TransactionManager.endTransaction();
				
		}catch(Exception e){
			TransactionManager.abortTransaction();
			logger.log(Level.WARNING, e.getMessage(), e);
		}
		
	}
	
	private INodePresentation lastAction = null;

	public void createActivityDiagram(FunctionCreator f)
			throws InvalidUsingException, InvalidEditingException {
		
		// ActivityDiagramEditorを取得する。
		ActivityDiagramEditor ade = projectAccessor.getDiagramEditorFactory().getActivityDiagramEditor();
		
		IActivityDiagram activityDiagram = (IActivityDiagram)f.diagram;
		
		if(createNewActivityDiagram){
			removeAllAction(activityDiagram);
		}

		// アクティビティ図をアクティビティ図エディタにセット
		ade.setDiagram(activityDiagram);

		if(f.isAssociation){

			INodePresentation np = Arrays.stream(activityDiagram.getPresentations())
					.filter(p -> p.getModel() instanceof IPartition)
					.map(INodePresentation.class::cast)
					.filter(p -> p.getLabel().equals(f.subjectName))
					.findFirst()
					.orElseGet(() -> {
						INodePresentation nnp = null;
						try {
							TransactionManager.beginTransaction();
							nnp = ade.createPartition(null, null, f.subjectName, false);
							TransactionManager.endTransaction();
						} catch (Exception e) {
							TransactionManager.abortTransaction();
						}
						return nnp;
					});

			// アクションの場所を決める
			Point2D p2d = np.getLocation();
			Point2D lastactionp2d = p2d;
			if(lastAction != null){
				lastactionp2d = lastAction.getLocation();
			}
			Point2D actionp2d = new Point2D.Double(p2d.getX() + 10, lastactionp2d.getY() + 50);

			// アクションを作成
			TransactionManager.beginTransaction();
			INodePresentation action = ade.createAction(f.objectName + f.relationName, actionp2d);
			TransactionManager.endTransaction();

			// 前のアクションが存在して、から線を引く
			if(lastAction != null 
					&& 
					Arrays.stream(activityDiagram.getPresentations())
					.filter(p -> p instanceof INodePresentation)
					.anyMatch(p -> p == lastAction)
					){
				TransactionManager.beginTransaction();
				ade.createFlow(lastAction, action); // 返値のILinkPresentationの処理はしない
				TransactionManager.endTransaction();
			}

			// アクションを保持
			lastAction = action;
		}
	}
	
	
	public Supplier<String> getModeGetter(){
		return () -> "アクティビティ図モード | AはBだ　→　A＝スイムレーン B＝アクティビティ";
	}
	
	@SuppressWarnings("unused")
	private boolean nounMode = false;
	public void setNounMode(boolean b){
		this.nounMode = b;
	}
	
	public Function<String, String[][]> getTextAanalyzer(){
		// アクティビティ図の場合には、主語と述部を分ける
		return Morpho::getSubFunction;
	}
	
	public Consumer<FunctionCreator> getFunctionVisualizer(){
		return f -> { 
			if(f.subjectName.isEmpty()){
				logger.log(Level.WARNING, "cannot add function because of argument(s) is empty.");
				return;
			}

			try {
				f.checkRelation(); 
				createActivityDiagram(f);				

			} catch (Exception e) {
				logger.log(Level.WARNING, e.getMessage(), e);
			}

		};
	}

	@Override
	public void setSequenceDiagramMode(boolean b) {
		// no action
	}
}
