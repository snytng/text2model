package snytng.astah.plugin.text2model;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.editor.SequenceDiagramEditor;
import com.change_vision.jude.api.inf.editor.TransactionManager;
import com.change_vision.jude.api.inf.exception.InvalidEditingException;
import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.exception.ProjectNotFoundException;
import com.change_vision.jude.api.inf.model.IClassDiagram;
import com.change_vision.jude.api.inf.model.ILifeline;
import com.change_vision.jude.api.inf.model.IMessage;
import com.change_vision.jude.api.inf.model.INamedElement;
import com.change_vision.jude.api.inf.model.ISequenceDiagram;
import com.change_vision.jude.api.inf.presentation.ILinkPresentation;
import com.change_vision.jude.api.inf.presentation.INodePresentation;
import com.change_vision.jude.api.inf.presentation.IPresentation;
import com.change_vision.jude.api.inf.project.ProjectAccessor;

public class SequenceDiagramWriter {

	/**
	 * logger
	 */
	static final Logger logger = Logger.getLogger(SequenceDiagramWriter.class.getName());
	static {
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.CONFIG);      
		logger.addHandler(consoleHandler);
		logger.setUseParentHandlers(false);
	}

	private ProjectAccessor projectAccessor = null;

	private SequenceDiagramWriter() {
		try {
			projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
		} catch (Exception e){
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}

	private static SequenceDiagramWriter instance = null;

	public static SequenceDiagramWriter getInstance(){
		if(instance == null){
			instance = new SequenceDiagramWriter();
		}
		return instance;
	}


	private boolean createNewSequenceDiagram = false;
	
	public void setCreateNewSequenceDiagram(boolean b){
		this.createNewSequenceDiagram = b;
	}
	
	public void createSequenceDiagram(IClassDiagram classDiagram, FunctionCreator f)
			throws InvalidUsingException, ProjectNotFoundException, InvalidEditingException {
		TransactionManager.beginTransaction();

		String sdName = classDiagram.getName() + " seq";
		logger.log(Level.INFO, () -> "SequenceDiagram container:" + classDiagram.getContainer());
		logger.log(Level.INFO, () -> "SequenceDiagram name:" + sdName);

		// ClassDiagramEditorを取得する。
		SequenceDiagramEditor sde = projectAccessor.getDiagramEditorFactory().getSequenceDiagramEditor();
		List<ISequenceDiagram> sds = Arrays.stream(projectAccessor.findElements(ISequenceDiagram.class))
				.map(ISequenceDiagram.class::cast)
				.collect(Collectors.toList());

		ISequenceDiagram sd = null;
		for(ISequenceDiagram d : sds){
			logger.log(Level.INFO, "diagram container:" + d.getContainer());
			logger.log(Level.INFO, "diagram name:" + d.getName());
			if(d.getContainer() == classDiagram.getContainer() && d.getName().equals(sdName)){
				sd = d;
				break;
			}
		}

		// 新規モデル作成モードのときには、既存モデルを削除する
		if(sd != null && createNewSequenceDiagram){
			sde.delete(sd);
			sd = null;
		}

		if(sd == null){
			sd = sde.createSequenceDiagram((INamedElement) classDiagram.getContainer(), sdName);
			createNewSequenceDiagram = false;
			logger.log(Level.INFO,"SequenceDiagram 新規 " + sd.getName());
		} else {
			logger.log(Level.INFO,"SequenceDiagram 既存 " + sd.getName());
		}

		TransactionManager.endTransaction();

		// シーケンス図をシーケンス図エディタにセット
		sde.setDiagram(sd);

		if(f.isAssociation){

			ILifeline subjectL = null;
			ILifeline objectL = null;
			INodePresentation subjectN = null;
			INodePresentation objectN = null;

			double maxL = 0d;
			double maxM = 0d;

			// subject
			for(IPresentation p : sd.getPresentations()){
				if(p.getModel() instanceof ILifeline){
					ILifeline l = (ILifeline)p.getModel();
					INodePresentation np = (INodePresentation)p;

					Point2D pl = np.getLocation();
					if(pl.getX() + np.getWidth() > maxL){
						maxL = pl.getX() + np.getWidth();
					}

					logger.log(Level.INFO, () -> "l:" + l.getBase().getName() + "<-> f.subjectName:" + f.subjectName);
					if(l.getBase().getName().equals(f.subjectName)){
						subjectL = l;
						subjectN = np;
					}

				} else if(p.getModel() instanceof IMessage){
					ILinkPresentation lp = (ILinkPresentation)p;
					for(Point2D pl : lp.getAllPoints()){
						if(pl.getY() > maxM){
							maxM = pl.getY();
						}
					}
				}
			}

			// subjectのライフラインがなければ作成する
			if(subjectL == null){
				TransactionManager.beginTransaction();

				maxL += 200;
				subjectN = sde.createLifeline("", maxL);
				subjectL = (ILifeline)subjectN.getModel();
				subjectL.setBase(f.subjectC);

				TransactionManager.endTransaction();
				logger.log(Level.INFO,"subjectL 新規 " + subjectL.getBase().getName());
			} else {
				logger.log(Level.INFO,"subjectL 既存" + subjectL.getBase().getName());						
			}

			// object
			for(IPresentation p : sd.getPresentations()){
				if(p.getModel() instanceof ILifeline){
					ILifeline l = (ILifeline)p.getModel();
					INodePresentation np = (INodePresentation)p;
					Point2D pl = np.getLocation();
					if(pl.getX() + np.getWidth() > maxL){
						maxL = pl.getX() + np.getWidth();
					}

					logger.log(Level.INFO, () -> "l:" + l.getBase().getName() + "<-> f.objectName:" + f.objectName);
					if(l.getBase().getName().equals(f.objectName)){
						objectL = l;
						objectN = np;
					}

				} else if(p.getModel() instanceof IMessage){
					ILinkPresentation lp = (ILinkPresentation)p;
					for(Point2D pl : lp.getAllPoints()){
						if(pl.getY() > maxM){
							maxM = pl.getY();
						}
					}
				}
			}

			// objectのライフラインがなければ作る
			if(objectL == null){
				TransactionManager.beginTransaction();

				maxL += 200;
				objectN = sde.createLifeline("", maxL);
				objectL = (ILifeline)objectN.getModel();
				objectL.setBase(f.objectC);

				TransactionManager.endTransaction();
				logger.log(Level.INFO,"objectL 新規 " + objectL.getBase().getName());
			} else {
				logger.log(Level.INFO,"objectL 既存" + objectL.getBase().getName());
			}

			// message
			// subjectLの最後のMessageを取得
			double lastM = 0;
			INodePresentation lastMessageN = null;
			for(IPresentation p : sd.getPresentations()){
				if(p.getModel() instanceof IMessage){
					IMessage m = (IMessage)p.getModel();
					ILinkPresentation lp = (ILinkPresentation)p;
					if(m.getTarget() == subjectL){
						for(Point2D pl : lp.getAllPoints()){
							if(pl.getY() > lastM){
								lastM = pl.getY();
								lastMessageN = lp.getTarget();
							}
						}
					}
					if(m.getSource() == subjectL){
						for(Point2D pl : lp.getAllPoints()){
							if(pl.getY() > lastM){
								lastM = pl.getY();
								lastMessageN = lp.getSource();
							}
						}
					}
				}
			}

			if(lastMessageN == null){
				lastMessageN = subjectN;
			}

			TransactionManager.beginTransaction();

			ILinkPresentation message = sde.createMessage(f.operationName, lastMessageN, objectN, maxM + 100);
			logger.log(Level.INFO,"message 作成 " + message.getLabel());

			if(f.addOperation){
				IMessage m = (IMessage)message.getModel();
				m.setOperation(f.operation);
			}

			TransactionManager.endTransaction();
		}
	}

}
