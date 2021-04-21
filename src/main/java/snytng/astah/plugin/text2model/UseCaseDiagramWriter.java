package snytng.astah.plugin.text2model;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.editor.BasicModelEditor;
import com.change_vision.jude.api.inf.editor.TransactionManager;
import com.change_vision.jude.api.inf.editor.UseCaseDiagramEditor;
import com.change_vision.jude.api.inf.editor.UseCaseModelEditor;
import com.change_vision.jude.api.inf.exception.InvalidEditingException;
import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.exception.ProjectNotFoundException;
import com.change_vision.jude.api.inf.model.IAssociation;
import com.change_vision.jude.api.inf.model.IClass;
import com.change_vision.jude.api.inf.model.IExtend;
import com.change_vision.jude.api.inf.model.IInclude;
import com.change_vision.jude.api.inf.model.IPackage;
import com.change_vision.jude.api.inf.model.IUseCase;
import com.change_vision.jude.api.inf.model.IUseCaseDiagram;
import com.change_vision.jude.api.inf.presentation.INodePresentation;
import com.change_vision.jude.api.inf.project.ProjectAccessor;

public class UseCaseDiagramWriter implements DiagramWriter {

	private enum RelationType {ASSOCIATION, INCLUDE, EXTEND}

	/**
	 * logger
	 */
	static final Logger logger = Logger.getLogger(UseCaseDiagramWriter.class.getName());
	static {
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.CONFIG);      
		logger.addHandler(consoleHandler);
		logger.setUseParentHandlers(false);
	}

	private ProjectAccessor projectAccessor = null;

	private UseCaseDiagramWriter() {
		try {
			projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
		} catch (Exception e){
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}

	private static UseCaseDiagramWriter instance = null;

	public static UseCaseDiagramWriter getInstance(){
		if(instance == null){
			instance = new UseCaseDiagramWriter();
		}
		return instance;
	}


	private boolean createNewDiagram = false;

	public void setCreateNewDiagram(boolean b){
		this.createNewDiagram = b;
	}

	public void removeAllPresentation(IUseCaseDiagram usecaseDiagram){

		createNewDiagram = false;

		try {

			// UseCaseDiagramEditorを取得する。
			UseCaseDiagramEditor ade = projectAccessor.getDiagramEditorFactory().getUseCaseDiagramEditor();

			TransactionManager.beginTransaction();

			Arrays.stream(usecaseDiagram.getPresentations())
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

	public void createUseCaseDiagram(FunctionCreator f)
			throws InvalidUsingException, InvalidEditingException {

		// UseCaseDiagramEditorを取得する。
		UseCaseDiagramEditor ude = projectAccessor.getDiagramEditorFactory().getUseCaseDiagramEditor();
		UseCaseModelEditor ume = projectAccessor.getModelEditorFactory().getUseCaseModelEditor();
		BasicModelEditor bme = projectAccessor.getModelEditorFactory().getBasicModelEditor();

		IUseCaseDiagram usecaseDiagram = (IUseCaseDiagram)f.diagram;

		if(createNewDiagram){
			removeAllPresentation(usecaseDiagram);
		}

		// アクティビティ図をアクティビティ図エディタにセット
		ude.setDiagram(usecaseDiagram);

		if(f.isAssociation){

			IPackage pkg = (IPackage)usecaseDiagram.getOwner();

			// アクターの場所を決める
			double ymax = Arrays.stream(usecaseDiagram.getPresentations())
					.filter(p -> p.getModel() instanceof IClass)
					.map(INodePresentation.class::cast)
					.map(np -> np.getLocation().getY())
					.max(Comparator.naturalOrder())
					.orElse(0d);
			
			double xactor = 0;
			double yactor = ymax + 100;
			Point2D p2dactor = new Point2D.Double(xactor, yactor);

			// アクターを作成する
			try {
				TransactionManager.beginTransaction();

				Optional<IClass> oactor = Arrays.stream(projectAccessor.findElements(IClass.class))
						.map(IClass.class::cast)
						.filter(c -> Arrays.asList(c.getStereotypes()).contains("actor"))
						.filter(c -> c.getOwner() == pkg && c.getName().equals(f.subjectName))
						.findFirst();				
				IClass actor = oactor.isPresent() ? 
						oactor.get() : ume.createActor(pkg, f.subjectName); 

				Optional<INodePresentation> oactorNode = Arrays.stream(usecaseDiagram.getPresentations())
						.filter(p -> p.getModel() == actor)
						.map(INodePresentation.class::cast)
						.findFirst();
				INodePresentation actorNode = oactorNode.isPresent() ? 
						oactorNode.get() : ude.createNodePresentation(actor, p2dactor);

				// f.relationNameに含まれる　>, <I>, <E>で場合分け
				RelationType nextRelationType = null;
				RelationType relationType = RelationType.ASSOCIATION;
				IUseCase lastUseCase = null;
				INodePresentation lastUseCaseNode = null;
				
				for(String relationName : f.relationName.split(">")){

					if(! relationName.contains("<")){
						nextRelationType = RelationType.ASSOCIATION;					
					} else {
						if(relationName.endsWith("<I")){ // <I
							nextRelationType = RelationType.INCLUDE;
						} else { // <E only
							nextRelationType = RelationType.EXTEND;				
						}
						relationName = relationName.substring(0, relationName.indexOf('<'));
					}
					final String modifiedRelationName = f.objectName + relationName; 

					// ユースケースの場所を決める
					INodePresentation lastuc = null;
					double lastucymax = 0;
					for(INodePresentation np : Arrays.stream(usecaseDiagram.getPresentations())
							.filter(p -> p.getModel() instanceof IUseCase)
							.map(INodePresentation.class::cast)
							.collect(Collectors.toList())){
						Point2D p2d = np.getLocation();
						if(lastucymax < p2d.getY()){
							lastucymax = p2d.getY();
							lastuc = np;
						}
					}

					// ユースケースが存在しない場合
					Point2D p2d = actorNode.getLocation();
					Point2D lastnodep2d = p2d;
					int xoffset = 100;
					int yoffset = 0;
					// ユースケースが存在する場合
					if(lastuc != null){
						lastnodep2d = lastuc.getLocation();
						if(relationType == RelationType.ASSOCIATION){
							xoffset = 10;
							yoffset = 100;
						} else {
							xoffset = 300;
							yoffset = 10;

						}
					}
					Point2D usecasep2d = new Point2D.Double(lastnodep2d.getX() + xoffset, lastnodep2d.getY() + yoffset);

					// ユースケースを作成

					Optional<IUseCase> ousecase = Arrays.stream(projectAccessor.findElements(IUseCase.class))
							.map(IUseCase.class::cast)
							.filter(uc -> uc.getOwner() == pkg && uc.getName().equals(modifiedRelationName))
							.findFirst();
					IUseCase usecase = ousecase.isPresent() ? 
							ousecase.get() : ume.createUseCase(pkg, modifiedRelationName);

					Optional<INodePresentation> ousecaseNode = Arrays.stream(usecaseDiagram.getPresentations())
							.filter(p -> p.getModel() == usecase)
							.map(INodePresentation.class::cast)
							.findFirst();
					INodePresentation usecaseNode = ousecaseNode.isPresent() ? 
							ousecaseNode.get() : ude.createNodePresentation(usecase, usecasep2d);

					// アクターから関連線を引く
					if(relationType == RelationType.ASSOCIATION){
						careteAssociation(ude, bme, usecaseDiagram, actor, actorNode, usecase, usecaseNode);


					} else if(relationType == RelationType.INCLUDE){
						careteInclude(ude, ume, usecaseDiagram, usecase, usecaseNode, lastUseCase, lastUseCaseNode);


					} else if(relationType == RelationType.EXTEND){
						createExtend(ude, ume, usecaseDiagram, usecase, usecaseNode, lastUseCase, lastUseCaseNode);
					}

					// 次のための情報を保存
					relationType = nextRelationType;
					lastUseCase = usecase;
					lastUseCaseNode = usecaseNode;

				}

				TransactionManager.endTransaction();
			} catch (Exception e) {
				TransactionManager.abortTransaction();
				logger.log(Level.WARNING, e.getMessage(), e);
			}

		}
	}

	private void createExtend(
			final UseCaseDiagramEditor ude, 
			final UseCaseModelEditor ume, 
			final IUseCaseDiagram usecaseDiagram,
			final IUseCase usecase, 
			final INodePresentation usecaseNode, 
			final IUseCase finalLastUseCase, 
			final INodePresentation finalLastUseCaseNode
			)
					throws ProjectNotFoundException, InvalidEditingException, InvalidUsingException {
		Optional<IExtend> oextend = Arrays.stream(projectAccessor.findElements(IExtend.class))
				.map(IExtend.class::cast)
				.filter(e -> e.getExtendedCase() == finalLastUseCase && e.getExtension() == usecase)
				.findFirst();
		IExtend extend = oextend.isPresent() ? oextend.get() : ume.createExtend(usecase, finalLastUseCase, "");

		if(Arrays.stream(usecaseDiagram.getPresentations())
				.noneMatch(p -> p.getModel() == extend)
				){
			ude.createLinkPresentation(extend, finalLastUseCaseNode, usecaseNode); // 矢印が反対むき？
		}
	}

	private void careteInclude(
			final UseCaseDiagramEditor ude, 
			final UseCaseModelEditor ume, 
			final IUseCaseDiagram usecaseDiagram,
			final IUseCase usecase, 
			final INodePresentation usecaseNode, 
			final IUseCase finalLastUseCase,
			final INodePresentation finalLastUseCaseNode
			)
					throws ProjectNotFoundException, InvalidEditingException, InvalidUsingException {
		Optional<IInclude> oinclude = Arrays.stream(projectAccessor.findElements(IInclude.class))
				.map(IInclude.class::cast)
				.filter(i -> i.getIncludingCase() == finalLastUseCase && i.getAddition() == usecase)
				.findFirst();
		IInclude include = oinclude.isPresent() ? oinclude.get() : ume.createInclude(finalLastUseCase, usecase, "");

		if(Arrays.stream(usecaseDiagram.getPresentations())
				.noneMatch(p -> p.getModel() == include)
				){
			ude.createLinkPresentation(include, finalLastUseCaseNode, usecaseNode); // 矢印が反対むき？
		}
	}

	private void careteAssociation(
			final UseCaseDiagramEditor ude, 
			final BasicModelEditor bme, 
			final IUseCaseDiagram usecaseDiagram,
			final IClass actor, 
			final INodePresentation actorNode, 
			final IUseCase usecase, 
			final INodePresentation usecaseNode
			)
					throws ProjectNotFoundException, InvalidEditingException, InvalidUsingException {
		Optional<IAssociation> oassociation = Arrays.stream(projectAccessor.findElements(IAssociation.class))
				.map(IAssociation.class::cast)
				.filter(a -> a.getMemberEnds()[0].getType() == actor && a.getMemberEnds()[1].getType() == usecase)
				.findFirst();
		IAssociation association = oassociation.isPresent() ? oassociation.get() : bme.createAssociation(actor, usecase, "", "", "");

		if(Arrays.stream(usecaseDiagram.getPresentations())
				.noneMatch(p -> p.getModel() == association)
				){
			ude.createLinkPresentation(association, actorNode, usecaseNode);
		}
	}


	public Supplier<String> getModeGetter(){
		return () -> "ユースケース図モード | 包含:～とき、いつも～、拡張：～とき、～";
	}

	@SuppressWarnings("unused")
	private boolean nounMode = false;
	public void setNounMode(boolean b){
		this.nounMode = b;
	}

	public Function<String, String[][]> getTextAanalyzer(){
		// ユースケース図の場合には、主語と述部を分ける
		return Morpho::getSubFunctions;
	}

	public Consumer<FunctionCreator> getFunctionVisualizer(){
		return u -> { 
			if(u.subjectName.isEmpty()){
				logger.log(Level.WARNING, "cannot add function because of argument(s) is empty.");
				return;
			}

			try {
				u.checkRelation(); 
				createUseCaseDiagram(u);				

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
