package snytng.astah.plugin.text2model;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.editor.StateMachineDiagramEditor;
import com.change_vision.jude.api.inf.editor.TransactionManager;
import com.change_vision.jude.api.inf.exception.InvalidEditingException;
import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.model.IFinalState;
import com.change_vision.jude.api.inf.model.IPseudostate;
import com.change_vision.jude.api.inf.model.IStateMachineDiagram;
import com.change_vision.jude.api.inf.model.ITransition;
import com.change_vision.jude.api.inf.model.IVertex;
import com.change_vision.jude.api.inf.presentation.ILinkPresentation;
import com.change_vision.jude.api.inf.presentation.INodePresentation;
import com.change_vision.jude.api.inf.project.ProjectAccessor;

public class StateMachineDiagramWriter implements DiagramWriter {

	/**
	 * logger
	 */
	static final Logger logger = Logger.getLogger(StateMachineDiagramWriter.class.getName());
	static {
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.CONFIG);      
		logger.addHandler(consoleHandler);
		logger.setUseParentHandlers(false);
	}

	private ProjectAccessor projectAccessor = null;

	private StateMachineDiagramWriter() {
		try {
			projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
		} catch (Exception e){
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}

	private static StateMachineDiagramWriter instance = null;

	public static StateMachineDiagramWriter getInstance(){
		if(instance == null){
			instance = new StateMachineDiagramWriter();
		}
		return instance;
	}

	public void createStateMachineDiagram(FunctionCreator f)
			throws InvalidUsingException, InvalidEditingException {

		// StateMachineDiagramEditorを取得する。
		StateMachineDiagramEditor sde = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
		//BasicModelEditor bme = projectAccessor.getModelEditorFactory().getBasicModelEditor();

		IStateMachineDiagram stateMachineDiagram = (IStateMachineDiagram)f.diagram;

		// ステートマシン図エディタにセット
		sde.setDiagram(stateMachineDiagram);

		// 状態を作成する
		try {
			TransactionManager.beginTransaction();
			INodePresentation fromStateNode = createState(stateMachineDiagram, sde, f.subjectName);
			TransactionManager.endTransaction();
			
			TransactionManager.beginTransaction();
			INodePresentation toStateNode = createState(stateMachineDiagram, sde, f.relationName);		
			TransactionManager.endTransaction();
			
			TransactionManager.beginTransaction();
			/* ILinkPresentation transitionLink = */ createTransition(stateMachineDiagram, sde, f.objectName, fromStateNode, toStateNode);		
			TransactionManager.endTransaction();
			
		} catch (Exception e) {
			TransactionManager.abortTransaction();
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}

	/**
	 * 新しい状態を作成する場所を決める
	 */
	private double[][] shiftPosition = {{100, 0}, {0,100}};
	private int shiftIndex = 0;
	private Point2D getNewLocation(IStateMachineDiagram stateMachineDiagram) throws InvalidUsingException{
		List<INodePresentation> nps = Arrays.stream(stateMachineDiagram.getPresentations())
				.filter(INodePresentation.class::isInstance)
				.map(INodePresentation.class::cast)
				.collect(Collectors.toList());
		
		double xstate = 0;
		double ystate = 0;

		if(! nps.isEmpty()){
			double xmax = 0;
			double xmin = 0;
			double ymax = 0;
			double ymin = 0;
			for(INodePresentation np : nps){
				Point2D p = np.getLocation();
				xmax = Math.max(xmax, p.getX());
				xmin = Math.min(xmin, p.getX());
				ymax = Math.max(ymax, p.getY());					
				ymin = Math.min(ymin, p.getY());					
			}
			
			xstate = xmax + shiftPosition[shiftIndex][0];
			ystate = ymax + shiftPosition[shiftIndex][1];
			shiftIndex = (shiftIndex + 1) % shiftPosition.length;
			
		} else {
			shiftIndex = 0;
		}
		
		return new Point2D.Double(xstate, ystate);
	}
	
	private INodePresentation createState(IStateMachineDiagram stateMachineDiagram, StateMachineDiagramEditor sde, String name) throws InvalidEditingException, InvalidUsingException {
		INodePresentation stateNode = null;

		if(name.equals("最初")){
			Optional<INodePresentation> ostateNode = Arrays.stream(stateMachineDiagram.getPresentations())
					.filter(p -> p.getModel() instanceof IPseudostate)
					.filter(p -> ((IPseudostate)p.getModel()).isInitialPseudostate())
					.map(INodePresentation.class::cast)
					.findFirst();
			if(ostateNode.isPresent()){
				stateNode = ostateNode.get();
			} else {
				stateNode = sde.createInitialPseudostate(null, getNewLocation(stateMachineDiagram));
			}
			
		} else if(name.equals("最後")){
			Optional<INodePresentation> ostateNode = Arrays.stream(stateMachineDiagram.getPresentations())
					.filter(p -> p.getModel() instanceof IFinalState)
					.map(INodePresentation.class::cast)
					.findFirst();
			if(ostateNode.isPresent()){
				stateNode = ostateNode.get();
			} else {
				stateNode = sde.createFinalState(null, getNewLocation(stateMachineDiagram));
			}
			
		} else {
			Optional<INodePresentation> ostateNode = Arrays.stream(stateMachineDiagram.getPresentations())
					.filter(p -> p.getModel() instanceof IVertex)
					.filter(p -> p.getLabel().equals(name))
					.map(INodePresentation.class::cast)
					.findFirst();

			if(ostateNode.isPresent()){
				stateNode = ostateNode.get();
			} else {
				stateNode = sde.createState(name, null, getNewLocation(stateMachineDiagram));
			}
		}
		
		logger.log(Level.INFO, "create " + stateNode.getLabel());	
		return stateNode;
	}

	private ILinkPresentation createTransition(IStateMachineDiagram stateMachineDiagram, StateMachineDiagramEditor sde, String name, INodePresentation from, INodePresentation to) throws InvalidEditingException, InvalidUsingException {
		Optional<ILinkPresentation> otransition = Arrays.stream(stateMachineDiagram.getPresentations())
				.filter(p -> p.getModel() instanceof ITransition)
				.filter(p -> {
					ITransition t = (ITransition)p.getModel();
					return (t.getSource() == from.getModel() && t.getTarget() == to.getModel() && t.getName().equals(name));
					}) 
				.map(ILinkPresentation.class::cast)
				.findFirst();
		
		ILinkPresentation transitionLink = null;
		if(otransition.isPresent()){
			transitionLink = otransition.get();
		} else {
			transitionLink = sde.createTransition(from, to);
			if(name != null){
				transitionLink.setLabel(name);
			}
		}
		
		logger.log(Level.INFO, "create " + transitionLink.getLabel());	
		return transitionLink;
	}


	public Supplier<String> getModeGetter(){
		return () -> "ステートマシン図モード | （前状態）は、（イベント名）で、（後状態）に遷移する";
	}

	public Function<String, String[][]> getTextAanalyzer(){
		// ステートマシン図の場合には、主語と直接目的語、間接目的語と述部を分ける
		return Morpho::getSVOO;
	}

	public Consumer<FunctionCreator> getFunctionVisualizer(){
		return u -> { 
			if(u.subjectName.isEmpty()){
				logger.log(Level.WARNING, "cannot add function because of argument(s) is empty.");
				return;
			}

			try {
				createStateMachineDiagram(u);				

			} catch (Exception e) {
				logger.log(Level.WARNING, e.getMessage(), e);
			}

		};
	}

	@Override
	public void setSequenceDiagramMode(boolean b) {
		// no action
	}

	@Override
	public void setNounMode(boolean b) {
		// no action
	}
}
