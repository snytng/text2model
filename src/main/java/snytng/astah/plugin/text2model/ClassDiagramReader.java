package snytng.astah.plugin.text2model;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.exception.ProjectNotFoundException;
import com.change_vision.jude.api.inf.model.IClass;
import com.change_vision.jude.api.inf.model.IClassDiagram;
import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IModel;
import com.change_vision.jude.api.inf.model.INamedElement;
import com.change_vision.jude.api.inf.model.IPackage;
import com.change_vision.jude.api.inf.presentation.IPresentation;
import com.change_vision.jude.api.inf.project.ProjectAccessor;

/**
 * クラス図の関連名を読み上げる
 */
public class ClassDiagramReader {
	
	/**
	 * logger
	 */
	static final Logger logger = Logger.getLogger(ClassDiagramReader.class.getName());
	static {
	      ConsoleHandler consoleHandler = new ConsoleHandler();
	      consoleHandler.setLevel(Level.CONFIG);      
	      logger.addHandler(consoleHandler);
	      logger.setUseParentHandlers(false);
	}
	
	private IClassDiagram diagram = null;
	
	public ClassDiagramReader(IClassDiagram diagram){
		this.diagram = diagram;
	}
    
	public static List<String> readClassDiagramInProject() throws ProjectNotFoundException,ClassNotFoundException {
		ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
		IModel iCurrentProject = projectAccessor.getProject();
		return readClassDiagramInPackage(iCurrentProject);
	}

	private static List<String> readClassDiagramInPackage(IPackage iPackage) {
		List<String> classInfoInEachDiagram = new ArrayList<>();

		// パッケージのダイアグラムを表示
        IDiagram[] iDiagrams = iPackage.getDiagrams();
        for (int i = 0; i < iDiagrams.length; i++){
        	IDiagram iDiagram = iDiagrams[i];
        	if(iDiagram instanceof IClassDiagram){
        		classInfoInEachDiagram.addAll(new ClassDiagramReader((IClassDiagram)iDiagram).read());
        	}
        }

        // 子パッケージの要素（クラスやパッケージ）を表示
        INamedElement[] iNamedElements = iPackage.getOwnedElements();
        for (int i = 0; i < iNamedElements.length; i++) {
        	if (iNamedElements[i] instanceof IPackage) {
                IPackage iChildPackage = (IPackage) iNamedElements[i];
            	classInfoInEachDiagram.addAll(readClassDiagramInPackage(iChildPackage));
            }
        }
        
        return classInfoInEachDiagram;
    }
	
	public int getNumberOfClasses() {
		int noc = 0;

		try {
			IPresentation[] iPresentations = diagram.getPresentations();
			for(int j = 0; j < iPresentations.length; j++){
				IPresentation iPresentation = iPresentations[j];
				if(iPresentation.getModel() instanceof IClass){
					noc++;
				}
			}
		}catch(Exception e){
			logger.log(Level.WARNING, e.getMessage(), e);
		}

		return noc;
	}

	public List<String> read() {
		List<String> classInfoInClassDiagram = new ArrayList<>();
		try {
			IPresentation[] iPresentations = diagram.getPresentations();
	
			// 関連読み上げ
			for(int j = 0; j < iPresentations.length; j++){
				IPresentation iPresentation = iPresentations[j];
				if(RelationReader.isSupportedRelation(iPresentation.getModel())){
					String relation = RelationReader.printRelation(iPresentation.getModel());
					if(relation != null){
						classInfoInClassDiagram.add(relation);
					}
				}
			}
		}catch(Exception e){
			logger.log(Level.WARNING, e.getMessage(), e);
		}
		return classInfoInClassDiagram;
	}
}
